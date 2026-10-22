package com.ltmtu.ielts.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;

/** Khung lỗi chung theo docs/05-api.md mục A.1. */
class ErrorResponseIntegrationTest extends IntegrationTest {

	@Test
	@DisplayName("Health trả UP, không cần đăng nhập")
	void health_isPublicAndUp() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	@DisplayName("Giờ server công khai (SD-11)")
	void serverTime_isPublic() throws Exception {
		mvc.perform(get("/api/time"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.serverTime").value(clock.instant().toString()));
	}

	@Test
	@DisplayName("Đường dẫn không tồn tại (đã đăng nhập) → 404 NOT_FOUND đúng khung lỗi, có data: null")
	void unknownPath_returns404WithErrorBody() throws Exception {
		String token = accessToken(newUser(Role.TEACHER));
		mvc.perform(get("/api/khong-ton-tai").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").isEmpty())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Không tìm thấy đường dẫn"))
				.andExpect(jsonPath("$.path").value("/api/khong-ton-tai"));
	}
}
