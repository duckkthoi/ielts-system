package com.ltmtu.ielts.realtime;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.support.StompTestClient;
import com.ltmtu.ielts.support.StompTestClient.Connection;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/**
 * Kênh Mức 2 theo bài tập, WebSocket thật (docs/05 B.2): tiến độ live (FR-61) và online / đang mở phần nào (FR-54),
 * phân quyền chỉ giáo viên chủ bài.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeIntegrationTest extends IntegrationTest {

	/** 27 từ, trên ngưỡng 20 từ của BR-18. */
	private static final String ESSAY = "In recent years, people is increasingly concerned about the environment. "
			+ "Governments should invest more in public transport because it reduces pollution and traffic congestion "
			+ "in big cities.";

	@LocalServerPort
	private int port;

	private StompTestClient stomp;
	private User teacherUser;
	private String teacher;
	private User student;
	private long assignmentId;
	private long partId;
	private long criterionId;

	@BeforeEach
	void setUp() throws Exception {
		stomp = new StompTestClient(port, json);
		teacherUser = newUser(Role.TEACHER);
		teacher = accessToken(teacherUser);
		student = newUser(Role.STUDENT);
		Map<String, Object> part = Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>Đề</p>",
				"criteria", List.of(Map.of("code", "TR", "name", "Task Response", "step", "1", "checklist", List.of())));
		JsonNode a = data(call(postJson("/api/teacher/assignments", Map.of("title", "Bài live", "dueAt",
				clock.instant().plus(Duration.ofDays(3)).toString(), "parts", List.of(part))), teacher)
				.andExpect(apiStatus(201)).andReturn().getResponse().getContentAsString());
		assignmentId = a.get("id").asLong();
		partId = a.get("parts").get(0).get("id").asLong();
		criterionId = a.get("parts").get(0).get("criteria").get(0).get("id").asLong();
		call(postJson("/api/teacher/assignments/" + assignmentId + "/publish", Map.of("studentIds", List.of(student.getId()))),
				teacher).andExpect(apiStatus(200));
	}

	@AfterEach
	void tearDown() {
		stomp.close();
	}

	@Test
	@DisplayName("FR-61: giáo viên chủ bài thấy ô tiến độ đổi ngay — bắt đầu làm (DRAFT), nộp (SUBMITTED), trả bài (RETURNED kèm band)")
	void progressLive() throws Exception {
		BlockingQueue<JsonNode> progress = stomp.subscribe(stomp.connect(teacher), topic("progress"));
		String studentToken = accessToken(student);

		call(put("/api/student/parts/" + partId + "/draft").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("content", "Bắt đầu"))), studentToken).andExpect(apiStatus(200));
		JsonNode draft = progress.poll(5, TimeUnit.SECONDS);
		assertThat(draft).isNotNull();
		assertThat(draft.get("status").asString()).isEqualTo("DRAFT");
		assertThat(draft.get("studentId").asLong()).isEqualTo(student.getId());
		assertThat(draft.get("partId").asLong()).isEqualTo(partId);

		call(put("/api/student/parts/" + partId + "/draft").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("content", "Lưu lần hai"))), studentToken).andExpect(apiStatus(200));
		assertThat(progress.poll(500, TimeUnit.MILLISECONDS)).as("lưu nháp lần sau không đổi trạng thái").isNull();

		call(postJson("/api/student/parts/" + partId + "/submit", Map.of("content", ESSAY, "expectedVersion", 0)),
				studentToken).andExpect(apiStatus(201));
		JsonNode submitted = progress.poll(5, TimeUnit.SECONDS);
		assertThat(submitted.get("status").asString()).isEqualTo("SUBMITTED");
		assertThat(submitted.get("late").asBoolean()).isFalse();
		long submissionId = submitted.get("submissionId").asLong();

		call(put("/api/teacher/submissions/" + submissionId + "/grade").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("versionNo", 1, "scores",
						List.of(Map.of("criterionId", criterionId, "score", 6))))), teacher).andExpect(apiStatus(200));
		call(post("/api/teacher/submissions/" + submissionId + "/return"), teacher).andExpect(apiStatus(200));
		JsonNode returned = progress.poll(5, TimeUnit.SECONDS);
		assertThat(returned.get("status").asString()).isEqualTo("RETURNED");
		assertThat(returned.get("score").decimalValue()).isEqualByComparingTo("6.0");
	}

	@Test
	@DisplayName("FR-54: sinh viên kết nối → online; mở phần → viewingPartId; ma trận REST cũng thấy; rời kết nối → offline")
	void presence() throws Exception {
		BlockingQueue<JsonNode> presence = stomp.subscribe(stomp.connect(teacher), topic("presence"));

		Connection s = stomp.connect(accessToken(student));
		JsonNode online = presence.poll(5, TimeUnit.SECONDS);
		assertThat(online).isNotNull();
		assertThat(online.get("studentId").asLong()).isEqualTo(student.getId());
		assertThat(online.get("online").asBoolean()).isTrue();
		assertThat(online.get("viewingPartId").isNull()).isTrue();

		stomp.send(s, "/app/presence/viewing", Map.of("assignmentId", assignmentId, "partId", partId));
		JsonNode viewing = presence.poll(5, TimeUnit.SECONDS);
		assertThat(viewing.get("viewingPartId").asLong()).isEqualTo(partId);

		JsonNode row = data(call(get("/api/teacher/assignments/" + assignmentId + "/progress"), teacher)
				.andReturn().getResponse().getContentAsString()).get("rows").get(0);
		assertThat(row.get("online").asBoolean()).isTrue();
		assertThat(row.get("viewingPartId").asLong()).isEqualTo(partId);

		s.session().disconnect();
		JsonNode offline = presence.poll(5, TimeUnit.SECONDS);
		assertThat(offline.get("online").asBoolean()).isFalse();
		JsonNode after = data(call(get("/api/teacher/assignments/" + assignmentId + "/progress"), teacher)
				.andReturn().getResponse().getContentAsString()).get("rows").get(0);
		assertThat(after.get("online").asBoolean()).isFalse();
		assertThat(after.get("viewingPartId").isNull()).isTrue();
	}

	@Test
	@DisplayName("Sinh viên báo đang mở bài không được giao / phần không thuộc bài → bỏ qua, giáo viên không nhận gì")
	void viewingIgnoredWhenNotVisible() throws Exception {
		BlockingQueue<JsonNode> presence = stomp.subscribe(stomp.connect(teacher), topic("presence"));
		User outsider = newUser(Role.STUDENT);
		Connection s = stomp.connect(accessToken(outsider));
		stomp.send(s, "/app/presence/viewing", Map.of("assignmentId", assignmentId, "partId", partId));
		assertThat(presence.poll(800, TimeUnit.MILLISECONDS)).isNull();

		Connection mine = stomp.connect(accessToken(student));
		assertThat(presence.poll(5, TimeUnit.SECONDS).get("online").asBoolean()).isTrue();
		stomp.send(mine, "/app/presence/viewing", Map.of("assignmentId", assignmentId, "partId", 999_999_999L));
		assertThat(presence.poll(800, TimeUnit.MILLISECONDS)).isNull();
	}

	@Test
	@DisplayName("Phân quyền kênh /topic/assignments/{id}/…: giáo viên khác và sinh viên → ERROR; giáo viên gửi /app/presence/viewing → ERROR")
	void channelAccess() throws Exception {
		Connection other = stomp.connect(accessToken(newUser(Role.TEACHER)));
		other.session().subscribe(topic("progress"), new StompSessionHandlerAdapter() {
		});
		assertThat(other.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền nhận kênh");

		Connection s = stomp.connect(accessToken(student));
		s.session().subscribe(topic("presence"), new StompSessionHandlerAdapter() {
		});
		assertThat(s.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền nhận kênh");

		Connection t = stomp.connect(teacher);
		stomp.send(t, "/app/presence/viewing", Map.of("assignmentId", assignmentId));
		assertThat(t.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền gửi");
	}

	private String topic(String kind) {
		return "/topic/assignments/" + assignmentId + "/" + kind;
	}

	private JsonNode data(String body) {
		return json.readTree(body).get("data");
	}

	private ResultActions call(MockHttpServletRequestBuilder req, String token)
			throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}
}
