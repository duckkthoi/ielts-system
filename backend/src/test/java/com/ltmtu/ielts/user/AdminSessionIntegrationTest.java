package com.ltmtu.ielts.user;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

/** UC-08: admin xem / thu hồi / dọn phiên đăng nhập — docs/05-api.md mục A.3. */
class AdminSessionIntegrationTest extends IntegrationTest {

	private static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder req) {
		return req.header(HttpHeaders.AUTHORIZATION, bearer(token));
	}

	/** Phiên của một người trong danh sách (lọc theo tên đăng nhập). */
	private JsonNode sessionsOf(String adminToken, String username, String status) throws Exception {
		MockHttpServletRequestBuilder req = get("/api/admin/sessions").param("keyword", username);
		if (status != null) {
			req.param("status", status);
		}
		String body = mvc.perform(as(adminToken, req)).andExpect(apiStatus(200)).andReturn().getResponse()
				.getContentAsString();
		return json.readTree(body);
	}

	@Test
	@DisplayName("UC-08: chỉ admin xem được danh sách phiên")
	void onlyAdmin() throws Exception {
		String teacherToken = accessToken(newUser(Role.TEACHER));
		mvc.perform(as(teacherToken, get("/api/admin/sessions"))).andExpect(apiStatus(403));
		mvc.perform(get("/api/admin/sessions")).andExpect(apiStatus(401));
	}

	@Test
	@DisplayName("UC-08: lọc theo tên + trạng thái, phân trang; IP che, không có token, cờ current đúng phiên admin")
	void searchSafeFields() throws Exception {
		User admin = newUser(Role.ADMIN);
		String adminToken = accessToken(admin);
		User student = newUser(Role.STUDENT);
		login(student.getUsername(), DEFAULT_PASSWORD);

		JsonNode page = sessionsOf(adminToken, student.getUsername(), "ACTIVE");
		assertThat(page.get("totalRecord").asLong()).isEqualTo(1);
		JsonNode row = page.get("data").get(0);
		assertThat(row.get("username").asString()).isEqualTo(student.getUsername());
		assertThat(row.get("status").asString()).isEqualTo("ACTIVE");
		assertThat(row.get("ipMasked").asString()).endsWith(".*");
		assertThat(row.get("current").asBoolean()).isFalse();
		assertThat(row.has("refreshTokenHash")).isFalse();
		assertThat(row.has("ip")).isFalse();
		assertThat(row.toString()).doesNotContain("token", "Token");

		JsonNode mine = sessionsOf(adminToken, admin.getUsername(), null).get("data").get(0);
		assertThat(mine.get("current").asBoolean()).isTrue();

		mvc.perform(as(adminToken, get("/api/admin/sessions").param("role", "STUDENT").param("size", "1")))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data", hasSize(1)))
				.andExpect(jsonPath("$.data[0].role").value("STUDENT"));
		mvc.perform(as(adminToken, get("/api/admin/sessions").param("sort", "refreshTokenHash,asc")))
				.andExpect(apiStatus(400));
	}

	@Test
	@DisplayName("UC-08: thu hồi một phiên → access token và refresh token đó bị từ chối; thu hồi lại không lỗi; có nhật ký")
	void revokeOne() throws Exception {
		String adminToken = accessToken(newUser(Role.ADMIN));
		User student = newUser(Role.STUDENT);
		JsonNode session = login(student.getUsername(), DEFAULT_PASSWORD);
		long sessionId = sessionsOf(adminToken, student.getUsername(), "ACTIVE").get("data").get(0).get("sessionId")
				.asLong();

		mvc.perform(as(adminToken, delete("/api/admin/sessions/" + sessionId)))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.status").value("REVOKED"))
				.andExpect(jsonPath("$.data.revokeReason").value("ADMIN_FORCED"));

		mvc.perform(as(session.get("accessToken").asString(), get("/api/auth/me"))).andExpect(apiStatus(401));
		mvc.perform(postJson("/api/auth/refresh", Map.of("refreshToken", session.get("refreshToken").asString())))
				.andExpect(apiStatus(401));

		mvc.perform(as(adminToken, delete("/api/admin/sessions/" + sessionId)))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.status").value("REVOKED"));
		mvc.perform(as(adminToken, get("/api/admin/audit-logs").param("action", "REVOKE_SESSION")))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data[0].targetId").value(sessionId));
		mvc.perform(as(adminToken, delete("/api/admin/sessions/999999999"))).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("UC-08: không thu hồi phiên admin đang dùng → 400 CURRENT_SESSION")
	void cannotRevokeCurrent() throws Exception {
		User admin = newUser(Role.ADMIN);
		String adminToken = accessToken(admin);
		long mine = sessionsOf(adminToken, admin.getUsername(), "ACTIVE").get("data").get(0).get("sessionId").asLong();

		mvc.perform(as(adminToken, delete("/api/admin/sessions/" + mine)))
				.andExpect(apiStatus(400))
				.andExpect(jsonPath("$.errorCode").value("CURRENT_SESSION"));
		mvc.perform(as(adminToken, get("/api/auth/me"))).andExpect(apiStatus(200));
	}

	@Test
	@DisplayName("UC-08: thu hồi mọi phiên của một tài khoản → trả số phiên, token cũ bị từ chối")
	void revokeAllOfUser() throws Exception {
		String adminToken = accessToken(newUser(Role.ADMIN));
		User teacher = newUser(Role.TEACHER);
		String teacherToken = accessToken(teacher);

		mvc.perform(as(adminToken, post("/api/admin/users/" + teacher.getId() + "/sessions/revoke")))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.count").value(1));
		mvc.perform(as(teacherToken, get("/api/auth/me"))).andExpect(apiStatus(401));

		mvc.perform(as(adminToken, post("/api/admin/users/" + teacher.getId() + "/sessions/revoke")))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.count").value(0));
	}

	@Test
	@DisplayName("UC-08: phiên hết hạn → lọc EXPIRED, không thu hồi được (409), dọn thì xoá, refresh vẫn bị từ chối")
	void expiredSessions() throws Exception {
		User student = newUser(Role.STUDENT);
		JsonNode session = login(student.getUsername(), DEFAULT_PASSWORD);
		clock.advance(Duration.ofDays(8));
		String adminToken = accessToken(newUser(Role.ADMIN));

		JsonNode expired = sessionsOf(adminToken, student.getUsername(), "EXPIRED");
		assertThat(expired.get("totalRecord").asLong()).isEqualTo(1);
		long sessionId = expired.get("data").get(0).get("sessionId").asLong();
		assertThat(sessionsOf(adminToken, student.getUsername(), "ACTIVE").get("totalRecord").asLong()).isZero();

		mvc.perform(as(adminToken, delete("/api/admin/sessions/" + sessionId)))
				.andExpect(apiStatus(409))
				.andExpect(jsonPath("$.errorCode").value("INVALID_STATE"));
		mvc.perform(postJson("/api/auth/refresh", Map.of("refreshToken", session.get("refreshToken").asString())))
				.andExpect(apiStatus(401));

		String body = mvc.perform(as(adminToken, post("/api/admin/sessions/cleanup"))).andExpect(apiStatus(200))
				.andReturn().getResponse().getContentAsString();
		assertThat(json.readTree(body).get("data").get("count").asInt()).isGreaterThanOrEqualTo(1);
		assertThat(sessionsOf(adminToken, student.getUsername(), null).get("totalRecord").asLong()).isZero();
	}
}
