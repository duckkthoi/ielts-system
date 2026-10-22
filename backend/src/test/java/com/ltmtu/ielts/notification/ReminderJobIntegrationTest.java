package com.ltmtu.ielts.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.assignment.AssignmentOpeningService;
import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/**
 * UC-30, SD-10, BR-14: nhắc hạn nộp / hạn sửa tự động; thông báo bài tới giờ mở; giáo viên nhắc thủ công (UC-16).
 * DB test dùng chung nên chỉ kiểm tra thông báo của chính các sinh viên tạo trong test, không đếm tổng.
 */
class ReminderJobIntegrationTest extends IntegrationTest {

	@Autowired
	private DeadlineReminderJob reminderJob;

	@Autowired
	private AssignmentOpeningService openingService;

	@Autowired
	private NotificationRepository notificationRepository;

	private String teacher;

	@BeforeEach
	void setUp() throws Exception {
		teacher = accessToken(newUser(Role.TEACHER));
	}

	private JsonNode data(ResultActions r) throws Exception {
		return json.readTree(r.andReturn().getResponse().getContentAsString()).get("data");
	}

	private ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	/** Tiêu đề các thông báo của một người, mới nhất trước. */
	private List<String> titles(User u) {
		return notificationRepository.search(u.getId(), false, null, PageRequest.of(0, 50)).getContent().stream()
				.map(Notification::getTitle).toList();
	}

	private List<Notification> ofType(User u, NotificationType type) {
		return notificationRepository.search(u.getId(), false, null, PageRequest.of(0, 50)).getContent().stream()
				.filter(n -> n.getType() == type).toList();
	}

	/** Bài 1 phần, hạn = now + {@code dueIn}; trả về chi tiết bài. {@code publishBody} null = để nháp. */
	private JsonNode assignment(String title, Duration dueIn, Instant openAt, Map<String, Object> publishBody)
			throws Exception {
		Map<String, Object> part = Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>Đề</p>",
				"criteria", List.of(Map.of("code", "TR", "name", "Task Response", "checklist", List.of())));
		Map<String, Object> body = new HashMap<>(Map.of("title", title, "dueAt", clock.instant().plus(dueIn).toString(),
				"parts", List.of(part)));
		if (openAt != null) {
			body.put("openAt", openAt.toString());
		}
		JsonNode a = data(call(postJson("/api/teacher/assignments", body), teacher).andExpect(status().isCreated()));
		if (publishBody != null) {
			call(postJson("/api/teacher/assignments/" + a.get("id").asLong() + "/publish", publishBody), teacher)
					.andExpect(status().isOk());
		}
		return a;
	}

	private JsonNode published(String title, Duration dueIn, User... students) throws Exception {
		Set<Long> ids = new HashSet<>();
		for (User s : students) {
			ids.add(s.getId());
		}
		return assignment(title, dueIn, null, Map.of("studentIds", ids));
	}

	private static long partId(JsonNode a) {
		return a.get("parts").get(0).get("id").asLong();
	}

	private void submit(User s, long partId, int expectedVersion) throws Exception {
		call(postJson("/api/student/parts/" + partId + "/submit", Map.of("content", "My essay.", "expectedVersion",
				expectedVersion)), accessToken(s)).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("BR-14: còn ≤ 24 giờ → nhắc 1 lần người chưa nộp (kể cả đang nháp), không nhắc người đã nộp; ≤ 2 giờ → nhắc mốc thứ hai 1 lần")
	void twoMilestonesOncePerStudent() throws Exception {
		User notStarted = newUser(Role.STUDENT);
		User drafting = newUser(Role.STUDENT);
		User done = newUser(Role.STUDENT);
		JsonNode a = published("Nhắc hạn", Duration.ofHours(20), notStarted, drafting, done);
		call(put("/api/student/parts/" + partId(a) + "/draft").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("content", "nháp"))), accessToken(drafting)).andExpect(status().isOk());
		submit(done, partId(a), 0);

		reminderJob.run();
		reminderJob.run(); // chạy lại ngay: không nhắc trùng
		assertThat(ofType(notStarted, NotificationType.DEADLINE_REMINDER)).singleElement().satisfies(n -> {
			assertThat(n.getTitle()).isEqualTo("Sắp hết hạn nộp: Nhắc hạn – Task 2");
			assertThat(n.getBody()).contains("còn dưới 24 giờ").contains("Bạn chưa nộp phần này.");
			assertThat(n.getLink()).isEqualTo("/sinh-vien/bai-tap/" + a.get("id").asLong());
		});
		assertThat(ofType(drafting, NotificationType.DEADLINE_REMINDER)).hasSize(1);
		assertThat(ofType(done, NotificationType.DEADLINE_REMINDER)).isEmpty();
		assertThat(mail.awaitMatching(m -> List.of(m.getTo()).contains(notStarted.getEmail())
				&& m.getSubject().startsWith("Sắp hết hạn nộp"), 1, Duration.ofSeconds(5))).hasSize(1);

		clock.advance(Duration.ofHours(19)); // còn 1 giờ
		reminderJob.run();
		reminderJob.run();
		List<Notification> reminders = ofType(notStarted, NotificationType.DEADLINE_REMINDER);
		assertThat(reminders).hasSize(2);
		assertThat(reminders.getFirst().getBody()).contains("còn dưới 2 giờ");

		clock.advance(Duration.ofHours(2)); // quá hạn: không nhắc nữa
		reminderJob.run();
		assertThat(ofType(notStarted, NotificationType.DEADLINE_REMINDER)).hasSize(2);
	}

	@Test
	@DisplayName("SD-10: lần chạy đầu tiên đã còn ≤ 2 giờ → chỉ nhắc mốc 2 giờ; hạn còn > 24 giờ, bài chưa mở, bài đã đóng → không nhắc")
	void windowsAndStates() throws Exception {
		User late = newUser(Role.STUDENT);
		User far = newUser(Role.STUDENT);
		User notOpen = newUser(Role.STUDENT);
		User closed = newUser(Role.STUDENT);
		published("Gấp", Duration.ofMinutes(90), late);
		published("Còn lâu", Duration.ofHours(30), far);
		assignment("Chưa mở", Duration.ofHours(20), clock.instant().plus(Duration.ofHours(10)),
				Map.of("studentIds", Set.of(notOpen.getId())));
		JsonNode c = published("Đã đóng", Duration.ofHours(20), closed);
		call(post("/api/teacher/assignments/" + c.get("id").asLong() + "/close"), teacher).andExpect(status().isOk());

		reminderJob.run();
		assertThat(ofType(late, NotificationType.DEADLINE_REMINDER)).singleElement()
				.satisfies(n -> assertThat(n.getBody()).contains("còn dưới 2 giờ"));
		assertThat(ofType(far, NotificationType.DEADLINE_REMINDER)).isEmpty();
		assertThat(ofType(notOpen, NotificationType.DEADLINE_REMINDER)).isEmpty();
		assertThat(ofType(closed, NotificationType.DEADLINE_REMINDER)).isEmpty();
	}

	@Test
	@DisplayName("BR-04 + BR-07: người nhận qua lớp cũng được nhắc; hạn riêng của phần được ưu tiên hơn hạn bài")
	void classMembersAndPartDeadline() throws Exception {
		User member = newUser(Role.STUDENT);
		long classId = data(call(postJson("/api/teacher/classes", Map.of("code", "N" + UUID.randomUUID().toString()
				.substring(0, 8), "name", "Lớp nhắc")), teacher)).get("id").asLong();
		call(postJson("/api/teacher/classes/" + classId + "/members", Map.of("studentCodes", List.of(member.getStudentCode()))),
				teacher).andExpect(status().isOk());
		// Hạn bài còn 3 ngày, nhưng hạn riêng của phần còn 5 giờ.
		JsonNode a = assignment("Theo lớp", Duration.ofDays(3), null, null);
		call(patch(
				"/api/teacher/assignments/" + a.get("id").asLong() + "/settings").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("partDueAts",
						Map.of(String.valueOf(partId(a)), clock.instant().plus(Duration.ofHours(5)).toString())))), teacher)
				.andExpect(status().isOk());
		call(postJson("/api/teacher/assignments/" + a.get("id").asLong() + "/publish", Map.of("classIds", Set.of(classId))),
				teacher).andExpect(status().isOk());

		reminderJob.run();
		assertThat(ofType(member, NotificationType.DEADLINE_REMINDER)).singleElement()
				.satisfies(n -> assertThat(n.getBody()).contains("còn dưới 24 giờ"));
	}

	@Test
	@DisplayName("Hạn sửa: đang \"Cần sửa\" → nhắc REVISION_H24; nộp bản sửa thì thôi; yêu cầu sửa lần 2 → được nhắc lại")
	void revisionDeadline() throws Exception {
		User s = newUser(Role.STUDENT);
		JsonNode a = published("Sửa bài", Duration.ofDays(5), s);
		long criterionId = a.get("parts").get(0).get("criteria").get(0).get("id").asLong();
		submit(s, partId(a), 0);
		long submissionId = data(call(get("/api/teacher/submissions").param("partId", String.valueOf(partId(a))), teacher))
				.get(0).get("submissionId").asLong();

		for (int cycle = 1; cycle <= 2; cycle++) {
			call(put("/api/teacher/submissions/" + submissionId + "/grade").contentType(MediaType.APPLICATION_JSON)
					.content(json.writeValueAsString(Map.of("versionNo", cycle, "scores",
							List.of(Map.of("criterionId", criterionId, "score", 1))))), teacher).andExpect(status().isOk());
			call(postJson("/api/teacher/submissions/" + submissionId + "/request-revision",
					Map.of("dueAt", clock.instant().plus(Duration.ofHours(10)).toString())), teacher).andExpect(status().isOk());
			reminderJob.run();
			reminderJob.run();
			assertThat(ofType(s, NotificationType.DEADLINE_REMINDER)).hasSize(cycle)
					.first().satisfies(n -> {
						assertThat(n.getTitle()).isEqualTo("Sắp hết hạn sửa bài: Sửa bài – Task 2");
						assertThat(n.getBody()).contains("Bạn chưa nộp bản sửa.");
					});
			submit(s, partId(a), cycle);
			reminderJob.run(); // đã nộp bản sửa → không nhắc
			assertThat(ofType(s, NotificationType.DEADLINE_REMINDER)).hasSize(cycle);
		}
	}

	@Test
	@DisplayName("BR-14: hai lượt job chạy ĐỒNG THỜI → mỗi sinh viên vẫn chỉ nhận 1 lần nhắc (INSERT … ON CONFLICT DO NOTHING)")
	void concurrentRuns() throws Exception {
		User s1 = newUser(Role.STUDENT);
		User s2 = newUser(Role.STUDENT);
		published("Đồng thời", Duration.ofHours(10), s1, s2);
		CountDownLatch start = new CountDownLatch(1);
		Callable<Integer> task = () -> {
			start.await();
			return reminderJob.run();
		};
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> a = pool.submit(task);
			Future<Integer> b = pool.submit(task);
			start.countDown();
			a.get();
			b.get();
		} finally {
			pool.shutdown();
		}
		assertThat(ofType(s1, NotificationType.DEADLINE_REMINDER)).hasSize(1);
		assertThat(ofType(s2, NotificationType.DEADLINE_REMINDER)).hasSize(1);
	}

	@Test
	@DisplayName("SD-05: bài hẹn giờ mở → lúc phát hành chưa báo; tới giờ mở job báo 1 lần cho mọi người nhận (cả người được giao thêm)")
	void openingAnnouncement() throws Exception {
		User early = newUser(Role.STUDENT);
		User added = newUser(Role.STUDENT);
		User alreadyOpen = newUser(Role.STUDENT);
		JsonNode a = assignment("Hẹn giờ", Duration.ofDays(3), clock.instant().plus(Duration.ofHours(2)),
				Map.of("studentIds", Set.of(early.getId())));
		call(put(
				"/api/teacher/assignments/" + a.get("id").asLong() + "/targets").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("studentIds", Set.of(added.getId())))), teacher)
				.andExpect(status().isOk());
		published("Mở ngay", Duration.ofDays(3), alreadyOpen);

		openingService.announceOpened(clock.instant());
		assertThat(titles(early)).doesNotContain("Bài tập mới: Hẹn giờ");

		clock.advance(Duration.ofHours(2));
		openingService.announceOpened(clock.instant());
		openingService.announceOpened(clock.instant());
		assertThat(titles(early)).containsOnlyOnce("Bài tập mới: Hẹn giờ");
		assertThat(titles(added)).containsOnlyOnce("Bài tập mới: Hẹn giờ");
		// Bài phát hành khi đã mở: đã báo lúc phát hành, job không báo lại.
		assertThat(titles(alreadyOpen)).containsOnlyOnce("Bài tập mới: Mở ngay");
	}

	@Test
	@DisplayName("UC-16: giáo viên nhắc thủ công → MANUAL_REMINDER; người không được giao / phần khác → 400; bài của người khác → 404; bài nháp → 409")
	void manualReminder() throws Exception {
		User s = newUser(Role.STUDENT);
		User outsider = newUser(Role.STUDENT);
		JsonNode a = published("Nhắc tay", Duration.ofDays(2), s);
		long id = a.get("id").asLong();

		call(postJson("/api/teacher/assignments/" + id + "/remind", Map.of("studentIds", Set.of(s.getId()),
				"partId", partId(a))), teacher)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sent").value(1));
		assertThat(ofType(s, NotificationType.MANUAL_REMINDER)).singleElement().satisfies(n -> {
			assertThat(n.getTitle()).isEqualTo("Giáo viên nhắc bạn hoàn thành: Nhắc tay – Task 2");
			assertThat(n.getBody()).startsWith("Hạn nộp: ");
		});

		call(postJson("/api/teacher/assignments/" + id + "/remind", Map.of("studentIds", Set.of(outsider.getId()))), teacher)
				.andExpect(status().isBadRequest());
		call(postJson("/api/teacher/assignments/" + id + "/remind", Map.of("studentIds", Set.of(s.getId()), "partId", 999999)),
				teacher).andExpect(status().isBadRequest());
		call(postJson("/api/teacher/assignments/" + id + "/remind", Map.of("studentIds", Set.of())), teacher)
				.andExpect(status().isBadRequest());
		call(postJson("/api/teacher/assignments/" + id + "/remind", Map.of("studentIds", Set.of(s.getId()))),
				accessToken(newUser(Role.TEACHER))).andExpect(status().isNotFound());
		long draft = assignment("Nháp", Duration.ofDays(2), null, null).get("id").asLong();
		call(postJson("/api/teacher/assignments/" + draft + "/remind", Map.of("studentIds", Set.of(s.getId()))), teacher)
				.andExpect(status().isConflict());
	}
}
