package com.ltmtu.ielts.grading;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;

import tools.jackson.databind.JsonNode;

/** BR-21, BR-22 — mã lỗi và ngân hàng nhận xét của giáo viên. */
class FeedbackLibraryIntegrationTest extends IntegrationTest {

	private String teacher;
	private String otherTeacher;

	@BeforeEach
	void setUp() throws Exception {
		teacher = accessToken(newUser(Role.TEACHER));
		otherTeacher = accessToken(newUser(Role.TEACHER));
	}

	private JsonNode data(ResultActions r) throws Exception {
		return json.readTree(r.andReturn().getResponse().getContentAsString()).get("data");
	}

	private ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	private MockHttpServletRequestBuilder putJson(String url, Object body) {
		return put(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
	}

	private static Map<String, Object> code(String code, String name, String criterion) {
		Map<String, Object> m = new HashMap<>(Map.of("code", code, "name", name));
		if (criterion != null) {
			m.put("criterion", criterion);
		}
		return m;
	}

	private static List<String> field(JsonNode list, String name) {
		List<String> values = new ArrayList<>();
		list.forEach(n -> values.add(n.get(name).asString()));
		return values;
	}

	// ─── Mã lỗi ──────────────────────────────────────────────────────────────

	@Test
	@DisplayName("BR-21: có sẵn 24 mã lỗi hệ thống theo thứ tự (22 mã V5 + GR, OTHER ở V9), kèm nhóm tiêu chí; mã riêng xếp sau và chỉ chủ thấy")
	void systemAndOwnErrorCodes() throws Exception {
		JsonNode before = data(call(get("/api/teacher/error-codes"), teacher).andExpect(apiStatus(200)));
		List<JsonNode> system = new ArrayList<>();
		before.forEach(n -> {
			if (n.get("system").asBoolean()) {
				system.add(n);
			}
		});
		assertThat(system).hasSize(24);
		assertThat(system.getLast().get("code").asString()).isEqualTo("OTHER");
		assertThat(system.getFirst().get("code").asString()).isEqualTo("SP");
		assertThat(system.getFirst().get("criterion").asString()).isEqualTo("LR");
		assertThat(field(before, "code")).contains("SING/PL", "^", "?", "IRR", "GOOD", "GR");
		JsonNode good = system.stream().filter(n -> n.get("code").asString().equals("GOOD")).findFirst().orElseThrow();
		assertThat(good.get("positive").asBoolean()).isTrue();
		assertThat(good.get("criterion").isNull()).isTrue();
		JsonNode irr = system.stream().filter(n -> n.get("code").asString().equals("IRR")).findFirst().orElseThrow();
		assertThat(irr.get("criterion").asString()).isEqualTo("TASK");

		call(postJson("/api/teacher/error-codes", code(" para2 ", "Đoạn quá dài", "CC")), teacher)
				.andExpect(apiStatus(201))
				.andExpect(jsonPath("$.data.code").value("PARA2"))
				.andExpect(jsonPath("$.data.system").value(false))
				.andExpect(jsonPath("$.data.positive").value(false));

		JsonNode after = data(call(get("/api/teacher/error-codes"), teacher));
		assertThat(field(after, "code").getLast()).isEqualTo("PARA2");
		assertThat(field(data(call(get("/api/teacher/error-codes"), otherTeacher)), "code")).doesNotContain("PARA2");
	}

	@Test
	@DisplayName("BR-22: mã riêng không được trùng mã hệ thống / mã của mình (không phân biệt hoa thường); giáo viên khác dùng lại được")
	void duplicateErrorCodes() throws Exception {
		call(postJson("/api/teacher/error-codes", code("sp", "Chính tả 2", null)), teacher)
				.andExpect(apiStatus(409)).andExpect(jsonPath("$.errorCode").value("DUPLICATE"));
		call(postJson("/api/teacher/error-codes", code("TONE", "Giọng văn", "TASK")), teacher).andExpect(apiStatus(201));
		call(postJson("/api/teacher/error-codes", code("tone", "Giọng văn", "TASK")), teacher)
				.andExpect(apiStatus(409));
		call(postJson("/api/teacher/error-codes", code("TONE", "Giọng văn", "TASK")), otherTeacher)
				.andExpect(apiStatus(201));
	}

	@Test
	@DisplayName("BR-22: sửa / xoá mã riêng; mã hệ thống → 403; mã của giáo viên khác → 404")
	void editAndDeleteErrorCodes() throws Exception {
		long id = data(call(postJson("/api/teacher/error-codes", code("REG", "Văn phong", "LR")), teacher)).get("id").asLong();
		// Giữ nguyên mã khi sửa không bị coi là trùng với chính nó.
		call(putJson("/api/teacher/error-codes/" + id, code("REG", "Văn phong không trang trọng", "LR")), teacher)
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.name").value("Văn phong không trang trọng"));
		call(putJson("/api/teacher/error-codes/" + id, code("REG", "x", null)), otherTeacher).andExpect(apiStatus(404));
		call(delete("/api/teacher/error-codes/" + id), otherTeacher).andExpect(apiStatus(404));

		long systemId = data(call(get("/api/teacher/error-codes"), teacher)).get(0).get("id").asLong();
		call(putJson("/api/teacher/error-codes/" + systemId, code("SP", "Đổi tên", "LR")), teacher)
				.andExpect(apiStatus(403));
		call(delete("/api/teacher/error-codes/" + systemId), teacher).andExpect(apiStatus(403));

		call(delete("/api/teacher/error-codes/" + id), teacher).andExpect(apiStatus(200));
		assertThat(field(data(call(get("/api/teacher/error-codes"), teacher)), "code")).doesNotContain("REG");
	}

	@Test
	@DisplayName("Validate mã lỗi: khoảng trắng / quá dài / thiếu tên / nhóm tiêu chí sai → 400")
	void validateErrorCode() throws Exception {
		call(postJson("/api/teacher/error-codes", code("A B", "Có khoảng trắng", null)), teacher)
				.andExpect(apiStatus(400));
		call(postJson("/api/teacher/error-codes", code("X".repeat(21), "Quá dài", null)), teacher)
				.andExpect(apiStatus(400));
		call(postJson("/api/teacher/error-codes", Map.of("code", "OK")), teacher).andExpect(apiStatus(400));
		call(postJson("/api/teacher/error-codes", code("OK2", "Tên", "TR")), teacher).andExpect(apiStatus(400));
	}

	// ─── Ngân hàng nhận xét ──────────────────────────────────────────────────

	private long addComment(String token, String criterion, String content) throws Exception {
		Map<String, Object> body = new HashMap<>(Map.of("content", content));
		if (criterion != null) {
			body.put("criterion", criterion);
		}
		return data(call(postJson("/api/teacher/comment-bank", body), token).andExpect(apiStatus(201)))
				.get("id").asLong();
	}

	@Test
	@DisplayName("BR-22: có 20 nhận xét mẫu (5 mỗi tiêu chí) xếp sau nhận xét của tôi; lọc theo tiêu chí kèm nhận xét dùng chung")
	void listAndFilter() throws Exception {
		long cc = addComment(teacher, "CC", "Đoạn 2 thiếu câu chủ đề");
		long shared = addComment(teacher, null, "Bài làm có tiến bộ so với lần trước");
		addComment(teacher, "LR", "Dùng từ học thuật tốt");

		JsonNode all = data(call(get("/api/teacher/comment-bank"), teacher).andExpect(apiStatus(200)));
		long systemCount = 0;
		for (JsonNode n : all) {
			if (n.get("system").asBoolean()) {
				systemCount++;
			}
		}
		assertThat(systemCount).isEqualTo(20);
		assertThat(all.get(0).get("system").asBoolean()).isFalse();
		assertThat(all.get(all.size() - 1).get("system").asBoolean()).isTrue();

		JsonNode onlyCc = data(call(get("/api/teacher/comment-bank").param("criterion", "CC"), teacher));
		List<Long> ids = new ArrayList<>();
		onlyCc.forEach(n -> {
			ids.add(n.get("id").asLong());
			assertThat(n.get("criterion").isNull() || n.get("criterion").asString().equals("CC")).isTrue();
		});
		assertThat(ids).contains(cc, shared);
		assertThat(onlyCc).hasSize(2 + 5);

		// Giáo viên khác chỉ thấy nhận xét mẫu.
		JsonNode other = data(call(get("/api/teacher/comment-bank"), otherTeacher));
		other.forEach(n -> assertThat(n.get("system").asBoolean()).isTrue());
	}

	@Test
	@DisplayName("Tìm theo từ khoá: không phân biệt hoa thường, ký tự % và _ được hiểu theo nghĩa đen")
	void keyword() throws Exception {
		addComment(teacher, null, "Đạt 100% yêu cầu về số từ");
		addComment(teacher, null, "Đạt 1000 yêu cầu");
		JsonNode found = data(call(get("/api/teacher/comment-bank").param("keyword", "100%"), teacher));
		assertThat(field(found, "content")).containsExactly("Đạt 100% yêu cầu về số từ");
		assertThat(data(call(get("/api/teacher/comment-bank").param("keyword", "ĐẠT 1000"), teacher))).hasSize(1);
	}

	@Test
	@DisplayName("BR-22: đánh dấu đã dùng → tăng số lần, xếp lên đầu; nhận xét mẫu của hệ thống không bị đếm")
	void markUsed() throws Exception {
		long first = addComment(teacher, "TASK", "Nhận xét A");
		long second = addComment(teacher, "TASK", "Nhận xét B");
		call(post("/api/teacher/comment-bank/" + second + "/use"), teacher)
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.useCount").value(1))
				.andExpect(jsonPath("$.data.lastUsedAt").value(clock.instant().toString()));
		JsonNode list = data(call(get("/api/teacher/comment-bank").param("criterion", "TASK"), teacher));
		assertThat(list.get(0).get("id").asLong()).isEqualTo(second);
		assertThat(list.get(1).get("id").asLong()).isEqualTo(first);

		long systemId = list.get(list.size() - 1).get("id").asLong();
		call(post("/api/teacher/comment-bank/" + systemId + "/use"), teacher)
				.andExpect(apiStatus(200)).andExpect(jsonPath("$.data.useCount").value(0));
		call(post("/api/teacher/comment-bank/" + second + "/use"), otherTeacher).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("BR-22: sửa / xoá nhận xét của tôi; mẫu hệ thống → 403; của giáo viên khác → 404; nội dung rỗng → 400")
	void editAndDeleteComments() throws Exception {
		long id = addComment(teacher, "GRA", "Chú ý mạo từ");
		call(putJson("/api/teacher/comment-bank/" + id, Map.of("criterion", "GRA", "content", "  Chú ý mạo từ 'the'  ")), teacher)
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.content").value("Chú ý mạo từ 'the'"));
		call(putJson("/api/teacher/comment-bank/" + id, Map.of("content", "x")), otherTeacher).andExpect(apiStatus(404));
		call(putJson("/api/teacher/comment-bank/" + id, Map.of("content", " ")), teacher).andExpect(apiStatus(400));

		JsonNode list = data(call(get("/api/teacher/comment-bank"), teacher));
		long systemId = list.get(list.size() - 1).get("id").asLong();
		call(putJson("/api/teacher/comment-bank/" + systemId, Map.of("content", "Sửa mẫu")), teacher)
				.andExpect(apiStatus(403));
		call(delete("/api/teacher/comment-bank/" + systemId), teacher).andExpect(apiStatus(403));

		call(delete("/api/teacher/comment-bank/" + id), otherTeacher).andExpect(apiStatus(404));
		call(delete("/api/teacher/comment-bank/" + id), teacher).andExpect(apiStatus(200));
		assertThat(field(data(call(get("/api/teacher/comment-bank"), teacher)), "content")).doesNotContain("Chú ý mạo từ 'the'");
	}

	@Test
	@DisplayName("Phân quyền: sinh viên gọi API mã lỗi / ngân hàng nhận xét → 403")
	void studentForbidden() throws Exception {
		String student = accessToken(newUser(Role.STUDENT));
		call(get("/api/teacher/error-codes"), student).andExpect(apiStatus(403));
		call(get("/api/teacher/comment-bank"), student).andExpect(apiStatus(403));
	}
}
