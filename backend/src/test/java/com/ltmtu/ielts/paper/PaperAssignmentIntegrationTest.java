package com.ltmtu.ielts.paper;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Bài tập loại PAPER (docs/12, B21a): trường riêng, khung mục, thang điểm 10, phát hành, nhân bản. */
class PaperAssignmentIntegrationTest extends PaperTestSupport {

	private String teacher;

	@BeforeEach
	void setUp() throws Exception {
		teacher = accessToken(newUser(Role.TEACHER));
	}

	@Test
	@DisplayName("Tạo bài PAPER: mặc định IEEE, nhóm 4 người; khung mục có mã tự sinh; tiêu chí mặc định thang 10, bước 0,25")
	void createPaper() throws Exception {
		JsonNode a = createPaper(teacher, Map.of());
		assertThat(a.get("kind").asString()).isEqualTo("PAPER");
		assertThat(a.get("citationStyle").asString()).isEqualTo("IEEE");
		assertThat(a.get("maxGroupSize").asInt()).isEqualTo(4);
		JsonNode sections = a.get("parts").get(1).get("sections");
		assertThat(sections.valueStream().map(s -> s.get("sectionKey").asString()).toList())
				.containsExactly("abstract", "related-work");
		assertThat(sections.get(1).get("required").asBoolean()).isFalse();
		assertThat(sections.get(1).get("maxWords").asInt()).isEqualTo(800);
		JsonNode criterion = a.get("parts").get(0).get("criteria").get(0);
		assertThat(criterion.get("maxScore").decimalValue()).isEqualByComparingTo("10");
		assertThat(criterion.get("step").decimalValue()).isEqualByComparingTo("0.25");

		JsonNode essay = data(call(postJson("/api/teacher/assignments", Map.of("title", "Bài luận", "parts",
				List.of(Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>Đề</p>")))), teacher));
		assertThat(essay.get("kind").asString()).as("mặc định vẫn là bài luận").isEqualTo("ESSAY");
		assertThat(essay.get("citationStyle").isNull()).isTrue();
	}

	@Test
	@DisplayName("Kiểm tra khi lưu: bài luận không có khung mục / điểm > 9; paper không dùng chế độ thi; hạn chốt nhóm sau hạn nộp → 400")
	void validation() throws Exception {
		Map<String, Object> essayWithSections = paperBody(Map.of("kind", "ESSAY"));
		call(postJson("/api/teacher/assignments", essayWithSections), teacher).andExpect(apiStatus(400))
				.andExpect(jsonPath("$.errorMessage").value("Phần 1: chỉ bài paper mới có khung mục"));

		Map<String, Object> essayTen = Map.of("title", "Bài luận", "parts", List.of(Map.of("title", "Task 2", "taskType",
				"WRITING_TASK2", "prompt", "<p>Đề</p>", "criteria", List.of(Map.of("name", "TR", "maxScore", 10)))));
		call(postJson("/api/teacher/assignments", essayTen), teacher).andExpect(apiStatus(400));

		Map<String, Object> examStage = new java.util.HashMap<>(stage("Thi", List.of(section("Abstract", true))));
		examStage.put("examMode", true);
		examStage.put("examDurationMinutes", 30);
		call(postJson("/api/teacher/assignments", paperBody(Map.of("parts", List.of(examStage)))), teacher)
				.andExpect(apiStatus(400)).andExpect(jsonPath("$.errorMessage").value("Phần 1: bài paper không dùng chế độ thi"));

		call(postJson("/api/teacher/assignments", paperBody(Map.of("groupLockAt",
				clock.instant().plus(Duration.ofDays(20)).toString()))), teacher).andExpect(apiStatus(400))
				.andExpect(jsonPath("$.errorMessage").value("Hạn chốt nhóm phải trước hạn nộp"));

		Map<String, Object> dupKeys = stage("Lặp", List.of(Map.of("title", "A", "sectionKey", "intro"),
				Map.of("title", "B", "sectionKey", "Intro")));
		call(postJson("/api/teacher/assignments", paperBody(Map.of("parts", List.of(dupKeys)))), teacher)
				.andExpect(apiStatus(400));
	}

	@Test
	@DisplayName("Phát hành paper: giai đoạn chưa có khung mục → 400 NOT_READY; nhân bản giữ loại, kiểu trích dẫn, cỡ nhóm, khung mục")
	void publishAndDuplicate() throws Exception {
		Map<String, Object> noSections = stage("Không khung", List.of());
		long bad = createPaper(teacher, Map.of("parts", List.of(noSections))).get("id").asLong();
		User s = newUser(Role.STUDENT);
		call(postJson("/api/teacher/assignments/" + bad + "/publish", Map.of("studentIds", List.of(s.getId()))), teacher)
				.andExpect(apiStatus(400)).andExpect(jsonPath("$.errorCode").value("NOT_READY"))
				.andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.containsString("chưa có khung mục")));

		long id = createPaper(teacher, Map.of("citationStyle", "APA", "maxGroupSize", 3)).get("id").asLong();
		JsonNode copy = data(call(post("/api/teacher/assignments/" + id + "/duplicate"), teacher));
		assertThat(copy.get("kind").asString()).isEqualTo("PAPER");
		assertThat(copy.get("citationStyle").asString()).isEqualTo("APA");
		assertThat(copy.get("maxGroupSize").asInt()).isEqualTo(3);
		assertThat(copy.get("parts").get(0).get("sections")).hasSize(2);
	}

	@Test
	@DisplayName("Sinh viên xem chi tiết bài paper: có loại bài, kiểu trích dẫn, khung mục từng giai đoạn")
	void studentDetail() throws Exception {
		User s = newUser(Role.STUDENT);
		long id = publishedPaper(teacher, Map.of(), s);
		JsonNode d = data(call(get("/api/student/assignments/" + id), accessToken(s)));
		assertThat(d.get("kind").asString()).isEqualTo("PAPER");
		assertThat(d.get("citationStyle").asString()).isEqualTo("IEEE");
		assertThat(d.get("parts").get(0).get("sections").get(0).get("title").asString()).isEqualTo("Introduction");
	}

	@Test
	@DisplayName("Cài đặt sau phát hành: đổi cỡ nhóm không nhỏ hơn nhóm đông nhất; bài luận không có cài đặt nhóm")
	void groupSettings() throws Exception {
		User a = newUser(Role.STUDENT);
		User b = newUser(Role.STUDENT);
		long id = publishedPaper(teacher, Map.of(), a, b);
		call(postJson("/api/teacher/assignments/" + id + "/groups",
				Map.of("name", "Nhóm A", "studentIds", List.of(a.getId(), b.getId()))), teacher).andExpect(apiStatus(200));
		call(settings(id, Map.of("maxGroupSize", 1)), teacher).andExpect(apiStatus(400));
		JsonNode ok = data(call(settings(id, Map.of("maxGroupSize", 2)), teacher));
		assertThat(ok.get("maxGroupSize").asInt()).isEqualTo(2);

		long essay = data(call(postJson("/api/teacher/assignments", Map.of("title", "Bài luận")), teacher)).get("id")
				.asLong();
		call(settings(essay, Map.of("maxGroupSize", 3)), teacher).andExpect(apiStatus(400));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder settings(long id,
			Map<String, Object> body) {
		return patch("/api/teacher/assignments/" + id + "/settings").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(body));
	}
}
