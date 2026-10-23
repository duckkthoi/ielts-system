package com.ltmtu.ielts.support;

import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kiểm tra kết quả API khi HTTP status luôn là 200 (docs/05 mục A.1): mã thật nằm trong trường {@code status} của
 * body lỗi; response thành công (JSON {@code success: true}, hoặc file) được coi là 200.
 */
public final class ApiStatus {

	private static final JsonMapper MAPPER = new JsonMapper();

	private ApiStatus() {
	}

	/**
	 * HTTP 200 và mã trong body đúng {@code expected}. Mã thành công (2xx, vd 201 cũ) đều quy về 200: nghĩa là
	 * request thành công, không phải body lỗi.
	 */
	public static ResultMatcher apiStatus(int expected) {
		return result -> {
			int http = result.getResponse().getStatus();
			if (http != 200) {
				throw new AssertionError("HTTP status phải luôn là 200 nhưng là " + http);
			}
			int want = expected < 300 ? 200 : expected;
			int actual = apiStatusOf(result);
			if (actual != want) {
				throw new AssertionError("Mã trong body: mong đợi " + want + " nhưng là " + actual + " — "
						+ result.getResponse().getContentAsString(StandardCharsets.UTF_8));
			}
		};
	}

	/** Mã kết quả của một response: {@code status} trong body lỗi, còn lại là 200. */
	public static int apiStatusOf(MvcResult result) throws Exception {
		MockHttpServletResponse res = result.getResponse();
		if (res.getStatus() != 200) {
			return res.getStatus();
		}
		String type = res.getContentType();
		if (type == null || !MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(type))) {
			return 200;
		}
		String body = res.getContentAsString(StandardCharsets.UTF_8);
		if (body.isBlank()) {
			return 200;
		}
		JsonNode node = MAPPER.readTree(body);
		return node.has("success") && !node.get("success").asBoolean() ? node.path("status").asInt() : 200;
	}
}
