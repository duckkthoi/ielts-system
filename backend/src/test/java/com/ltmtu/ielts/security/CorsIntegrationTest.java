package com.ltmtu.ielts.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;

import com.ltmtu.ielts.support.IntegrationTest;

/** CORS nhận cả mẫu origin (vd domain tunnel khi demo) — cùng cấu hình với endpoint WebSocket. */
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000,https://*.trycloudflare.com")
class CorsIntegrationTest extends IntegrationTest {

	private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder preflight(String origin) {
		return options("/api/auth/login").header(HttpHeaders.ORIGIN, origin)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
	}

	@Test
	@DisplayName("Origin khớp mẫu https://*.trycloudflare.com → được nhận")
	void patternOriginAllowed() throws Exception {
		mvc.perform(preflight("https://abc-xyz.trycloudflare.com")).andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://abc-xyz.trycloudflare.com"));
	}

	@Test
	@DisplayName("Origin ghi đích danh vẫn được nhận như trước")
	void exactOriginAllowed() throws Exception {
		mvc.perform(preflight("http://localhost:3000")).andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
	}

	@Test
	@DisplayName("Origin không có trong danh sách → 403")
	void unknownOriginRejected() throws Exception {
		mvc.perform(preflight("https://evil.example.com")).andExpect(status().isForbidden());
	}
}
