package com.ltmtu.ielts.paper;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static com.ltmtu.ielts.support.ApiStatus.apiStatusOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Dữ liệu chung cho test bài PAPER: tạo bài paper (2 giai đoạn, có khung mục), phát hành, lập lớp. */
abstract class PaperTestSupport extends IntegrationTest {

	protected Map<String, Object> paperBody(Map<String, Object> extra) {
		Map<String, Object> body = new HashMap<>();
		body.put("title", "Paper nhóm");
		body.put("kind", "PAPER");
		body.put("dueAt", clock.instant().plus(Duration.ofDays(14)).toString());
		body.put("parts", List.of(stage("Đề cương", List.of(section("Introduction", true), section("Method", true))),
				stage("Bản cuối", List.of(section("Abstract", true), section("Related Work", false)))));
		body.putAll(extra);
		return body;
	}

	protected static Map<String, Object> stage(String title, List<Map<String, Object>> sections) {
		Map<String, Object> p = new HashMap<>();
		p.put("title", title);
		p.put("taskType", "CUSTOM");
		p.put("prompt", "<p>Viết theo khung mục.</p>");
		p.put("sections", sections);
		p.put("criteria", List.of(Map.of("name", "Phương pháp", "checklist", List.of())));
		return p;
	}

	protected static Map<String, Object> section(String title, boolean required) {
		return Map.of("title", title, "required", required, "maxWords", 800);
	}

	protected JsonNode createPaper(String teacher, Map<String, Object> extra) throws Exception {
		return data(call(postJson("/api/teacher/assignments", paperBody(extra)), teacher));
	}

	/** Tạo bài paper và phát hành cho các sinh viên; trả về id bài. */
	protected long publishedPaper(String teacher, Map<String, Object> extra, User... students) throws Exception {
		long id = createPaper(teacher, extra).get("id").asLong();
		call(postJson("/api/teacher/assignments/" + id + "/publish",
				Map.of("studentIds", java.util.Arrays.stream(students).map(User::getId).toList())), teacher)
				.andExpect(apiStatus(200));
		return id;
	}

	protected long createClass(String teacher, User... members) throws Exception {
		String code = "PP" + members[0].getStudentCode().substring(2).toUpperCase();
		long id = data(call(postJson("/api/teacher/classes", Map.of("code", code, "name", "Lớp paper")), teacher))
				.get("id").asLong();
		call(postJson("/api/teacher/classes/" + id + "/members", Map.of("studentCodes",
				java.util.Arrays.stream(members).map(User::getStudentCode).toList())), teacher).andExpect(apiStatus(200));
		return id;
	}

	protected JsonNode data(ResultActions result) throws Exception {
		JsonNode body = json.readTree(result.andReturn().getResponse().getContentAsString());
		assertThat(apiStatusOf(result.andReturn())).as(body.toString()).isEqualTo(200);
		return body.get("data");
	}

	protected ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}
}
