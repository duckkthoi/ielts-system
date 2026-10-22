package com.ltmtu.ielts.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;
import com.ltmtu.ielts.user.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Nền chung cho integration test: chạy trên DB {@code ielts_test}, đồng hồ {@link MutableClock}.
 * DB dùng chung giữa các lần chạy, nên mỗi test tự tạo dữ liệu riêng với tên duy nhất ({@link #newUser}) và không
 * dựa vào số lượng bản ghi trong bảng.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
public abstract class IntegrationTest {

	protected static final String DEFAULT_PASSWORD = "Passw0rd!";

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected ObjectMapper json;

	@Autowired
	protected MutableClock clock;

	@Autowired
	protected CapturingMailSender mail;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	@BeforeEach
	void resetClock() {
		clock.reset();
	}

	/** Tạo tài khoản mới với tên đăng nhập duy nhất, mật khẩu {@link #DEFAULT_PASSWORD}. */
	protected User newUser(Role role, boolean mustChangePassword) {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		User u = new User();
		u.setUsername(role.name().toLowerCase() + "_" + suffix);
		u.setFullName("Test " + suffix);
		u.setEmail(suffix + "@test.local");
		u.setRole(role);
		if (role == Role.STUDENT) {
			u.setStudentCode("SV" + suffix);
		}
		u.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
		u.setMustChangePassword(mustChangePassword);
		return userRepository.save(u);
	}

	protected User newUser(Role role) {
		return newUser(role, false);
	}

	/** Đăng nhập và trả về {@code data} của response (accessToken, refreshToken, user). */
	protected JsonNode login(String username, String password) throws Exception {
		String body = mvc.perform(postJson("/api/auth/login", Map.of("username", username, "password", password)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("data");
	}

	protected String accessToken(User user) throws Exception {
		return login(user.getUsername(), DEFAULT_PASSWORD).get("accessToken").asString();
	}

	protected MockHttpServletRequestBuilder postJson(String url, Object body) {
		return post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
	}

	protected static String bearer(String token) {
		return "Bearer " + token;
	}
}
