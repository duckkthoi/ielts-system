package com.ltmtu.ielts.chat;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.support.StompTestClient;
import com.ltmtu.ielts.support.StompTestClient.Connection;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Chat realtime (UC-29, FR-55, SD-12) với WebSocket thật: kênh bài nộp và kênh lớp. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatIntegrationTest extends IntegrationTest {

	private static final String ESSAY = "In recent years, people is increasingly concerned about the environment. "
			+ "Governments should invest more in public transport because it reduces pollution and traffic congestion "
			+ "in big cities.";

	@LocalServerPort
	private int port;

	private StompTestClient stomp;
	private User teacherUser;
	private String teacher;
	private User s1;
	private User s2;
	private long classId;
	private String classCode;
	private String submissionChannel;
	private String classChannel;

	@BeforeEach
	void setUp() throws Exception {
		stomp = new StompTestClient(port, json);
		teacherUser = newUser(Role.TEACHER);
		teacher = accessToken(teacherUser);
		s1 = newUser(Role.STUDENT);
		s2 = newUser(Role.STUDENT);
		classCode = "CH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
		classId = data(call(postJson("/api/teacher/classes", Map.of("code", classCode, "name", "Lớp chat")), teacher)
				.andExpect(apiStatus(201))).get("id").asLong();
		call(postJson("/api/teacher/classes/" + classId + "/members",
				Map.of("studentCodes", List.of(s1.getStudentCode(), s2.getStudentCode()))), teacher)
				.andExpect(apiStatus(200));
		Map<String, Object> part = Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>Đề</p>",
				"criteria", List.of(Map.of("code", "TR", "name", "Task Response", "step", "1", "checklist", List.of())));
		JsonNode a = data(call(postJson("/api/teacher/assignments", Map.of("title", "Bài chat", "dueAt",
				clock.instant().plus(Duration.ofDays(3)).toString(), "parts", List.of(part))), teacher)
				.andExpect(apiStatus(201)));
		long partId = a.get("parts").get(0).get("id").asLong();
		call(postJson("/api/teacher/assignments/" + a.get("id").asLong() + "/publish", Map.of("classIds", List.of(classId))),
				teacher).andExpect(apiStatus(200));
		call(postJson("/api/student/parts/" + partId + "/submit", Map.of("content", ESSAY, "expectedVersion", 0)),
				accessToken(s1)).andExpect(apiStatus(201));
		long submissionId = data(call(get("/api/teacher/submissions").param("partId", String.valueOf(partId)), teacher))
				.get(0).get("submissionId").asLong();
		submissionChannel = "submission-" + submissionId;
		classChannel = "class-" + classId;
	}

	@AfterEach
	void tearDown() {
		stomp.close();
	}

	@Test
	@DisplayName("FR-55: kênh bài nộp — gửi tin tới cả hai bên, gửi lại cùng clientMsgId không lưu trùng, đang gõ, đã xem, số chưa đọc")
	void submissionChat() throws Exception {
		Connection t = stomp.connect(teacher);
		Connection s = stomp.connect(accessToken(s1));
		BlockingQueue<JsonNode> toTeacher = stomp.subscribe(t, "/topic/chat/" + submissionChannel);
		BlockingQueue<JsonNode> toStudent = stomp.subscribe(s, "/topic/chat/" + submissionChannel);

		stomp.send(s, "/app/chat/" + submissionChannel + "/send", Map.of("content", "  Thưa cô, em hỏi về overview ", "clientMsgId", "c-1"));
		JsonNode got = toTeacher.poll(5, TimeUnit.SECONDS);
		assertThat(got.get("type").asString()).isEqualTo("MESSAGE");
		assertThat(got.get("content").asString()).isEqualTo("Thưa cô, em hỏi về overview");
		assertThat(got.get("senderName").asString()).isEqualTo(s1.getFullName());
		assertThat(got.get("clientMsgId").asString()).isEqualTo("c-1");
		long messageId = got.get("id").asLong();
		assertThat(toStudent.poll(5, TimeUnit.SECONDS).get("id").asLong()).isEqualTo(messageId);

		stomp.send(s, "/app/chat/" + submissionChannel + "/send", Map.of("content", "Thưa cô, em hỏi về overview", "clientMsgId", "c-1"));
		assertThat(toTeacher.poll(5, TimeUnit.SECONDS).get("id").asLong()).as("gửi lại → cùng tin").isEqualTo(messageId);
		assertThat(history(teacher, submissionChannel, null, null).get("messages")).hasSize(1);

		JsonNode channel = channel(teacher, submissionChannel);
		assertThat(channel.get("unreadCount").asLong()).isEqualTo(1);
		assertThat(channel.get("title").asString()).contains("Bài chat – Task 2 · " + s1.getFullName());
		assertThat(channel.get("lastMessage").get("preview").asString()).isEqualTo("Thưa cô, em hỏi về overview");

		stomp.send(t, "/app/chat/" + submissionChannel + "/typing", Map.of());
		JsonNode typing = nextOfType(toStudent, "TYPING");
		assertThat(typing.get("type").asString()).isEqualTo("TYPING");
		assertThat(typing.get("userName").asString()).isEqualTo(teacherUser.getFullName());
		stomp.send(t, "/app/chat/" + submissionChannel + "/typing", Map.of());

		stomp.send(t, "/app/chat/" + submissionChannel + "/read", Map.of("lastMessageId", messageId));
		JsonNode read = nextOfType(toStudent, "READ");
		assertThat(toStudent).as("lần \"đang gõ\" thứ hai (trong 2 giây) bị bỏ qua").noneMatch(n -> "TYPING".equals(n.get("type").asString()));
		assertThat(read.get("userId").asLong()).isEqualTo(teacherUser.getId());
		assertThat(read.get("lastMessageId").asLong()).isEqualTo(messageId);
		assertThat(channel(teacher, submissionChannel).get("unreadCount").asLong()).isZero();
		JsonNode reads = history(accessToken(s1), submissionChannel, null, null).get("reads");
		assertThat(reads.get(0).get("userId").asLong()).isEqualTo(teacherUser.getId());
		assertThat(reads.get(0).get("lastMessageId").asLong()).isEqualTo(messageId);
	}

	@Test
	@DisplayName("UC-29 E1: người ngoài kênh không nghe / gửi / xem lịch sử được; mã kênh sai → 404")
	void access() throws Exception {
		String other = accessToken(s2);
		Connection c = stomp.connect(other);
		c.session().subscribe("/topic/chat/" + submissionChannel, new StompSessionHandlerAdapter() {
		});
		assertThat(c.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền nhận kênh");
		call(get("/api/chat/" + submissionChannel + "/messages"), other).andExpect(apiStatus(404));

		String otherTeacher = accessToken(newUser(Role.TEACHER));
		Connection t = stomp.connect(otherTeacher);
		stomp.send(t, "/app/chat/" + classChannel + "/send", Map.of("content", "Xin chào"));
		assertThat(t.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền gửi");
		call(get("/api/chat/abc/messages"), teacher).andExpect(apiStatus(404));
		call(get("/api/chat/class-999999999/messages"), teacher).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("Kênh lớp: mọi thành viên và giáo viên; lịch sử mới nhất trước, tải ngược bằng beforeId; danh sách kênh có lớp kể cả khi chưa có tin")
	void classChatHistory() throws Exception {
		assertThat(channel(accessToken(s2), classChannel).get("lastMessage").isNull()).isTrue();

		Connection t = stomp.connect(teacher);
		BlockingQueue<JsonNode> mine = stomp.subscribe(t, "/topic/chat/" + classChannel);
		for (int i = 1; i <= 3; i++) {
			stomp.send(t, "/app/chat/" + classChannel + "/send", Map.of("content", "Thông báo " + i));
			mine.poll(5, TimeUnit.SECONDS);
		}
		String student = accessToken(s2);
		JsonNode page1 = history(student, classChannel, null, 2).get("messages");
		assertThat(contents(page1)).containsExactly("Thông báo 3", "Thông báo 2");
		JsonNode page2 = history(student, classChannel, page1.get(1).get("id").asLong(), 2).get("messages");
		assertThat(contents(page2)).containsExactly("Thông báo 1");
		JsonNode summary = channel(student, classChannel);
		assertThat(summary.get("title").asString()).isEqualTo(classCode + " – Lớp chat");
		assertThat(summary.get("unreadCount").asLong()).isEqualTo(3);
	}

	@Test
	@DisplayName("Tin rỗng / quá 4000 ký tự → lỗi riêng người gửi trên /user/queue/errors, không lưu, không phát")
	void validation() throws Exception {
		Connection s = stomp.connect(accessToken(s1));
		BlockingQueue<JsonNode> errors = stomp.subscribe(s, "/user/queue/errors");
		BlockingQueue<JsonNode> channel = stomp.subscribe(s, "/topic/chat/" + classChannel);
		stomp.send(s, "/app/chat/" + classChannel + "/send", Map.of("content", "   "));
		JsonNode err = errors.poll(5, TimeUnit.SECONDS);
		assertThat(err.get("errorCode").asString()).isEqualTo("VALIDATION_ERROR");
		stomp.send(s, "/app/chat/" + classChannel + "/send", Map.of("content", "x".repeat(4001)));
		assertThat(errors.poll(5, TimeUnit.SECONDS)).isNotNull();
		assertThat(channel.poll(500, TimeUnit.MILLISECONDS)).isNull();
		assertThat(history(teacher, classChannel, null, null).get("messages")).isEmpty();
	}

	// ─── Hỗ trợ ─────────────────────────────────────────────────────────────

	private JsonNode history(String token, String channelId, Long beforeId, Integer size) throws Exception {
		MockHttpServletRequestBuilder req = get("/api/chat/" + channelId + "/messages");
		if (beforeId != null) {
			req.param("beforeId", String.valueOf(beforeId));
		}
		if (size != null) {
			req.param("size", String.valueOf(size));
		}
		return data(call(req, token).andExpect(apiStatus(200)));
	}

	private JsonNode channel(String token, String channelId) throws Exception {
		for (JsonNode c : data(call(get("/api/chat/channels"), token).andExpect(apiStatus(200)))) {
			if (channelId.equals(c.get("channelId").asString())) {
				return c;
			}
		}
		throw new AssertionError("Thiếu kênh " + channelId);
	}

	private static JsonNode nextOfType(BlockingQueue<JsonNode> q, String type) throws InterruptedException {
		for (int i = 0; i < 5; i++) {
			JsonNode n = q.poll(5, TimeUnit.SECONDS);
			if (n != null && type.equals(n.get("type").asString())) {
				return n;
			}
		}
		throw new AssertionError("Không nhận được " + type);
	}

	private static List<String> contents(JsonNode messages) {
		List<String> out = new ArrayList<>();
		messages.forEach(m -> out.add(m.get("content").asString()));
		return out;
	}

	private JsonNode data(ResultActions r) throws Exception {
		return json.readTree(r.andReturn().getResponse().getContentAsString()).get("data");
	}

	private ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}
}
