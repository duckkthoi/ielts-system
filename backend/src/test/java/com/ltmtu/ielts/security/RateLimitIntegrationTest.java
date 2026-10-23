package com.ltmtu.ielts.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

/** Giới hạn tần suất trong bộ nhớ (NFR-09, bước 12): đăng nhập sai theo tài khoản / theo IP, nhắc thủ công. */
class RateLimitIntegrationTest extends IntegrationTest {

	private static final AtomicInteger IP_SEQ = new AtomicInteger(1);

	/** Mỗi lần một IP riêng, để bộ đếm theo IP không lẫn giữa các ca. */
	private static String newIp() {
		return "10.77.0." + IP_SEQ.getAndIncrement();
	}

	private ResultActions login(String username, String password, String ip) throws Exception {
		return mvc.perform(postJson("/api/auth/login", Map.of("username", username, "password", password))
				.with(r -> {
					r.setRemoteAddr(ip);
					return r;
				}));
	}

	@Test
	@DisplayName("NFR-09: sai 5 lần một tài khoản → lần 6 bị 429 kể cả mật khẩu đúng, từ IP khác cũng vậy; hết 5 phút thì đăng nhập lại được")
	void perAccount() throws Exception {
		User u = newUser(Role.TEACHER);
		for (int i = 0; i < 5; i++) {
			login(u.getUsername(), "sai-mat-khau", newIp()).andExpect(status().isUnauthorized());
		}
		login(u.getUsername(), DEFAULT_PASSWORD, newIp())
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"))
				.andExpect(jsonPath("$.message").value("Bạn đã đăng nhập sai quá nhiều lần. Vui lòng thử lại sau 5 phút"));

		clock.advance(Duration.ofMinutes(5).plusSeconds(1));
		login(u.getUsername(), DEFAULT_PASSWORD, newIp()).andExpect(status().isOk());
	}

	@Test
	@DisplayName("NFR-09: một IP sai 5 lần trên nhiều tài khoản → IP đó bị chặn với mọi tài khoản; IP khác vẫn đăng nhập được")
	void perIp() throws Exception {
		String ip = newIp();
		for (int i = 0; i < 5; i++) {
			login("khong-ton-tai-" + i, "x", ip).andExpect(status().isUnauthorized());
		}
		User u = newUser(Role.STUDENT);
		login(u.getUsername(), DEFAULT_PASSWORD, ip).andExpect(status().isTooManyRequests());
		login(u.getUsername(), DEFAULT_PASSWORD, newIp()).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Đăng nhập đúng xoá bộ đếm của tài khoản: sai 4, đúng 1, sai 4 → chưa bị chặn")
	void successResetsAccount() throws Exception {
		User u = newUser(Role.TEACHER);
		for (int i = 0; i < 4; i++) {
			login(u.getUsername(), "sai", newIp()).andExpect(status().isUnauthorized());
		}
		login(u.getUsername(), DEFAULT_PASSWORD, newIp()).andExpect(status().isOk());
		for (int i = 0; i < 4; i++) {
			login(u.getUsername(), "sai", newIp()).andExpect(status().isUnauthorized());
		}
		login(u.getUsername(), DEFAULT_PASSWORD, newIp()).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Nhắc thủ công: mỗi (giáo viên, bài) tối đa 3 lần / 10 phút → lần 4 bị 429; yêu cầu sai không bị tính; hết 10 phút thì nhắc lại được")
	void remind() throws Exception {
		User teacher = newUser(Role.TEACHER);
		String token = accessToken(teacher);
		User student = newUser(Role.STUDENT);
		Map<String, Object> criterion = Map.of("code", "TR", "name", "Task Response", "step", "1", "checklist", List.of());
		Map<String, Object> part = Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>Đề</p>",
				"criteria", List.of(criterion));
		String body = mvc.perform(postJson("/api/teacher/assignments", Map.of("title", "Nhắc", "dueAt",
				clock.instant().plus(Duration.ofDays(3)).toString(), "parts", List.of(part)))
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		long id = json.readTree(body).get("data").get("id").asLong();
		mvc.perform(postJson("/api/teacher/assignments/" + id + "/publish", Map.of("studentIds", List.of(student.getId())))
				.header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isOk());

		String url = "/api/teacher/assignments/" + id + "/remind";
		mvc.perform(postJson(url, Map.of("studentIds", List.of(-1))).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isBadRequest());
		for (int i = 0; i < 3; i++) {
			mvc.perform(postJson(url, Map.of("studentIds", List.of(student.getId())))
					.header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isOk());
		}
		mvc.perform(postJson(url, Map.of("studentIds", List.of(student.getId())))
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"));

		clock.advance(Duration.ofMinutes(10).plusSeconds(1));
		mvc.perform(postJson(url, Map.of("studentIds", List.of(student.getId())))
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken(teacher)))).andExpect(status().isOk());
	}

	@Test
	@DisplayName("IP bị chặn đăng nhập vẫn gọi được các API khác (refresh trả lỗi của chính nó, không phải 429)")
	void otherEndpointsUnaffected() throws Exception {
		String ip = newIp();
		for (int i = 0; i < 5; i++) {
			login("ai-do-" + i, "x", ip);
		}
		mvc.perform(post("/api/auth/refresh").contentType("application/json").content("{\"refreshToken\":\"x\"}")
				.with(r -> {
					r.setRemoteAddr(ip);
					return r;
				})).andExpect(status().isUnauthorized());
	}
}
