package com.ltmtu.ielts.exam;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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

/** Sự kiện chế độ thi qua WebSocket thật (docs/05 B.2): rời tab, dán, mất kết nối → giáo viên thấy ngay (BR-25). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExamWebSocketIntegrationTest extends IntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private ExamThrottle examThrottle;

	private StompTestClient stomp;
	private User teacherUser;
	private String teacher;
	private User student;
	private long assignmentId;
	private long partId;

	@BeforeEach
	void setUp() throws Exception {
		examThrottle.clear();
		stomp = new StompTestClient(port, json);
		teacherUser = newUser(Role.TEACHER);
		teacher = accessToken(teacherUser);
		student = newUser(Role.STUDENT);
		Map<String, Object> part = Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>Đề</p>",
				"examMode", true, "examDurationMinutes", 40,
				"criteria", List.of(Map.of("code", "TR", "name", "Task Response", "step", "1", "checklist", List.of())));
		JsonNode a = data(call(postJson("/api/teacher/assignments", Map.of("title", "Bài thi live", "dueAt",
				clock.instant().plus(Duration.ofDays(3)).toString(), "parts", List.of(part))), teacher));
		assignmentId = a.get("id").asLong();
		partId = a.get("parts").get(0).get("id").asLong();
		call(postJson("/api/teacher/assignments/" + assignmentId + "/publish", Map.of("studentIds", List.of(student.getId()))),
				teacher).andExpect(apiStatus(200));
	}

	@AfterEach
	void tearDown() {
		stomp.close();
	}

	@Test
	@DisplayName("BR-25: rời tab / dán → giáo viên nhận ngay trên /topic/…/exam; chỉ một thông báo trong 2 phút, sau đó lại báo")
	void tabAndPasteAreRecordedAndAlerted() throws Exception {
		BlockingQueue<JsonNode> exam = stomp.subscribe(stomp.connect(teacher), topic());
		String studentToken = accessToken(student);
		long sessionId = data(call(post("/api/student/parts/" + partId + "/exam/start"), studentToken))
				.get("sessionId").asLong();
		JsonNode started = exam.poll(5, TimeUnit.SECONDS);
		assertThat(started.get("type").asString()).isEqualTo("STARTED");
		assertThat(started.get("studentId").asLong()).isEqualTo(student.getId());
		assertThat(started.get("status").asString()).isEqualTo("IN_PROGRESS");

		Connection s = stomp.connect(studentToken);
		assertThat(nextOfType(exam, "RECONNECT")).as("kết nối đầu tiên khi đang thi").isNotNull();

		stomp.send(s, eventDest(sessionId), Map.of("type", "TAB_HIDDEN"));
		JsonNode hidden = nextOfType(exam, "TAB_HIDDEN");
		assertThat(hidden.get("sessionId").asLong()).isEqualTo(sessionId);
		assertThat(hidden.get("studentName").asString()).isEqualTo(student.getFullName());

		stomp.send(s, eventDest(sessionId), Map.of("type", "TAB_VISIBLE"));
		assertThat(nextOfType(exam, "TAB_VISIBLE")).isNotNull();
		stomp.send(s, eventDest(sessionId), Map.of("type", "PASTE", "pasteChars", 350));
		assertThat(nextOfType(exam, "PASTE").get("pasteChars").asInt()).isEqualTo(350);
		assertThat(alerts()).as("rời tab + dán trong 2 phút → một thông báo").hasSize(1);

		clock.advance(Duration.ofMinutes(2).plusSeconds(1));
		stomp.send(s, eventDest(sessionId), Map.of("type", "PASTE", "pasteChars", 20));
		assertThat(nextOfType(exam, "PASTE")).isNotNull();
		List<JsonNode> alerts = alerts();
		assertThat(alerts).hasSize(2);
		assertThat(alerts.getFirst().get("title").asString()).contains("dán văn bản (20 ký tự)");
		assertThat(alerts.getFirst().get("link").asString()).isEqualTo("/giao-vien/bai-tap/" + assignmentId + "/giam-sat");

		s.session().disconnect();
		assertThat(nextOfType(exam, "DISCONNECT")).isNotNull();

		JsonNode row = data(call(get("/api/teacher/exam-sessions/" + sessionId + "/events"), teacher)).get("session");
		assertThat(row.get("tabHiddenCount").asInt()).isEqualTo(1);
		assertThat(row.get("pasteCount").asInt()).isEqualTo(2);
		assertThat(row.get("disconnectCount").asInt()).isEqualTo(1);
	}

	@Test
	@DisplayName("Sự kiện vào phiên của người khác / loại server tự ghi / sau khi nộp → bỏ qua, giáo viên không nhận gì")
	void ignoredEvents() throws Exception {
		String studentToken = accessToken(student);
		long sessionId = data(call(post("/api/student/parts/" + partId + "/exam/start"), studentToken))
				.get("sessionId").asLong();
		BlockingQueue<JsonNode> exam = stomp.subscribe(stomp.connect(teacher), topic());

		Connection outsider = stomp.connect(accessToken(newUser(Role.STUDENT)));
		stomp.send(outsider, eventDest(sessionId), Map.of("type", "TAB_HIDDEN"));
		Connection mine = stomp.connect(studentToken);
		assertThat(nextOfType(exam, "RECONNECT")).isNotNull();
		stomp.send(mine, eventDest(sessionId), Map.of("type", "OVERTIME"));
		stomp.send(mine, eventDest(999_999_999L), Map.of("type", "TAB_HIDDEN"));
		assertThat(exam.poll(800, TimeUnit.MILLISECONDS)).isNull();
	}

	@Test
	@DisplayName("Phân quyền: giáo viên khác / sinh viên nghe /topic/…/exam → ERROR; giáo viên gửi /app/exam/…/event → ERROR")
	void channelAccess() throws Exception {
		Connection other = stomp.connect(accessToken(newUser(Role.TEACHER)));
		other.session().subscribe(topic(), new StompSessionHandlerAdapter() {
		});
		assertThat(other.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền nhận kênh");

		Connection s = stomp.connect(accessToken(student));
		s.session().subscribe(topic(), new StompSessionHandlerAdapter() {
		});
		assertThat(s.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền nhận kênh");

		Connection t = stomp.connect(teacher);
		stomp.send(t, eventDest(1L), Map.of("type", "TAB_HIDDEN"));
		assertThat(t.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền gửi");
	}

	@Test
	@DisplayName("BR-25: rời cửa sổ (tab vẫn hiện) được ghi, tính vào số lần rời trang và báo giáo viên")
	void windowBlur() throws Exception {
		BlockingQueue<JsonNode> exam = stomp.subscribe(stomp.connect(teacher), topic());
		String studentToken = accessToken(student);
		long sessionId = data(call(post("/api/student/parts/" + partId + "/exam/start"), studentToken))
				.get("sessionId").asLong();
		Connection s = stomp.connect(studentToken);
		stomp.send(s, eventDest(sessionId), Map.of("type", "WINDOW_BLUR"));
		assertThat(nextOfType(exam, "WINDOW_BLUR")).isNotNull();
		stomp.send(s, eventDest(sessionId), Map.of("type", "WINDOW_FOCUS"));
		assertThat(nextOfType(exam, "WINDOW_FOCUS")).isNotNull();

		assertThat(alerts().getFirst().get("title").asString()).contains("rời khỏi cửa sổ làm bài");
		JsonNode row = data(call(get("/api/teacher/exam-sessions/" + sessionId + "/events"), teacher)).get("session");
		assertThat(row.get("tabHiddenCount").asInt()).isEqualTo(1);
	}

	@Test
	@DisplayName("BR-25: sự kiện lúc mất mạng được gửi bù — giữ giờ xảy ra, gắn reportedLate; sau khi nộp chỉ nhận sự kiện trước lúc nộp")
	void lateReports() throws Exception {
		BlockingQueue<JsonNode> exam = stomp.subscribe(stomp.connect(teacher), topic());
		String studentToken = accessToken(student);
		Instant started = clock.instant();
		long sessionId = data(call(post("/api/student/parts/" + partId + "/exam/start"), studentToken))
				.get("sessionId").asLong();
		call(put("/api/student/parts/" + partId + "/draft").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("content", "x"))), studentToken).andExpect(apiStatus(200));
		clock.advance(Duration.ofMinutes(3));
		Connection s = stomp.connect(studentToken);

		Instant offlineAt = started.plus(Duration.ofMinutes(1));
		stomp.send(s, eventDest(sessionId), Map.of("type", "TAB_HIDDEN", "occurredAt", offlineAt.toString()));
		JsonNode late = nextOfType(exam, "TAB_HIDDEN");
		assertThat(Instant.parse(late.get("at").asString())).isEqualTo(offlineAt);
		assertThat(late.get("reportedLate").asBoolean()).isTrue();
		assertThat(alerts().getFirst().get("body").asString()).contains("lúc mất mạng");

		studentToken = accessToken(student);
		call(postJson("/api/student/parts/" + partId + "/submit", Map.of("content", "Bài nộp", "expectedVersion", 0)),
				studentToken).andExpect(apiStatus(200));
		assertThat(nextOfType(exam, "SUBMITTED")).isNotNull();
		stomp.send(s, eventDest(sessionId), Map.of("type", "TAB_VISIBLE", "occurredAt",
				started.plus(Duration.ofMinutes(2)).toString()));
		assertThat(nextOfType(exam, "TAB_VISIBLE")).as("gửi bù, xảy ra trước lúc nộp → nhận").isNotNull();
		stomp.send(s, eventDest(sessionId), Map.of("type", "TAB_HIDDEN"));
		assertThat(exam.poll(800, TimeUnit.MILLISECONDS)).as("sau khi nộp → bỏ").isNull();

		JsonNode events = data(call(get("/api/teacher/exam-sessions/" + sessionId + "/events"), teacher)).get("events");
		assertThat(events.valueStream().map(e -> e.get("type").asString()).toList())
				.as("xếp theo giờ xảy ra: sự kiện gửi bù (phút 1, 2) đứng trước lúc kết nối lại (phút 3)")
				.containsExactly("STARTED", "TAB_HIDDEN", "TAB_VISIBLE", "RECONNECT", "SUBMITTED");
		assertThat(events.get(1).get("reportedLate").asBoolean()).isTrue();
	}

	/** Bỏ qua các sự kiện khác loại (vd RECONNECT đến chen giữa), trả sự kiện đầu tiên đúng loại. */
	private static JsonNode nextOfType(BlockingQueue<JsonNode> queue, String type) throws InterruptedException {
		long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < until) {
			JsonNode e = queue.poll(200, TimeUnit.MILLISECONDS);
			if (e != null && type.equals(e.get("type").asString())) {
				return e;
			}
		}
		return null;
	}

	/** Thông báo EXAM_ALERT của giáo viên, mới nhất trước. */
	private List<JsonNode> alerts() throws Exception {
		teacher = accessToken(teacherUser);
		List<JsonNode> found = new ArrayList<>();
		data(call(get("/api/notifications"), teacher)).forEach(n -> {
			if ("EXAM_ALERT".equals(n.get("type").asString())) {
				found.add(n);
			}
		});
		return found;
	}

	private String topic() {
		return "/topic/assignments/" + assignmentId + "/exam";
	}

	private static String eventDest(long sessionId) {
		return "/app/exam/" + sessionId + "/event";
	}

	private JsonNode data(ResultActions result) throws Exception {
		return json.readTree(result.andExpect(apiStatus(200)).andReturn().getResponse().getContentAsString()).get("data");
	}

	private ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}
}
