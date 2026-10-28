package com.ltmtu.ielts.exam;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static com.ltmtu.ielts.support.ApiStatus.apiStatusOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.time.Instant;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Chế độ thi qua REST (UC-27, UC-21, UC-31): bắt đầu tính giờ, chặn soạn khi chưa bắt đầu, quá giờ, giám sát. */
class ExamIntegrationTest extends IntegrationTest {

	/** 27 từ, trên ngưỡng 20 từ của BR-18. */
	private static final String ESSAY = "In recent years, people is increasingly concerned about the environment. "
			+ "Governments should invest more in public transport because it reduces pollution and traffic congestion "
			+ "in big cities.";

	@Autowired
	private ExamOvertimeJob overtimeJob;

	@Autowired
	private ExamThrottle examThrottle;

	private User teacherUser;
	private String teacher;
	private User student;
	private String studentToken;
	private long assignmentId;
	private long examPartId;
	private long normalPartId;

	@BeforeEach
	void setUp() throws Exception {
		examThrottle.clear();
		teacherUser = newUser(Role.TEACHER);
		teacher = accessToken(teacherUser);
		student = newUser(Role.STUDENT);
		studentToken = accessToken(student);
		JsonNode a = createAssignment(teacher, List.of(part("Task 2 thi", true, 40), part("Task 1", false, null)));
		assignmentId = a.get("id").asLong();
		examPartId = a.get("parts").get(0).get("id").asLong();
		normalPartId = a.get("parts").get(1).get("id").asLong();
		assertThat(a.get("parts").get(0).get("examMode").asBoolean()).isTrue();
		assertThat(a.get("parts").get(0).get("examDurationMinutes").asInt()).isEqualTo(40);
		assertThat(a.get("parts").get(1).get("examMode").asBoolean()).isFalse();
		publish(teacher, assignmentId, Map.of("studentIds", List.of(student.getId())));
	}

	@Test
	@DisplayName("BR-23: chưa bấm Bắt đầu → lưu nháp / nộp bị 409 EXAM_NOT_STARTED; phần thường không bị ảnh hưởng")
	void mustStartBeforeWriting() throws Exception {
		call(put("/api/student/parts/" + examPartId + "/draft").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("content", "x"))), studentToken)
				.andExpect(apiStatus(409)).andExpect(jsonPath("$.errorCode").value("EXAM_NOT_STARTED"));
		call(postJson("/api/student/parts/" + examPartId + "/submit", Map.of("content", ESSAY, "expectedVersion", 0)),
				studentToken).andExpect(apiStatus(409)).andExpect(jsonPath("$.errorCode").value("EXAM_NOT_STARTED"));

		call(post("/api/student/parts/" + normalPartId + "/exam/start"), studentToken).andExpect(apiStatus(409))
				.andExpect(jsonPath("$.errorCode").value("INVALID_STATE"));
		saveDraft(normalPartId, "phần thường").andExpect(apiStatus(200));

		JsonNode none = data(call(get("/api/student/parts/" + examPartId + "/exam"), studentToken));
		assertThat(none == null || none.isNull()).as("chưa bắt đầu → data rỗng").isTrue();
		JsonNode detail = data(call(get("/api/student/assignments/" + assignmentId), studentToken));
		assertThat(detail.get("parts").get(0).get("examMode").asBoolean()).isTrue();
		assertThat(detail.get("parts").get(0).get("examDurationMinutes").asInt()).isEqualTo(40);
	}

	@Test
	@DisplayName("BR-23: bắt đầu → endsAt = giờ server + 40 phút; mở lại sau 10 phút vẫn đúng phiên cũ, đồng hồ không đặt lại")
	void startIsFixedAndIdempotent() throws Exception {
		Instant t0 = clock.instant();
		JsonNode s = data(call(post("/api/student/parts/" + examPartId + "/exam/start"), studentToken));
		assertThat(Instant.parse(s.get("startedAt").asString())).isEqualTo(t0);
		assertThat(Instant.parse(s.get("endsAt").asString())).isEqualTo(t0.plus(Duration.ofMinutes(40)));
		assertThat(s.get("status").asString()).isEqualTo("IN_PROGRESS");

		clock.advance(Duration.ofMinutes(10));
		studentToken = accessToken(student);
		JsonNode again = data(call(post("/api/student/parts/" + examPartId + "/exam/start"), studentToken));
		assertThat(again.get("sessionId").asLong()).isEqualTo(s.get("sessionId").asLong());
		assertThat(again.get("endsAt").asString()).isEqualTo(s.get("endsAt").asString());
		assertThat(data(call(get("/api/student/parts/" + examPartId + "/exam"), studentToken)).get("sessionId").asLong())
				.isEqualTo(s.get("sessionId").asLong());

		saveDraft(examPartId, "Đang thi").andExpect(apiStatus(200));
	}

	@Test
	@DisplayName("BR-23: 2 lần bấm Bắt đầu ĐỒNG THỜI → cùng một phiên thi, chỉ một sự kiện STARTED")
	void concurrentStart() throws Exception {
		CountDownLatch go = new CountDownLatch(1);
		Callable<String> task = () -> {
			go.await();
			return call(post("/api/student/parts/" + examPartId + "/exam/start"), studentToken).andReturn().getResponse()
					.getContentAsString();
		};
		ExecutorService pool = Executors.newFixedThreadPool(2);
		List<Long> ids = new ArrayList<>();
		try {
			Future<String> a = pool.submit(task);
			Future<String> b = pool.submit(task);
			go.countDown();
			for (Future<String> f : List.of(a, b)) {
				JsonNode body = json.readTree(f.get());
				assertThat(body.get("success").asBoolean()).as(body.toString()).isTrue();
				ids.add(body.get("data").get("sessionId").asLong());
			}
		} finally {
			pool.shutdown();
		}
		assertThat(ids.get(0)).isEqualTo(ids.get(1));
		JsonNode timeline = data(call(get("/api/teacher/exam-sessions/" + ids.get(0) + "/events"), teacher));
		assertThat(timeline.get("events")).hasSize(1);
		assertThat(timeline.get("events").get(0).get("type").asString()).isEqualTo("STARTED");
	}

	@Test
	@DisplayName("BR-24: hết giờ không tự nộp — job báo giáo viên + sinh viên đúng một lần; nộp sau đó vẫn được, bị gắn quá thời gian")
	void overtimeIsFlaggedNotSubmitted() throws Exception {
		long sessionId = data(call(post("/api/student/parts/" + examPartId + "/exam/start"), studentToken))
				.get("sessionId").asLong();
		saveDraft(examPartId, "Viết dở").andExpect(apiStatus(200));

		// DB test dùng chung: job còn gặp phiên cũ của test khác, nên chỉ kiểm tra thông báo của chính test này.
		clock.advance(Duration.ofMinutes(39));
		overtimeJob.run();
		teacher = accessToken(teacherUser);
		assertThat(notifications(teacher, "EXAM_OVERTIME")).as("chưa hết giờ").isEmpty();
		clock.advance(Duration.ofMinutes(2));
		overtimeJob.run();
		overtimeJob.run();

		teacher = accessToken(teacherUser);
		studentToken = accessToken(student);
		assertThat(notifications(teacher, "EXAM_OVERTIME")).as("báo đúng một lần").hasSize(1);
		JsonNode mine = notifications(studentToken, "EXAM_OVERTIME");
		assertThat(mine).hasSize(1);
		assertThat(mine.get(0).get("link").asString()).isEqualTo("/sinh-vien/lam-bai/" + assignmentId);
		assertThat(data(call(get("/api/student/parts/" + examPartId + "/exam"), studentToken)).get("status").asString())
				.isEqualTo("OVERTIME");

		saveDraft(examPartId, "Viết tiếp sau giờ").andExpect(apiStatus(200));
		call(postJson("/api/student/parts/" + examPartId + "/submit", Map.of("content", ESSAY, "expectedVersion", 0)),
				studentToken).andExpect(apiStatus(200));

		JsonNode timeline = data(call(get("/api/teacher/exam-sessions/" + sessionId + "/events"), teacher));
		assertThat(timeline.get("session").get("status").asString()).isEqualTo("FINISHED_OVERTIME");
		assertThat(timeline.get("events").valueStream().map(e -> e.get("type").asString()).toList())
				.containsExactly("STARTED", "OVERTIME", "SUBMITTED");
	}

	@Test
	@DisplayName("Nộp trong giờ → FINISHED, job không báo quá giờ; màn giám sát lọc theo lớp")
	void monitorAndClassFilter() throws Exception {
		User other = newUser(Role.STUDENT);
		long classId = createClass(teacher, other);
		call(put("/api/teacher/assignments/" + assignmentId + "/targets").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("classIds", List.of(classId), "studentIds",
						List.of(student.getId())))), teacher).andExpect(apiStatus(200));
		String otherToken = accessToken(other);

		call(post("/api/student/parts/" + examPartId + "/exam/start"), studentToken).andExpect(apiStatus(200));
		call(post("/api/student/parts/" + examPartId + "/exam/start"), otherToken).andExpect(apiStatus(200));
		call(postJson("/api/student/parts/" + examPartId + "/submit", Map.of("content", ESSAY, "expectedVersion", 0)),
				studentToken).andExpect(apiStatus(200));
		clock.advance(Duration.ofMinutes(41));
		overtimeJob.run();
		teacher = accessToken(teacherUser);
		assertThat(notifications(teacher, "EXAM_OVERTIME")).as("chỉ phiên chưa nộp").hasSize(1);

		JsonNode all = data(call(get("/api/teacher/assignments/" + assignmentId + "/exam-monitor"), teacher));
		assertThat(all.get("parts")).hasSize(1);
		assertThat(all.get("parts").get(0).get("id").asLong()).isEqualTo(examPartId);
		assertThat(all.get("rows")).hasSize(2);
		Map<Long, String> statusByStudent = new HashMap<>();
		all.get("rows").forEach(r -> statusByStudent.put(r.get("studentId").asLong(), r.get("status").asString()));
		assertThat(statusByStudent).containsEntry(student.getId(), "FINISHED").containsEntry(other.getId(), "OVERTIME");

		JsonNode byClass = data(call(get("/api/teacher/assignments/" + assignmentId + "/exam-monitor")
				.param("classId", String.valueOf(classId)), teacher));
		assertThat(byClass.get("rows")).hasSize(1);
		assertThat(byClass.get("rows").get(0).get("studentId").asLong()).isEqualTo(other.getId());
	}

	@Test
	@DisplayName("Bảng tin giám sát: sự kiện mới nhất trước, lọc theo lớp; giáo viên khác → 404")
	void recentEvents() throws Exception {
		User other = newUser(Role.STUDENT);
		long classId = createClass(teacher, other);
		call(put("/api/teacher/assignments/" + assignmentId + "/targets").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("classIds", List.of(classId), "studentIds",
						List.of(student.getId())))), teacher).andExpect(apiStatus(200));
		call(post("/api/student/parts/" + examPartId + "/exam/start"), studentToken).andExpect(apiStatus(200));
		clock.advance(Duration.ofSeconds(5));
		call(post("/api/student/parts/" + examPartId + "/exam/start"), accessToken(other)).andExpect(apiStatus(200));

		JsonNode all = data(call(get("/api/teacher/assignments/" + assignmentId + "/exam-events"), teacher));
		assertThat(all).hasSize(2);
		assertThat(all.get(0).get("studentId").asLong()).as("mới nhất trước").isEqualTo(other.getId());
		assertThat(all.get(0).get("type").asString()).isEqualTo("STARTED");
		assertThat(all.get(0).get("reportedLate").asBoolean()).isFalse();

		JsonNode byClass = data(call(get("/api/teacher/assignments/" + assignmentId + "/exam-events")
				.param("classId", String.valueOf(classId)), teacher));
		assertThat(byClass).hasSize(1);
		call(get("/api/teacher/assignments/" + assignmentId + "/exam-events"), accessToken(newUser(Role.TEACHER)))
				.andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("BR-15: giáo viên khác xem giám sát / dòng thời gian → 404; lớp không phải của mình → 400")
	void ownership() throws Exception {
		long sessionId = data(call(post("/api/student/parts/" + examPartId + "/exam/start"), studentToken))
				.get("sessionId").asLong();
		String stranger = accessToken(newUser(Role.TEACHER));
		call(get("/api/teacher/assignments/" + assignmentId + "/exam-monitor"), stranger).andExpect(apiStatus(404));
		call(get("/api/teacher/exam-sessions/" + sessionId + "/events"), stranger).andExpect(apiStatus(404));

		long strangersClass = createClass(stranger, newUser(Role.STUDENT));
		call(get("/api/teacher/assignments/" + assignmentId + "/exam-monitor")
				.param("classId", String.valueOf(strangersClass)), teacher).andExpect(apiStatus(400));
		call(get("/api/teacher/assignments/" + assignmentId + "/exam-monitor"), studentToken).andExpect(apiStatus(403));
	}

	@Test
	@DisplayName("Bật chế độ thi mà không có thời gian thi lẫn thời gian gợi ý → 400; chỉ có thời gian gợi ý thì lấy theo đó")
	void durationRequired() throws Exception {
		Map<String, Object> noDuration = part("Thiếu giờ", true, null);
		call(postJson("/api/teacher/assignments", assignmentBody(List.of(noDuration))), teacher)
				.andExpect(apiStatus(400)).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

		Map<String, Object> suggested = new HashMap<>(noDuration);
		suggested.put("suggestedMinutes", 20);
		JsonNode a = data(call(postJson("/api/teacher/assignments", assignmentBody(List.of(suggested))), teacher));
		assertThat(a.get("parts").get(0).get("examDurationMinutes").asInt()).isEqualTo(20);
	}

	// ─── Hỗ trợ ──────────────────────────────────────────────────────────────

	private JsonNode createAssignment(String token, List<Map<String, Object>> parts) throws Exception {
		return data(call(postJson("/api/teacher/assignments", assignmentBody(parts)), token));
	}

	private Map<String, Object> assignmentBody(List<Map<String, Object>> parts) {
		return Map.of("title", "Bài thi", "dueAt", clock.instant().plus(Duration.ofDays(3)).toString(), "parts", parts);
	}

	private static Map<String, Object> part(String title, boolean exam, Integer minutes) {
		Map<String, Object> p = new HashMap<>();
		p.put("title", title);
		p.put("taskType", "WRITING_TASK2");
		p.put("prompt", "<p>Đề</p>");
		p.put("examMode", exam);
		if (minutes != null) {
			p.put("examDurationMinutes", minutes);
		}
		p.put("criteria", List.of(Map.of("code", "TR", "name", "Task Response", "step", "1", "checklist", List.of())));
		return p;
	}

	private void publish(String token, long id, Map<String, Object> body) throws Exception {
		call(postJson("/api/teacher/assignments/" + id + "/publish", body), token).andExpect(apiStatus(200));
	}

	private long createClass(String token, User member) throws Exception {
		String code = "EX" + member.getStudentCode().substring(2).toUpperCase();
		long id = data(call(postJson("/api/teacher/classes", Map.of("code", code, "name", "Lớp thi")), token))
				.get("id").asLong();
		call(postJson("/api/teacher/classes/" + id + "/members", Map.of("studentCodes", List.of(member.getStudentCode()))),
				token).andExpect(apiStatus(200));
		return id;
	}

	private ResultActions saveDraft(long partId, String content) throws Exception {
		return call(put("/api/student/parts/" + partId + "/draft").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("content", content))), studentToken);
	}

	private JsonNode notifications(String token, String type) throws Exception {
		List<JsonNode> found = new ArrayList<>();
		data(call(get("/api/notifications"), token)).forEach(n -> {
			if (type.equals(n.get("type").asString())) {
				found.add(n);
			}
		});
		return json.valueToTree(found);
	}

	private JsonNode data(ResultActions result) throws Exception {
		JsonNode body = json.readTree(result.andReturn().getResponse().getContentAsString());
		assertThat(apiStatusOf(result.andReturn())).as(body.toString()).isEqualTo(200);
		return body.get("data");
	}

	private ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}
}
