package com.ltmtu.ielts.paper;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.UpdateMessage;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/**
 * Nộp bài PAPER từ tài liệu nhóm (B25a, BR-28, BR-29, BR-30): server lấy snapshot mới nhất, kiểm tra mục bắt buộc, lưu
 * JSON + tài liệu được trích; client gửi chữ gì cũng bị bỏ qua.
 */
class PaperSubmitIntegrationTest extends PaperTestSupport {

	private String teacher;
	private User a;
	private User b;
	private long assignmentId;
	private long partId;
	private long groupId;

	@BeforeEach
	void setUp() throws Exception {
		documentService.clear();
		teacher = accessToken(newUser(Role.TEACHER));
		a = newUser(Role.STUDENT);
		b = newUser(Role.STUDENT);
		// Khung: Introduction (bắt buộc), Method (bắt buộc), Notes (không bắt buộc)
		assignmentId = publishedPaper(teacher, Map.of("parts", List.of(stage("Đề cương", List.of(
				section("Introduction", true), section("Method", true), section("Notes", false))))), a, b);
		partId = data(call(get("/api/teacher/assignments/" + assignmentId), teacher)).get("parts").get(0).get("id")
				.asLong();
		groupId = data(call(postJson("/api/teacher/assignments/" + assignmentId + "/groups",
				Map.of("name", "Nhóm AB", "studentIds", List.of(a.getId(), b.getId()))), teacher)).get("groups").get(0)
				.get("id").asLong();
	}

	private ResultActions submit(User student, Object content) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("content", content);
		body.put("expectedVersion", 0);
		return call(postJson("/api/student/parts/" + partId + "/submit", body), accessToken(student));
	}

	private long addReference(User student, String title) throws Exception {
		return data(call(postJson("/api/papers/groups/" + groupId + "/references",
				Map.of("csl", Map.of("title", title, "issued", Map.of("date-parts", List.of(List.of(2020)))))),
				accessToken(student))).get("id").asLong();
	}

	@Test
	@DisplayName("BR-29: mục bắt buộc còn trống → 422 REQUIRED_SECTIONS_EMPTY liệt kê tên mục; mục không bắt buộc trống vẫn nộp được")
	void requiredSections() throws Exception {
		long doc = writeDocument(a, partId, docJson(sectionsOf("introduction", "Our motivation is clear.", "method", "",
				"notes", "")), "Introduction Our motivation is clear.");
		submit(b, null).andExpect(apiStatus(422)).andExpect(jsonPath("$.errorCode").value("REQUIRED_SECTIONS_EMPTY"))
				.andExpect(jsonPath("$.errorMessage").value("Mục bắt buộc còn trống: Method"));

		typeSomething(a, doc);
		snapshot(a, doc, docJson(sectionsOf("introduction", "Our motivation is clear.", "method", "We train a model.",
				"notes", "")), "Introduction ... Method ...");
		submit(b, null).andExpect(apiStatus(200)).andExpect(jsonPath("$.data.wordCount").value(8));
	}

	@Test
	@DisplayName("Snapshot cũ hơn thay đổi cuối → 409 SNAPSHOT_STALE; lưu snapshot mới rồi nộp được; tài liệu chưa ai viết → 400")
	void staleAndEmpty() throws Exception {
		submit(a, "chữ client gửi bị bỏ qua").andExpect(apiStatus(400));

		Map<String, Object> full = docJson(sectionsOf("introduction", "Intro text here.", "method", "Method text."));
		long doc = writeDocument(a, partId, full, "Intro Method");
		typeSomething(b, doc);
		submit(a, null).andExpect(apiStatus(409)).andExpect(jsonPath("$.errorCode").value("SNAPSHOT_STALE"));
		snapshot(b, doc, full, "Intro Method");
		submit(a, null).andExpect(apiStatus(200));
	}

	@Test
	@DisplayName("BR-28 / BR-30: bản nộp giữ JSON + chữ thuần + tài liệu được trích (theo thứ tự, bỏ mục đã xoá), màn chấm cũng nhận; sau khi nộp tài liệu chỉ đọc")
	void versionKeepsContent() throws Exception {
		long r1 = addReference(a, "First paper");
		long r2 = addReference(b, "Second paper");
		long removed = addReference(a, "Removed later");
		Map<String, List<Map<String, Object>>> sections = new LinkedHashMap<>();
		sections.put("introduction", List.of(paragraph("As shown in", citation(r2 + "," + removed), " and",
				citation(String.valueOf(r1)))));
		sections.put("method", List.of(paragraph("We do things.")));
		long doc = writeDocument(a, partId, docJson(sections), "Introduction As shown in and\n\nMethod We do things.");
		call(delete("/api/papers/groups/" + groupId + "/references/" + removed), accessToken(a)).andExpect(apiStatus(200));

		submit(b, "client gửi gì cũng không dùng").andExpect(apiStatus(200));

		JsonNode v = data(call(get("/api/student/parts/" + partId + "/versions/1"), accessToken(a)));
		assertThat(v.get("content").asString()).isEqualTo("Introduction As shown in and\n\nMethod We do things.");
		assertThat(v.get("wordCount").asInt()).isEqualTo(7);
		assertThat(v.get("contentJson").get("content").get(0).get("attrs").get("sectionKey").asString())
				.isEqualTo("introduction");
		JsonNode refs = v.get("references");
		assertThat(refs).hasSize(2);
		assertThat(refs.get(0).get("id").asLong()).isEqualTo(r2);
		assertThat(refs.get(0).get("csl").get("title").asString()).isEqualTo("Second paper");
		assertThat(refs.get(1).get("id").asLong()).isEqualTo(r1);

		// B25c-2: màn chấm của giáo viên nhận cùng JSON + tài liệu được trích để hiện bài có định dạng
		long submissionId = data(call(get("/api/teacher/submissions").param("assignmentId",
				String.valueOf(assignmentId)), teacher)).get(0).get("submissionId").asLong();
		JsonNode screen = data(call(get("/api/teacher/submissions/" + submissionId), teacher));
		assertThat(screen.get("assignment").get("citationStyle").asString()).isNotBlank();
		JsonNode tv = screen.get("version");
		assertThat(tv.get("contentJson")).isEqualTo(v.get("contentJson"));
		assertThat(tv.get("references")).isEqualTo(refs);

		assertThat(data(call(get("/api/student/parts/" + partId + "/document"), accessToken(a))).get("editable")
				.asBoolean()).isFalse();
		AuthPrincipal pa = new AuthPrincipal(a.getId(), a.getUsername(), Role.STUDENT, 0L);
		assertThatThrownBy(() -> documentService.applyUpdate(pa, doc, new UpdateMessage("dGVzdA==", "x", 1, 0)))
				.isInstanceOf(ApiException.class).hasMessageContaining("chỉ đọc");
	}

	@Test
	@DisplayName("Bài luận không đổi: thiếu nội dung → 400; có nội dung thì nộp như cũ, không có contentJson")
	void essayUnchanged() throws Exception {
		User s = newUser(Role.STUDENT);
		long essay = data(call(postJson("/api/teacher/assignments", Map.of("title", "Essay", "dueAt",
				clock.instant().plusSeconds(86_400).toString(), "parts", List.of(Map.of("title", "Task 2", "taskType",
						"WRITING_TASK2", "prompt", "<p>Đề</p>", "criteria",
						List.of(Map.of("code", "TR", "name", "Task Response", "checklist", List.of())))))), teacher))
				.get("id").asLong();
		call(postJson("/api/teacher/assignments/" + essay + "/publish", Map.of("studentIds", List.of(s.getId()))), teacher)
				.andExpect(apiStatus(200));
		long essayPart = data(call(get("/api/teacher/assignments/" + essay), teacher)).get("parts").get(0).get("id")
				.asLong();
		String token = accessToken(s);
		call(postJson("/api/student/parts/" + essayPart + "/submit", Map.of("content", "  ", "expectedVersion", 0)), token)
				.andExpect(apiStatus(400));
		call(postJson("/api/student/parts/" + essayPart + "/submit", Map.of("content", "My essay text.",
				"expectedVersion", 0)), token).andExpect(apiStatus(200));
		JsonNode v = data(call(get("/api/student/parts/" + essayPart + "/versions/1"), token));
		assertThat(v.get("contentJson").isNull()).isTrue();
		assertThat(v.get("references").isNull()).isTrue();
	}
}
