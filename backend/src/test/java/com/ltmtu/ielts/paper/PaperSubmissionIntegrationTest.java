package com.ltmtu.ielts.paper;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static com.ltmtu.ielts.support.ApiStatus.apiStatusOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.ltmtu.ielts.assignment.AssignmentOpeningService;
import com.ltmtu.ielts.notification.DeadlineReminderJob;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Bài làm của bài PAPER thuộc nhóm (BR-28, B21b): soạn chung, nộp chung, kết quả / chat / thông báo cho cả nhóm. */
class PaperSubmissionIntegrationTest extends PaperTestSupport {

	/** 27 từ — trên ngưỡng 20 từ để chấm điểm tuỳ ý. */
	private static final String TEXT = "In recent years, people is increasingly concerned about the environment. "
			+ "Governments should invest more in public transport because it reduces pollution and traffic congestion "
			+ "in big cities.";

	@Autowired
	private AssignmentOpeningService openingService;

	@Autowired
	private DeadlineReminderJob reminderJob;

	private User teacherUser;
	private String teacher;
	private User a;
	private User b;
	private User loner;
	private long assignmentId;
	private long partId;
	private long criterionId;

	@BeforeEach
	void setUp() throws Exception {
		teacherUser = newUser(Role.TEACHER);
		teacher = accessToken(teacherUser);
		a = newUser(Role.STUDENT);
		b = newUser(Role.STUDENT);
		loner = newUser(Role.STUDENT);
		Map<String, Object> onePart = new HashMap<>(Map.of("parts", List.of(stage("Bản cuối",
				List.of(section("Abstract", true))))));
		onePart.put("groupLockAt", clock.instant().plus(Duration.ofDays(1)).toString());
		onePart.put("dueAt", clock.instant().plus(Duration.ofDays(1)).plus(Duration.ofHours(20)).toString());
		assignmentId = publishedPaper(teacher, onePart, a, b, loner);
		JsonNode detail = data(call(get("/api/teacher/assignments/" + assignmentId), teacher));
		partId = detail.get("parts").get(0).get("id").asLong();
		criterionId = detail.get("parts").get(0).get("criteria").get(0).get("id").asLong();
		call(postJson("/api/teacher/assignments/" + assignmentId + "/groups",
				Map.of("name", "Nhóm AB", "studentIds", List.of(a.getId(), b.getId()))), teacher).andExpect(apiStatus(200));
	}

	@Test
	@DisplayName("BR-28: chưa có nhóm → lưu nháp / nộp 409 NO_GROUP; thành viên cùng nhóm thấy và sửa CHUNG một bản nháp")
	void sharedDraft() throws Exception {
		String lonerToken = accessToken(loner);
		saveDraft(lonerToken, "x", null).andExpect(apiStatus(409)).andExpect(jsonPath("$.errorCode").value("NO_GROUP"));
		submit(lonerToken, 0).andExpect(apiStatus(409)).andExpect(jsonPath("$.errorCode").value("NO_GROUP"));
		assertThat(data(call(get(draftUrl()), lonerToken)).get("source").asString()).isEqualTo("EMPTY");

		String ta = accessToken(a);
		String tb = accessToken(b);
		JsonNode saved = data(saveDraft(ta, "Bản của A", 0));
		JsonNode seenByB = data(call(get(draftUrl()), tb));
		assertThat(seenByB.get("content").asString()).isEqualTo("Bản của A");
		assertThat(seenByB.get("submissionId").asLong()).isEqualTo(saved.get("submissionId").asLong());
		data(saveDraft(tb, "B sửa tiếp", 1));
		saveDraft(ta, "A ghi đè bản cũ", 1).andExpect(apiStatus(409))
				.andExpect(jsonPath("$.errorCode").value("DRAFT_CONFLICT"));
		assertThat(data(call(get(draftUrl()), lonerToken)).get("source").asString())
				.as("người ngoài nhóm không thấy bài của nhóm").isEqualTo("EMPTY");
	}

	@Test
	@DisplayName("BR-28: một người nộp → cả nhóm thấy đã nộp; trả bài → mọi thành viên nhận thông báo và xem được kết quả")
	void submitAndReturnForWholeGroup() throws Exception {
		String ta = accessToken(a);
		String tb = accessToken(b);
		data(saveDraft(ta, "Nháp", 0));
		data(submit(tb, 0));
		JsonNode detailA = data(call(get("/api/student/assignments/" + assignmentId), ta));
		assertThat(detailA.get("parts").get(0).get("status").asString()).isEqualTo("SUBMITTED");
		submit(ta, 0).andExpect(apiStatus(409)).andExpect(jsonPath("$.errorCode").value("VERSION_CONFLICT"));

		long submissionId = data(call(get(draftUrl()), ta)).get("submissionId").asLong();
		call(put("/api/teacher/submissions/" + submissionId + "/grade").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("versionNo", 1, "scores",
						List.of(Map.of("criterionId", criterionId, "score", 7.5))))), teacher).andExpect(apiStatus(200));
		call(post("/api/teacher/submissions/" + submissionId + "/return"), teacher).andExpect(apiStatus(200));

		for (User member : List.of(a, b)) {
			String token = accessToken(member);
			assertThat(notificationTypes(token)).contains("GRADE_RETURNED");
			JsonNode result = data(call(get("/api/student/assignments/" + assignmentId + "/result"), token));
			assertThat(result.toString()).contains("7.5");
		}
		assertThat(notificationTypes(accessToken(loner))).doesNotContain("GRADE_RETURNED");
	}

	@Test
	@DisplayName("BR-28: ma trận tiến độ hiện bài của nhóm ở mọi thành viên; danh sách bài đếm cả nhóm là đã nộp đủ")
	void progressCountsWholeGroup() throws Exception {
		data(saveDraft(accessToken(a), "Nháp", 0));
		data(submit(accessToken(a), 0));
		JsonNode rows = data(call(get("/api/teacher/assignments/" + assignmentId + "/progress"), teacher)).get("rows");
		Map<Long, String> status = new HashMap<>();
		rows.forEach(r -> status.put(r.get("student").get("id").asLong(), r.get("cells").get(0).get("status").asString()));
		assertThat(status).containsEntry(a.getId(), "SUBMITTED").containsEntry(b.getId(), "SUBMITTED")
				.containsEntry(loner.getId(), "NOT_STARTED");

		JsonNode list = data(call(get("/api/teacher/assignments").param("size", "100"), teacher));
		JsonNode mine = list.valueStream().filter(x -> x.get("id").asLong() == assignmentId).findFirst().orElseThrow();
		assertThat(mine.get("completedCount").asLong()).isEqualTo(2);
	}

	@Test
	@DisplayName("BR-28: kênh chat bài nộp gồm cả nhóm và giáo viên; người ngoài nhóm → 404")
	void chatChannelForGroup() throws Exception {
		long submissionId = data(saveDraft(accessToken(a), "Nháp", 0)).get("submissionId").asLong();
		String channel = "/api/chat/submission-" + submissionId + "/messages";
		call(get(channel), accessToken(b)).andExpect(apiStatus(200));
		call(get(channel), teacher).andExpect(apiStatus(200));
		call(get(channel), accessToken(loner)).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("Nhóm đã có bài làm: sinh viên không tự rời, giáo viên không xoá được nhóm")
	void groupWithWorkIsLocked() throws Exception {
		data(saveDraft(accessToken(a), "Nháp", 0));
		call(delete("/api/student/assignments/" + assignmentId + "/groups/mine"), accessToken(b))
				.andExpect(apiStatus(409));
		long groupId = data(call(get("/api/teacher/assignments/" + assignmentId + "/groups"), teacher)).get("groups")
				.get(0).get("id").asLong();
		call(delete("/api/teacher/assignments/" + assignmentId + "/groups/" + groupId), teacher).andExpect(apiStatus(409));
	}

	@Test
	@DisplayName("BR-28: hai thành viên lưu nháp lần đầu ĐỒNG THỜI → chỉ một bài làm cho nhóm (UNIQUE phần + nhóm)")
	void concurrentFirstSave() throws Exception {
		String ta = accessToken(a);
		String tb = accessToken(b);
		CountDownLatch go = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			List<Future<Integer>> results = new ArrayList<>();
			for (String token : List.of(ta, tb)) {
				Callable<Integer> task = () -> {
					go.await();
					return apiStatusOf(saveDraft(token, "Đua", 0).andReturn());
				};
				results.add(pool.submit(task));
			}
			go.countDown();
			assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(200, 409);
		} finally {
			pool.shutdown();
		}
	}

	@Test
	@DisplayName("BR-27: tới hạn chốt nhóm còn người chưa có nhóm → báo giáo viên đúng một lần")
	void groupLockNotification() throws Exception {
		clock.advance(Duration.ofDays(1));
		openingService.announceGroupLocks(clock.instant());
		openingService.announceGroupLocks(clock.instant());
		teacher = accessToken(teacherUser);
		assertThat(notificationTypes(teacher).stream().filter("PAPER_GROUP_LOCK"::equals).count()).isEqualTo(1);
	}

	@Test
	@DisplayName("Nhắc hạn: nhóm đã nộp thì không nhắc thành viên nào; người chưa có nhóm vẫn được nhắc")
	void deadlineReminderPerGroup() throws Exception {
		data(saveDraft(accessToken(a), "Nháp", 0));
		data(submit(accessToken(b), 0));
		clock.advance(Duration.ofDays(1));
		reminderJob.run();
		assertThat(notificationTypes(accessToken(a))).doesNotContain("DEADLINE_REMINDER");
		assertThat(notificationTypes(accessToken(b))).doesNotContain("DEADLINE_REMINDER");
		assertThat(notificationTypes(accessToken(loner))).contains("DEADLINE_REMINDER");
	}

	// ─── Hỗ trợ ──────────────────────────────────────────────────────────────

	private String draftUrl() {
		return "/api/student/parts/" + partId + "/draft";
	}

	private ResultActions saveDraft(String token, String content, Integer draftVersion) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("content", content);
		body.put("draftVersion", draftVersion);
		return call(put(draftUrl()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), token);
	}

	private ResultActions submit(String token, int expectedVersion) throws Exception {
		return call(postJson("/api/student/parts/" + partId + "/submit",
				Map.of("content", TEXT, "expectedVersion", expectedVersion)), token);
	}

	private List<String> notificationTypes(String token) throws Exception {
		List<String> types = new ArrayList<>();
		data(call(get("/api/notifications").param("size", "50"), token)).forEach(n -> types.add(n.get("type").asString()));
		return types;
	}
}
