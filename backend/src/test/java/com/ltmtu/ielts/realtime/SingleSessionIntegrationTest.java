package com.ltmtu.ielts.realtime;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static com.ltmtu.ielts.support.ApiStatus.apiStatusOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
import org.springframework.test.context.TestPropertySource;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.support.StompTestClient;
import com.ltmtu.ielts.support.StompTestClient.Connection;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/**
 * Một tài khoản một phiên (FR-06, SD-01) với WebSocket thật: phiên cũ nhận {@code FORCE_LOGOUT} trên
 * {@code /user/queue/session} rồi bị server đóng kết nối. Profile test tắt chính sách này, nên lớp test này bật riêng.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.session.single-login=true")
class SingleSessionIntegrationTest extends IntegrationTest {

	private static final String SESSION_QUEUE = "/user/queue/session";

	@LocalServerPort
	private int port;

	private StompTestClient stomp;

	@BeforeEach
	void setUp() {
		stomp = new StompTestClient(port, json);
	}

	@AfterEach
	void tearDown() {
		stomp.close();
	}

	private int me(String token) throws Exception {
		return apiStatusOf(mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn());
	}

	@Test
	@DisplayName("FR-06: đăng nhập máy 2 → máy 1 nhận FORCE_LOGOUT (NEW_LOGIN) và bị ngắt; token máy 1 hết dùng được; máy 2 không bị ảnh hưởng")
	void newLoginKicksOldDevice() throws Exception {
		User u = newUser(Role.STUDENT);
		String first = accessToken(u);
		Connection device1 = stomp.connect(first);
		BlockingQueue<JsonNode> kicked = stomp.subscribe(device1, SESSION_QUEUE);

		String second = accessToken(u);
		JsonNode msg = kicked.poll(5, TimeUnit.SECONDS);
		assertThat(msg).isNotNull();
		assertThat(msg.get("type").asString()).isEqualTo("FORCE_LOGOUT");
		assertThat(msg.get("reason").asString()).isEqualTo("NEW_LOGIN");
		assertThat(device1.awaitClosed(5000)).as("server đóng kết nối của phiên cũ").isTrue();

		assertThat(me(first)).isEqualTo(401);
		assertThat(me(second)).isEqualTo(200);
		assertThat(catchThrowable(() -> stomp.connect(first))).as("token cũ không CONNECT được").isNotNull();

		Connection device2 = stomp.connect(second);
		BlockingQueue<JsonNode> mine = stomp.subscribe(device2, SESSION_QUEUE);
		assertThat(mine.poll(800, TimeUnit.MILLISECONDS)).isNull();
		assertThat(device2.session().isConnected()).isTrue();
	}

	@Test
	@DisplayName("Admin khoá tài khoản → thiết bị đang mở nhận FORCE_LOGOUT (ACCOUNT_LOCKED)")
	void lockKicks() throws Exception {
		User u = newUser(Role.TEACHER);
		Connection c = stomp.connect(accessToken(u));
		BlockingQueue<JsonNode> kicked = stomp.subscribe(c, SESSION_QUEUE);
		String admin = accessToken(newUser(Role.ADMIN));
		mvc.perform(post("/api/admin/users/" + u.getId() + "/lock").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(apiStatus(200));
		assertThat(kicked.poll(5, TimeUnit.SECONDS).get("reason").asString()).isEqualTo("ACCOUNT_LOCKED");
		assertThat(c.awaitClosed(5000)).isTrue();
	}

	@Test
	@DisplayName("Đổi mật khẩu → phiên khác bị đá (PASSWORD_CHANGED), phiên đang đổi vẫn giữ")
	void changePasswordKicksOthers() throws Exception {
		User u = newUser(Role.TEACHER);
		String old = accessToken(u);
		Connection oldDevice = stomp.connect(old);
		BlockingQueue<JsonNode> kicked = stomp.subscribe(oldDevice, SESSION_QUEUE);

		String current = accessToken(u); // đá phiên "old" (NEW_LOGIN) — bỏ qua tin này
		assertThat(kicked.poll(5, TimeUnit.SECONDS).get("reason").asString()).isEqualTo("NEW_LOGIN");

		Connection currentDevice = stomp.connect(current);
		BlockingQueue<JsonNode> mine = stomp.subscribe(currentDevice, SESSION_QUEUE);
		mvc.perform(postJson("/api/auth/change-password", Map.of("currentPassword", DEFAULT_PASSWORD,
				"newPassword", "MatKhauMoi#2026")).header(HttpHeaders.AUTHORIZATION, bearer(current)))
				.andExpect(apiStatus(200));
		assertThat(mine.poll(800, TimeUnit.MILLISECONDS)).as("phiên đang đổi mật khẩu không bị đá").isNull();
		assertThat(me(current)).isEqualTo(200);
	}
}
