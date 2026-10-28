package com.ltmtu.ielts.assignment;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.math.BigDecimal;
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

/** UC-20 / FR-25: CRUD ngân hàng đề riêng của từng giáo viên. */
class QuestionBankIntegrationTest extends IntegrationTest {

	private String token;

	@BeforeEach
	void loginTeacher() throws Exception {
		token = accessToken(newUser(Role.TEACHER));
	}

	private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(token));
	}

	private ResultActions create(Object body) throws Exception {
		return mvc.perform(as(postJson("/api/teacher/question-bank", body)));
	}

	private JsonNode createOk(Object body) throws Exception {
		return json.readTree(create(body).andExpect(apiStatus(200)).andReturn().getResponse().getContentAsString())
				.get("data");
	}

	private ResultActions replace(long id, Object body) throws Exception {
		return mvc.perform(as(put("/api/teacher/question-bank/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(body))));
	}

	private static Map<String, Object> request(String title, String taskType, List<String> tags) {
		Map<String, Object> checklist = Map.of("content", "Có luận điểm rõ ràng", "kind", "CHECK");
		Map<String, Object> limit = Map.of("content", "Không trả lời đủ các phần", "kind", "LIMIT", "maxBand",
				new BigDecimal("5"));
		Map<String, Object> criterion = new HashMap<>();
		criterion.put("code", "WRITING_TASK1".equals(taskType) ? "TA" : "TR");
		criterion.put("name", "Task Response");
		criterion.put("maxScore", 9);
		criterion.put("step", 1);
		criterion.put("checklist", List.of(checklist, limit));
		Map<String, Object> body = new HashMap<>();
		body.put("title", title);
		body.put("taskType", taskType);
		body.put("prompt", "<p>Viết bài về giáo dục.</p><script>alert('x')</script>");
		body.put("minWords", 250);
		body.put("suggestedMinutes", 40);
		body.put("tags", tags);
		body.put("criteria", List.of(criterion));
		return body;
	}

	@Test
	@DisplayName("UC-20: tạo đề lưu nguyên rubric, lọc HTML và chuẩn hoá tag")
	void createAndRead() throws Exception {
		JsonNode item = createOk(request("Opinion essay", "WRITING_TASK2", List.of("Opinion", " Academic ", "opinion")));

		long id = item.get("id").asLong();
		mvc.perform(as(get("/api/teacher/question-bank/" + id)))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.prompt", not(containsString("<script"))))
				.andExpect(jsonPath("$.data.tags", contains("academic", "opinion")))
				.andExpect(jsonPath("$.data.criteria", hasSize(1)))
				.andExpect(jsonPath("$.data.criteria[0].checklist", hasSize(2)))
				.andExpect(jsonPath("$.data.criteria[0].checklist[1].maxBand").value(5));
	}

	@Test
	@DisplayName("UC-20: tìm theo từ khoá, loại đề và tag chỉ trả đề của giáo viên hiện tại")
	void searchAndOwnership() throws Exception {
		createOk(request("Biểu đồ quy trình", "WRITING_TASK1", List.of("process", "academic")));
		createOk(request("Advantages discussion", "WRITING_TASK2", List.of("discussion")));

		mvc.perform(as(get("/api/teacher/question-bank").param("keyword", "quy trình")
				.param("taskType", "WRITING_TASK1").param("tag", "PROCESS")))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.totalRecord").value(1))
				.andExpect(jsonPath("$.data[0].title").value("Biểu đồ quy trình"));

		String otherToken = accessToken(newUser(Role.TEACHER));
		mvc.perform(get("/api/teacher/question-bank").header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
				.andExpect(apiStatus(200)).andExpect(jsonPath("$.totalRecord").value(0));
	}

	@Test
	@DisplayName("UC-20: giáo viên khác không được xem, sửa hoặc xoá đề")
	void otherTeacherCannotMutate() throws Exception {
		Map<String, Object> body = request("Đề riêng", "WRITING_TASK2", List.of("private"));
		long id = createOk(body).get("id").asLong();
		token = accessToken(newUser(Role.TEACHER));

		mvc.perform(as(get("/api/teacher/question-bank/" + id))).andExpect(apiStatus(404));
		replace(id, body).andExpect(apiStatus(404));
		mvc.perform(as(delete("/api/teacher/question-bank/" + id))).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("UC-20: thay toàn bộ đề rồi xoá khỏi ngân hàng")
	void replaceAndDelete() throws Exception {
		long id = createOk(request("Đề cũ", "WRITING_TASK2", List.of("old"))).get("id").asLong();
		Map<String, Object> updated = request("Đề mới", "WRITING_TASK1", List.of("process", "new"));

		replace(id, updated).andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.title").value("Đề mới"))
				.andExpect(jsonPath("$.data.taskType").value("WRITING_TASK1"))
				.andExpect(jsonPath("$.data.tags", contains("new", "process")));

		mvc.perform(as(delete("/api/teacher/question-bank/" + id))).andExpect(apiStatus(200));
		mvc.perform(as(get("/api/teacher/question-bank/" + id))).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("UC-20: từ chối ảnh đề không thuộc uploads hoặc HTTP(S)")
	void rejectInvalidImage() throws Exception {
		Map<String, Object> body = request("Đề lỗi", "WRITING_TASK2", List.of());
		body.put("imageUrl", "file:///etc/passwd");
		create(body).andExpect(apiStatus(400))
				.andExpect(jsonPath("$.errorMessage", containsString("đường dẫn ảnh không hợp lệ")));
	}

	@Test
	@DisplayName("UC-20: chỉ giáo viên được dùng API ngân hàng đề")
	void teacherOnly() throws Exception {
		String studentToken = accessToken(newUser(Role.STUDENT));
		mvc.perform(get("/api/teacher/question-bank").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
				.andExpect(apiStatus(403));
	}
}
