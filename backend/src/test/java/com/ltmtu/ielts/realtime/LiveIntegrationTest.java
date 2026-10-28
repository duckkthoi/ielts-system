package com.ltmtu.ielts.realtime;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.beans.factory.annotation.Autowired;
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

/** Xem trực tiếp bài đang gõ + bình luận trực tiếp qua chat bài nộp, WebSocket thật (BR-26, docs/05 B.2). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LiveIntegrationTest extends IntegrationTest {

	/** 27 từ, trên ngưỡng 20 từ của BR-18. */
	private static final String ESSAY = "In recent years, people is increasingly concerned about the environment. "
			+ "Governments should invest more in public transport because it reduces pollution and traffic congestion "
			+ "in big cities.";

	@LocalServerPort
	private int port;

	@Autowired
	private LiveDraftService liveDraftService;

	private StompTestClient stomp;
	private String teacher;
	private User student;
	private String studentToken;
	private long partId;

	@BeforeEach
	void setUp() throws Exception {
		liveDraftService.clear();
		stomp = new StompTestClient(port, json);
		teacher = accessToken(newUser(Role.TEACHER));
		student = newUser(Role.STUDENT);
		studentToken = accessToken(student);
		Map<String, Object> part = Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>Đề</p>",
				"criteria", List.of(Map.of("code", "TR", "name", "Task Response", "step", "1", "checklist", List.of())));
		JsonNode a = data(call(postJson("/api/teacher/assignments", Map.of("title", "Bài xem trực tiếp", "dueAt",
				clock.instant().plus(Duration.ofDays(3)).toString(), "parts", List.of(part))), teacher));
		partId = a.get("parts").get(0).get("id").asLong();
		call(postJson("/api/teacher/assignments/" + a.get("id").asLong() + "/publish",
				Map.of("studentIds", List.of(student.getId()))), teacher).andExpect(apiStatus(200));
	}

	@AfterEach
	void tearDown() {
		stomp.close();
	}

	@Test
	@DisplayName("BR-26: giáo viên chủ bài thấy ngay nội dung sinh viên đang gõ (kèm số từ); REST trả bản mới nhất; nộp → SUBMITTED")
	void liveContent() throws Exception {
		JsonNode empty = data(call(get(snapshotUrl()), teacher));
		assertThat(empty.get("source").asString()).isEqualTo("EMPTY");
		assertThat(empty.get("submissionId").isNull()).isTrue();

		BlockingQueue<JsonNode> live = stomp.subscribe(stomp.connect(teacher), topic());
		Connection s = stomp.connect(studentToken);
		stomp.send(s, "/app/live/" + partId + "/content", Map.of("content", "Some people believe that"));
		JsonNode e = live.poll(5, TimeUnit.SECONDS);
		assertThat(e).isNotNull();
		assertThat(e.get("type").asString()).isEqualTo("CONTENT");
		assertThat(e.get("content").asString()).isEqualTo("Some people believe that");
		assertThat(e.get("wordCount").asInt()).isEqualTo(4);

		JsonNode snap = data(call(get(snapshotUrl()), teacher));
		assertThat(snap.get("source").asString()).isEqualTo("LIVE");
		assertThat(snap.get("content").asString()).isEqualTo("Some people believe that");
		assertThat(snap.get("online").asBoolean()).isTrue();

		call(postJson("/api/student/parts/" + partId + "/submit", Map.of("content", ESSAY, "expectedVersion", 0)),
				studentToken).andExpect(apiStatus(200));
		JsonNode submitted = live.poll(5, TimeUnit.SECONDS);
		assertThat(submitted.get("type").asString()).isEqualTo("SUBMITTED");
		assertThat(data(call(get(snapshotUrl()), teacher)).get("source").asString()).isEqualTo("NOT_WORKING");

		stomp.send(s, "/app/live/" + partId + "/content", Map.of("content", "sửa lén sau khi nộp"));
		assertThat(live.poll(800, TimeUnit.MILLISECONDS)).as("đã nộp → không phát nữa").isNull();
	}

	@Test
	@DisplayName("BR-26: chưa gõ gì từ lúc server chạy → REST lấy bản nháp đã lưu; bình luận trực tiếp qua chat bài nộp tới ngay sinh viên")
	void draftFallbackAndLiveComment() throws Exception {
		long savedId = data(call(put("/api/student/parts/" + partId + "/draft").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("content", "Bản nháp đã lưu"))), studentToken))
				.get("submissionId").asLong();
		assertThat(data(call(get("/api/student/parts/" + partId + "/draft"), studentToken)).get("submissionId").asLong())
				.as("sinh viên biết kênh chat bài nộp của mình").isEqualTo(savedId);
		JsonNode snap = data(call(get(snapshotUrl()), teacher));
		assertThat(snap.get("source").asString()).isEqualTo("DRAFT");
		assertThat(snap.get("content").asString()).isEqualTo("Bản nháp đã lưu");
		assertThat(snap.get("status").asString()).isEqualTo("DRAFT");
		long submissionId = snap.get("submissionId").asLong();
		assertThat(submissionId).isEqualTo(savedId);

		BlockingQueue<JsonNode> inbox = stomp.subscribe(stomp.connect(studentToken), "/user/queue/chat");
		stomp.send(stomp.connect(teacher), "/app/chat/submission-" + submissionId + "/send",
				Map.of("content", "Đoạn 2 nên thêm ví dụ", "clientMsgId", "live-1"));
		JsonNode msg = inbox.poll(5, TimeUnit.SECONDS);
		assertThat(msg).isNotNull();
		assertThat(msg.get("content").asString()).isEqualTo("Đoạn 2 nên thêm ví dụ");
		assertThat(msg.get("channelId").asString()).isEqualTo("submission-" + submissionId);
	}

	@Test
	@DisplayName("Sinh viên không được giao gửi nội dung → bỏ qua; quá 120 lần / phút → bỏ phần dư")
	void ignoredUpdates() throws Exception {
		BlockingQueue<JsonNode> live = stomp.subscribe(stomp.connect(teacher), topic());
		User outsider = newUser(Role.STUDENT);
		Connection o = stomp.connect(accessToken(outsider));
		stomp.send(o, "/app/live/" + partId + "/content", Map.of("content", "x"));
		BlockingQueue<JsonNode> outsiderTopic = stomp.subscribe(stomp.connect(teacher),
				"/topic/live/parts/" + partId + "/students/" + outsider.getId());
		assertThat(live.poll(500, TimeUnit.MILLISECONDS)).isNull();
		assertThat(outsiderTopic.poll(300, TimeUnit.MILLISECONDS)).isNull();

		Connection s = stomp.connect(studentToken);
		for (int i = 0; i < 125; i++) {
			stomp.send(s, "/app/live/" + partId + "/content", Map.of("content", "v" + i));
		}
		int received = 0;
		while (live.poll(1, TimeUnit.SECONDS) != null) {
			received++;
		}
		assertThat(received).isEqualTo(120);
	}

	@Test
	@DisplayName("BR-15: giáo viên khác / sinh viên nghe kênh xem trực tiếp → ERROR, REST → 404; giáo viên gửi nội dung → ERROR")
	void access() throws Exception {
		String stranger = accessToken(newUser(Role.TEACHER));
		call(get(snapshotUrl()), stranger).andExpect(apiStatus(404));

		Connection other = stomp.connect(stranger);
		other.session().subscribe(topic(), new StompSessionHandlerAdapter() {
		});
		assertThat(other.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền nhận kênh");

		Connection s = stomp.connect(studentToken);
		s.session().subscribe(topic(), new StompSessionHandlerAdapter() {
		});
		assertThat(s.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền nhận kênh");

		Connection t = stomp.connect(teacher);
		stomp.send(t, "/app/live/" + partId + "/content", Map.of("content", "x"));
		assertThat(t.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền gửi");
	}

	private String topic() {
		return "/topic/live/parts/" + partId + "/students/" + student.getId();
	}

	private String snapshotUrl() {
		return "/api/teacher/parts/" + partId + "/students/" + student.getId() + "/live";
	}

	private JsonNode data(ResultActions result) throws Exception {
		return json.readTree(result.andExpect(apiStatus(200)).andReturn().getResponse().getContentAsString()).get("data");
	}

	private ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}
}
