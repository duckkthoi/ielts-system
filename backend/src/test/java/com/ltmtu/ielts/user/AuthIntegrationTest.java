package com.ltmtu.ielts.user;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import com.ltmtu.ielts.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

/** UC-01 → UC-03, SD-01, SD-02 — docs/05-api.md mục A.2. */
class AuthIntegrationTest extends IntegrationTest {

	@Autowired
	private UserSessionRepository sessionRepository;

	private String refresh(String refreshToken, int expectedStatus) throws Exception {
		return mvc.perform(postJson("/api/auth/refresh", Map.of("refreshToken", refreshToken)))
				.andExpect(apiStatus(expectedStatus))
				.andReturn().getResponse().getContentAsString();
	}

	private void expectMe(String accessToken, int expectedStatus) throws Exception {
		mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
				.andExpect(apiStatus(expectedStatus));
	}

	@Nested
	@DisplayName("Đăng nhập")
	class Login {

		@Test
		@DisplayName("UC-01: đúng tài khoản → cặp token + thông tin user, tạo phiên trong DB")
		void success() throws Exception {
			User u = newUser(Role.TEACHER);
			JsonNode data = login(u.getUsername(), DEFAULT_PASSWORD);

			assertThat(data.get("accessToken").asString()).isNotBlank();
			assertThat(data.get("refreshToken").asString()).hasSize(43);
			assertThat(data.get("user").get("role").asString()).isEqualTo("TEACHER");
			assertThat(data.get("user").get("mustChangePassword").asBoolean()).isFalse();
			assertThat(userRepository.findById(u.getId()).orElseThrow().getLastLoginAt()).isNotNull();
		}

		@Test
		@DisplayName("UC-01 E1: sai mật khẩu và sai tên đăng nhập trả CÙNG một lỗi")
		void badCredentials_sameMessage() throws Exception {
			User u = newUser(Role.STUDENT);
			for (var creds : new String[][] { { u.getUsername(), "sai-mat-khau" }, { "khong_ton_tai", "x" } }) {
				mvc.perform(postJson("/api/auth/login", Map.of("username", creds[0], "password", creds[1])))
						.andExpect(apiStatus(401))
						.andExpect(jsonPath("$.errorCode").value("BAD_CREDENTIALS"))
						.andExpect(jsonPath("$.errorMessage").value("Tên đăng nhập hoặc mật khẩu không đúng"));
			}
		}

		@Test
		@DisplayName("UC-01 E2: tài khoản bị khoá → 403 ACCOUNT_LOCKED")
		void lockedAccount() throws Exception {
			User u = newUser(Role.STUDENT);
			u.setActive(false);
			userRepository.save(u);

			mvc.perform(postJson("/api/auth/login", Map.of("username", u.getUsername(), "password", DEFAULT_PASSWORD)))
					.andExpect(apiStatus(403))
					.andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
		}

		@Test
		@DisplayName("Thiếu trường → 400 VALIDATION_ERROR kèm lỗi từng trường")
		void validation() throws Exception {
			mvc.perform(postJson("/api/auth/login", Map.of("username", "")))
					.andExpect(apiStatus(400))
					.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
					.andExpect(jsonPath("$.fieldErrors.username").exists())
					.andExpect(jsonPath("$.fieldErrors.password").exists());
		}
	}

	@Nested
	@DisplayName("Access token")
	class AccessToken {

		@Test
		@DisplayName("Có token hợp lệ → /me trả đúng người dùng")
		void me() throws Exception {
			User u = newUser(Role.STUDENT);
			mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken(u))))
					.andExpect(apiStatus(200))
					.andExpect(jsonPath("$.data.username").value(u.getUsername()))
					.andExpect(jsonPath("$.data.studentCode").value(u.getStudentCode()));
		}

		@Test
		@DisplayName("Không có token / token rác → 401 UNAUTHORIZED dạng JSON")
		void missingOrGarbageToken() throws Exception {
			mvc.perform(get("/api/auth/me"))
					.andExpect(apiStatus(401))
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
			mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer("abc.def.ghi")))
					.andExpect(apiStatus(401));
		}

		@Test
		@DisplayName("NFR-07: access token quá 30 phút → 401")
		void expired() throws Exception {
			String token = accessToken(newUser(Role.TEACHER));
			clock.advance(Duration.ofMinutes(31));
			expectMe(token, 401);
		}

		@Test
		@DisplayName("Sai vai trò → 403 FORBIDDEN dạng JSON")
		void wrongRole() throws Exception {
			String token = accessToken(newUser(Role.TEACHER));
			mvc.perform(get("/api/admin/teachers").header(HttpHeaders.AUTHORIZATION, bearer(token)))
					.andExpect(apiStatus(403))
					.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		}

		@Test
		@DisplayName("BR-02: đang bị bắt đổi mật khẩu → chỉ gọi được /api/auth/*, còn lại 403 PASSWORD_CHANGE_REQUIRED")
		void mustChangePassword_blocksOtherApis() throws Exception {
			User u = newUser(Role.ADMIN, true);
			String token = accessToken(u);

			expectMe(token, 200);
			mvc.perform(get("/api/admin/teachers").header(HttpHeaders.AUTHORIZATION, bearer(token)))
					.andExpect(apiStatus(403))
					.andExpect(jsonPath("$.errorCode").value("PASSWORD_CHANGE_REQUIRED"));
		}

		@Test
		@DisplayName("Tài khoản bị khoá sau khi đăng nhập → token đang dùng bị từ chối ngay")
		void lockedAfterLogin_tokenRejectedImmediately() throws Exception {
			User u = newUser(Role.STUDENT);
			String token = accessToken(u);
			u.setActive(false);
			userRepository.save(u);

			expectMe(token, 401);
		}
	}

	@Nested
	@DisplayName("Refresh token xoay vòng (SD-02)")
	class Refresh {

		@Test
		@DisplayName("Refresh → cặp token mới; token mới dùng được")
		void rotates() throws Exception {
			JsonNode first = login(newUser(Role.STUDENT).getUsername(), DEFAULT_PASSWORD);
			JsonNode second = json.readTree(refresh(first.get("refreshToken").asString(), 200)).get("data");

			assertThat(second.get("refreshToken").asString()).isNotEqualTo(first.get("refreshToken").asString());
			expectMe(second.get("accessToken").asString(), 200);
			refresh(second.get("refreshToken").asString(), 200);
		}

		@Test
		@DisplayName("Token cũ gửi lại TRONG 30 giây (2 tab) → 401 REFRESH_TOKEN_ROTATED, phiên vẫn còn")
		void reuseWithinGrace_notRevoked() throws Exception {
			JsonNode first = login(newUser(Role.STUDENT).getUsername(), DEFAULT_PASSWORD);
			String oldRefresh = first.get("refreshToken").asString();
			JsonNode second = json.readTree(refresh(oldRefresh, 200)).get("data");

			clock.advance(Duration.ofSeconds(10));
			assertThat(json.readTree(refresh(oldRefresh, 401)).get("errorCode").asString())
					.isEqualTo("REFRESH_TOKEN_ROTATED");
			refresh(second.get("refreshToken").asString(), 200);
		}

		@Test
		@DisplayName("Token cũ gửi lại SAU 30 giây → nghi bị đánh cắp, thu hồi MỌI phiên của user")
		void reuseAfterGrace_revokesAllSessions() throws Exception {
			User u = newUser(Role.STUDENT);
			JsonNode otherDevice = login(u.getUsername(), DEFAULT_PASSWORD);
			JsonNode first = login(u.getUsername(), DEFAULT_PASSWORD);
			String oldRefresh = first.get("refreshToken").asString();
			JsonNode second = json.readTree(refresh(oldRefresh, 200)).get("data");

			clock.advance(Duration.ofSeconds(31));
			assertThat(json.readTree(refresh(oldRefresh, 401)).get("errorCode").asString()).isEqualTo("UNAUTHORIZED");

			refresh(second.get("refreshToken").asString(), 401);
			refresh(otherDevice.get("refreshToken").asString(), 401);
			expectMe(second.get("accessToken").asString(), 401);
			expectMe(otherDevice.get("accessToken").asString(), 401);
		}

		@Test
		@DisplayName("Refresh token quá 7 ngày → 401")
		void expired() throws Exception {
			JsonNode tokens = login(newUser(Role.TEACHER).getUsername(), DEFAULT_PASSWORD);
			clock.advance(Duration.ofDays(7).plusMinutes(1));
			refresh(tokens.get("refreshToken").asString(), 401);
		}
	}

	@Nested
	@DisplayName("Đăng xuất và đổi mật khẩu")
	class LogoutAndPassword {

		@Test
		@DisplayName("UC-03: đăng xuất → refresh token và access token của phiên đó hết dùng được ngay")
		void logout() throws Exception {
			JsonNode tokens = login(newUser(Role.STUDENT).getUsername(), DEFAULT_PASSWORD);
			String refreshToken = tokens.get("refreshToken").asString();

			mvc.perform(postJson("/api/auth/logout", Map.of("refreshToken", refreshToken)))
					.andExpect(apiStatus(200));

			refresh(refreshToken, 401);
			expectMe(tokens.get("accessToken").asString(), 401);
		}

		@Test
		@DisplayName("UC-02 E1: mật khẩu hiện tại sai → 400 WRONG_PASSWORD")
		void wrongCurrentPassword() throws Exception {
			String token = accessToken(newUser(Role.STUDENT));
			mvc.perform(postJson("/api/auth/change-password",
					Map.of("currentPassword", "sai", "newPassword", "MatKhauMoi123"))
					.header(HttpHeaders.AUTHORIZATION, bearer(token)))
					.andExpect(apiStatus(400))
					.andExpect(jsonPath("$.errorCode").value("WRONG_PASSWORD"));
		}

		@Test
		@DisplayName("UC-02 E2: mật khẩu mới yếu (không có số / quá ngắn) → 400 VALIDATION_ERROR")
		void weakNewPassword() throws Exception {
			String token = accessToken(newUser(Role.STUDENT));
			for (String weak : new String[] { "chicochu", "Ab1" }) {
				mvc.perform(postJson("/api/auth/change-password",
						Map.of("currentPassword", DEFAULT_PASSWORD, "newPassword", weak))
						.header(HttpHeaders.AUTHORIZATION, bearer(token)))
						.andExpect(apiStatus(400))
						.andExpect(jsonPath("$.fieldErrors.newPassword").exists());
			}
		}

		@Test
		@DisplayName("UC-02 + BR-02: đổi thành công → hết bị bắt đổi, phiên khác bị thu hồi, phiên hiện tại vẫn dùng được")
		void success() throws Exception {
			User u = newUser(Role.STUDENT, true);
			JsonNode otherDevice = login(u.getUsername(), DEFAULT_PASSWORD);
			String current = accessToken(u);

			mvc.perform(postJson("/api/auth/change-password",
					Map.of("currentPassword", DEFAULT_PASSWORD, "newPassword", "MatKhauMoi123"))
					.header(HttpHeaders.AUTHORIZATION, bearer(current)))
					.andExpect(apiStatus(200))
					.andExpect(jsonPath("$.data.mustChangePassword").value(false));

			expectMe(current, 200);
			expectMe(otherDevice.get("accessToken").asString(), 401);
			refresh(otherDevice.get("refreshToken").asString(), 401);
			login(u.getUsername(), "MatKhauMoi123");
			assertThat(sessionRepository.findAll().stream()
					.filter(s -> s.getUser().getId().equals(u.getId()) && s.getRevokeReason() == RevokeReason.PASSWORD_CHANGED))
					.isNotEmpty();
		}
	}
}
