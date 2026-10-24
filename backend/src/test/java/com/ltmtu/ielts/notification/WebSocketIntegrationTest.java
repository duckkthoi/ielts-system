package com.ltmtu.ielts.notification;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/**
 * WebSocket STOMP thật qua cổng ngẫu nhiên (docs/05-api.md phần B): xác thực khung CONNECT, phân quyền SUBSCRIBE,
 * nhận thông báo realtime đúng người (SD-05).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest extends IntegrationTest {

	private static final String QUEUE = "/user/queue/notifications";

	@LocalServerPort
	private int port;

	private final WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
	private final List<StompSession> sessions = new ArrayList<>();

	@AfterEach
	void disconnect() {
		sessions.forEach(WebSocketIntegrationTest::disconnectQuietly);
		client.stop();
	}

	/** Server có thể vừa đóng kết nối (sau khung ERROR) mà client chưa kịp biết → disconnect ném lỗi; bỏ qua. */
	private static void disconnectQuietly(StompSession s) {
		try {
			if (s.isConnected()) {
				s.disconnect();
			}
		} catch (RuntimeException ignored) {
			// Kết nối đã đóng.
		}
	}

	/** Ghi lại khung ERROR server gửi về (header {@code message}). */
	private static final class ErrorRecorder extends StompSessionHandlerAdapter {
		final BlockingQueue<String> errors = new LinkedBlockingQueue<>();

		@Override
		public Type getPayloadType(StompHeaders headers) {
			return byte[].class;
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			errors.add(String.valueOf(headers.getFirst("message")));
		}
	}

	private CompletableFuture<StompSession> connect(String token, ErrorRecorder handler) {
		StompHeaders headers = new StompHeaders();
		if (token != null) {
			headers.add(HttpHeaders.AUTHORIZATION, bearer(token));
		}
		return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers, handler);
	}

	private StompSession connected(String token) throws Exception {
		StompSession s = connect(token, new ErrorRecorder()).get(5, TimeUnit.SECONDS);
		sessions.add(s);
		return s;
	}

	/** SUBSCRIBE rồi chờ một chút để broker đăng ký xong (simple broker không hỗ trợ RECEIPT). */
	private BlockingQueue<JsonNode> subscribe(StompSession session) throws InterruptedException {
		BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();
		session.subscribe(QUEUE, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return byte[].class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				received.add(json.readTree(new String((byte[]) payload, StandardCharsets.UTF_8)));
			}
		});
		Thread.sleep(300);
		return received;
	}

	private void publishTo(String teacherToken, Set<Long> studentIds) throws Exception {
		Map<String, Object> part = Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>Đề</p>",
				"criteria", List.of(Map.of("code", "TR", "name", "Task Response", "checklist", List.of())));
		Map<String, Object> body = Map.of("title", "Bài realtime", "dueAt", "2027-01-05T16:59:00Z", "parts", List.of(part));
		String created = mvc.perform(postJson("/api/teacher/assignments", body)
				.header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))).andExpect(apiStatus(201))
				.andReturn().getResponse().getContentAsString();
		long id = json.readTree(created).get("data").get("id").asLong();
		mvc.perform(postJson("/api/teacher/assignments/" + id + "/publish", Map.of("studentIds", studentIds))
				.header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))).andExpect(apiStatus(200));
	}

	@Test
	@DisplayName("SD-05: phát hành → sinh viên đang online nhận thông báo qua /user/queue/notifications; người khác không nhận")
	void pushToRecipientOnly() throws Exception {
		User student = newUser(Role.STUDENT);
		User bystander = newUser(Role.STUDENT);
		BlockingQueue<JsonNode> mine = subscribe(connected(accessToken(student)));
		BlockingQueue<JsonNode> others = subscribe(connected(accessToken(bystander)));

		publishTo(accessToken(newUser(Role.TEACHER)), Set.of(student.getId()));

		JsonNode n = mine.poll(5, TimeUnit.SECONDS);
		assertThat(n).isNotNull();
		assertThat(n.get("type").asString()).isEqualTo("ASSIGNMENT_PUBLISHED");
		assertThat(n.get("title").asString()).isEqualTo("Bài tập mới: Bài realtime");
		assertThat(n.get("id").asLong()).isPositive();
		assertThat(others.poll(500, TimeUnit.MILLISECONDS)).isNull();
	}

	@Test
	@DisplayName("CONNECT thiếu token / token sai → khung ERROR, không kết nối được")
	void connectWithoutValidToken() {
		for (String token : new String[] { null, "abc.def.ghi" }) {
			ErrorRecorder handler = new ErrorRecorder();
			Throwable failure = catchThrowable(() -> connect(token, handler).get(5, TimeUnit.SECONDS));
			assertThat(failure).isNotNull();
			assertThat(handler.errors).anyMatch(m -> m.contains("Phiên đăng nhập không hợp lệ"));
		}
	}

	@Test
	@DisplayName("Đăng xuất (phiên bị thu hồi) → token cũ không CONNECT được nữa; tài khoản bị bắt đổi mật khẩu → từ chối")
	void revokedOrMustChangePassword() throws Exception {
		User student = newUser(Role.STUDENT);
		JsonNode login = login(student.getUsername(), DEFAULT_PASSWORD);
		String token = login.get("accessToken").asString();
		mvc.perform(postJson("/api/auth/logout", Map.of("refreshToken", login.get("refreshToken").asString())))
				.andExpect(apiStatus(200));
		assertThat(catchThrowable(() -> connect(token, new ErrorRecorder()).get(5, TimeUnit.SECONDS))).isNotNull();

		User fresh = newUser(Role.STUDENT, true);
		ErrorRecorder handler = new ErrorRecorder();
		assertThat(catchThrowable(() -> connect(accessToken(fresh), handler).get(5, TimeUnit.SECONDS))).isNotNull();
		assertThat(handler.errors).anyMatch(m -> m.contains("đổi mật khẩu"));
	}

	@Test
	@DisplayName("Phân quyền SUBSCRIBE / SEND: kênh /topic của bài không phải của mình, lệnh gửi không đúng vai trò → khung ERROR")
	void forbiddenDestinations() throws Exception {
		String token = accessToken(newUser(Role.TEACHER));
		ErrorRecorder handler = new ErrorRecorder();
		StompSession s = connect(token, handler).get(5, TimeUnit.SECONDS);
		sessions.add(s);
		s.subscribe("/topic/assignments/1/progress", new StompSessionHandlerAdapter() {
		});
		assertThat(handler.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền nhận kênh");

		ErrorRecorder handler2 = new ErrorRecorder();
		StompSession s2 = connect(token, handler2).get(5, TimeUnit.SECONDS);
		sessions.add(s2);
		s2.send("/app/presence/viewing", "{}".getBytes(StandardCharsets.UTF_8));
		assertThat(handler2.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền gửi");
	}

	@Test
	@DisplayName("Đọc thông báo vừa nhận qua REST (đánh dấu đã đọc) — WebSocket và REST dùng cùng NotificationDto")
	void sameDtoOverRest() throws Exception {
		User student = newUser(Role.STUDENT);
		String token = accessToken(student);
		BlockingQueue<JsonNode> mine = subscribe(connected(token));
		publishTo(accessToken(newUser(Role.TEACHER)), Set.of(student.getId()));
		JsonNode pushed = mine.poll(5, TimeUnit.SECONDS);
		assertThat(pushed).isNotNull();
		mvc.perform(post("/api/notifications/" + pushed.get("id").asLong() + "/read")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(apiStatus(200));
	}
}
