package com.ltmtu.ielts.notification;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import com.ltmtu.ielts.grading.GradingEvents;
import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** UC-04, SD-05: thông báo khi phát hành / nộp / trả bài / yêu cầu sửa; API thông báo; email (FR-50, FR-51, FR-53). */
class NotificationIntegrationTest extends IntegrationTest {

	@Autowired
	private ApplicationEventPublisher events;

	@Autowired
	private TransactionTemplate tx;

	@Autowired
	private NotificationService notificationService;

	private User teacherUser;
	private String teacher;
	private User student;
	private String studentToken;

	@BeforeEach
	void setUp() throws Exception {
		teacherUser = newUser(Role.TEACHER);
		teacher = accessToken(teacherUser);
		student = newUser(Role.STUDENT);
		studentToken = accessToken(student);
	}

	private JsonNode data(ResultActions r) throws Exception {
		return json.readTree(r.andReturn().getResponse().getContentAsString()).get("data");
	}

	private ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	private JsonNode notifications(String token) throws Exception {
		return data(call(get("/api/notifications"), token).andExpect(apiStatus(200)));
	}

	/** Bài 1 phần (Task 2, 1 tiêu chí TR), phát hành cho {@code students}; trả về chi tiết bài phía giáo viên. */
	private JsonNode publish(String title, Instant openAt, Set<Long> students) throws Exception {
		Map<String, Object> part = Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>Đề</p>",
				"criteria", List.of(Map.of("code", "TR", "name", "Task Response", "checklist", List.of())));
		Map<String, Object> body = new HashMap<>(Map.of("title", title, "dueAt",
				Instant.parse("2027-01-05T16:59:00Z").toString(), "parts", List.of(part)));
		if (openAt != null) {
			body.put("openAt", openAt.toString());
		}
		JsonNode a = data(call(postJson("/api/teacher/assignments", body), teacher).andExpect(apiStatus(201)));
		call(postJson("/api/teacher/assignments/" + a.get("id").asLong() + "/publish", Map.of("studentIds", students)),
				teacher).andExpect(apiStatus(200));
		return a;
	}

	@Test
	@DisplayName("SD-05 / FR-51: phát hành → mỗi người nhận có thông báo \"Bài tập mới\" (hạn theo giờ VN, link) và email")
	void publishNotifiesRecipients() throws Exception {
		User other = newUser(Role.STUDENT);
		JsonNode a = publish("Tuần 5 – Writing", null, Set.of(student.getId(), other.getId()));
		long id = a.get("id").asLong();

		JsonNode n = notifications(studentToken).get(0);
		assertThat(n.get("type").asString()).isEqualTo("ASSIGNMENT_PUBLISHED");
		assertThat(n.get("title").asString()).isEqualTo("Bài tập mới: Tuần 5 – Writing");
		assertThat(n.get("body").asString()).isEqualTo("Hạn nộp: 23:59 05/01/2027."); // 16:59Z = 23:59 giờ VN
		assertThat(n.get("link").asString()).isEqualTo("/sinh-vien/bai-tap/" + id);
		assertThat(n.get("readAt").isNull()).isTrue();
		assertThat(n.get("createdAt").asString()).isEqualTo(clock.instant().toString());
		assertThat(notifications(accessToken(other))).hasSize(1);
		call(get("/api/notifications/unread-count"), studentToken).andExpect(jsonPath("$.data.count").value(1));

		var mails = mail.awaitMatching(m -> List.of(m.getTo()).contains(student.getEmail())
				&& "Bài tập mới: Tuần 5 – Writing".equals(m.getSubject()), 1, Duration.ofSeconds(5));
		assertThat(mails).hasSize(1);
		assertThat(mails.getFirst().getText()).contains("Hạn nộp: 23:59 05/01/2027.")
				.contains("http://localhost:3000/sinh-vien/bai-tap/" + id);
	}

	@Test
	@DisplayName("BR-05: bài mở trong tương lai → chưa gửi thông báo lúc phát hành (job gửi khi tới giờ mở)")
	void futureOpenNotNotifiedYet() throws Exception {
		publish("Mở sau", clock.instant().plus(Duration.ofDays(1)), Set.of(student.getId()));
		assertThat(notifications(studentToken)).isEmpty();
	}

	@Test
	@DisplayName("FR-53: sinh viên nộp → giáo viên có thông báo (tên SV, link màn chấm, cờ muộn), không có email")
	void submissionNotifiesTeacher() throws Exception {
		JsonNode a = publish("Bài nộp", null, Set.of(student.getId()));
		long partId = a.get("parts").get(0).get("id").asLong();
		call(postJson("/api/student/parts/" + partId + "/submit", Map.of("content", "My essay.", "expectedVersion", 0)),
				studentToken).andExpect(apiStatus(201));

		JsonNode n = notifications(teacher).get(0);
		assertThat(n.get("type").asString()).isEqualTo("SUBMISSION_RECEIVED");
		assertThat(n.get("title").asString()).isEqualTo(student.getFullName() + " đã nộp bài: Bài nộp – Task 2");
		assertThat(n.get("body").isNull()).isTrue();
		assertThat(n.get("link").asString()).startsWith("/giao-vien/cham-bai/");
		Thread.sleep(300);
		assertThat(mail.sent()).noneMatch(m -> List.of(m.getTo()).contains(teacherUser.getEmail()));
	}

	@Test
	@DisplayName("UC-18: trả bài → \"Đã có kết quả\" kèm band; yêu cầu sửa → \"Cần sửa bài\" kèm hạn sửa và lời nhắn")
	void gradingNotifiesStudent() throws Exception {
		JsonNode a = publish("Bài chấm", null, Set.of(student.getId()));
		long partId = a.get("parts").get(0).get("id").asLong();
		long criterionId = a.get("parts").get(0).get("criteria").get(0).get("id").asLong();
		call(postJson("/api/student/parts/" + partId + "/submit", Map.of("content", "My essay.", "expectedVersion", 0)),
				studentToken).andExpect(apiStatus(201));
		long submissionId = data(call(get("/api/teacher/submissions").param("partId", String.valueOf(partId)), teacher))
				.get(0).get("submissionId").asLong();
		Map<String, Object> grade = Map.of("versionNo", 1, "scores", List.of(Map.of("criterionId", criterionId, "score", 1)));
		call(put("/api/teacher/submissions/" + submissionId + "/grade").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(grade)), teacher).andExpect(apiStatus(200));
		call(postJson("/api/teacher/submissions/" + submissionId + "/request-revision",
				Map.of("note", "Viết dài hơn", "dueAt", "2027-01-10T16:59:00Z")), teacher).andExpect(apiStatus(200));

		JsonNode n = notifications(studentToken).get(0);
		assertThat(n.get("type").asString()).isEqualTo("REVISION_REQUESTED");
		assertThat(n.get("title").asString()).isEqualTo("Cần sửa bài: Bài chấm – Task 2");
		assertThat(n.get("body").asString()).isEqualTo("Hạn sửa: 23:59 10/01/2027. Lời nhắn: Viết dài hơn");
		assertThat(n.get("link").asString()).isEqualTo("/sinh-vien/bai-tap/" + a.get("id").asLong());
		assertThat(mail.awaitMatching(m -> List.of(m.getTo()).contains(student.getEmail())
				&& m.getSubject().startsWith("Cần sửa bài"), 1, Duration.ofSeconds(5))).hasSize(1);

		// Trả bài: gửi thẳng sự kiện như GradingService (vòng sửa bài đầy đủ đã có ở GradingFlowIntegrationTest).
		tx.executeWithoutResult(s -> events.publishEvent(new GradingEvents.Returned(submissionId, a.get("id").asLong(),
				partId, student.getId(), 1, new BigDecimal("6.5"), "Bài chấm", "Task 2", List.of(student.getId()))));
		n = notifications(studentToken).get(0);
		assertThat(n.get("type").asString()).isEqualTo("GRADE_RETURNED");
		assertThat(n.get("title").asString()).isEqualTo("Đã có kết quả: Bài chấm – Task 2");
		assertThat(n.get("body").asString()).isEqualTo("Band: 6.5");
	}

	@Test
	@DisplayName("Lưu thông báo cùng transaction nghiệp vụ: nghiệp vụ rollback → không có thông báo, không đẩy, không email")
	void rollbackLeavesNoNotification() throws Exception {
		try {
			tx.executeWithoutResult(s -> {
				events.publishEvent(new GradingEvents.Returned(1L, 1L, 1L, student.getId(), 1, BigDecimal.ONE, "X", "Y",
						List.of(student.getId())));
				throw new IllegalStateException("nghiệp vụ lỗi sau khi phát sự kiện");
			});
		} catch (IllegalStateException expected) {
			// Đúng như mong đợi.
		}
		assertThat(notifications(studentToken)).isEmpty();
		Thread.sleep(300);
		assertThat(mail.sent()).noneMatch(m -> List.of(m.getTo()).contains(student.getEmail()));
	}

	@Test
	@DisplayName("API: lọc chưa đọc, ?after= (lấy thông báo bị lỡ), đánh dấu đã đọc / tất cả, của người khác → 404")
	void api() throws Exception {
		NotificationService.NewNotification msg = new NotificationService.NewNotification(
				NotificationType.MANUAL_REMINDER, "Nhắc 1", null, null);
		notificationService.notify(Set.of(student.getId()), msg);
		Instant afterFirst = clock.instant();
		clock.advance(Duration.ofSeconds(10));
		notificationService.notify(Set.of(student.getId()), new NotificationService.NewNotification(
				NotificationType.MANUAL_REMINDER, "Nhắc 2", null, null));

		JsonNode all = notifications(studentToken);
		assertThat(all).hasSize(2);
		assertThat(all.get(0).get("title").asString()).isEqualTo("Nhắc 2"); // mới nhất trước
		call(get("/api/notifications").param("after", afterFirst.toString()), studentToken)
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].title").value("Nhắc 2"))
				.andExpect(jsonPath("$.totalRecord").value(1));

		long firstId = all.get(1).get("id").asLong();
		call(post("/api/notifications/" + firstId + "/read"), teacher).andExpect(apiStatus(404));
		call(post("/api/notifications/" + firstId + "/read"), studentToken)
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.readAt").value(clock.instant().toString()));
		call(get("/api/notifications").param("unreadOnly", "true"), studentToken)
				.andExpect(jsonPath("$.data.length()").value(1));
		call(get("/api/notifications/unread-count"), studentToken).andExpect(jsonPath("$.data.count").value(1));
		call(post("/api/notifications/read-all"), studentToken).andExpect(jsonPath("$.data.updated").value(1));
		call(get("/api/notifications/unread-count"), studentToken).andExpect(jsonPath("$.data.count").value(0));
		call(get("/api/notifications").param("after", "không-phải-ngày"), studentToken).andExpect(apiStatus(400));
		mvc.perform(get("/api/notifications")).andExpect(apiStatus(401));
	}
}
