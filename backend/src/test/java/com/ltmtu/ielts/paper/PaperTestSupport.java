package com.ltmtu.ielts.paper;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static com.ltmtu.ielts.support.ApiStatus.apiStatusOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.SnapshotRequest;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.UpdateMessage;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Dữ liệu chung cho test bài PAPER: tạo bài paper (2 giai đoạn, có khung mục), phát hành, lập lớp. */
abstract class PaperTestSupport extends IntegrationTest {

	@Autowired
	protected PaperDocumentService documentService;

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

	// ─── Tài liệu nhóm (Yjs) ───

	/** Đoạn văn trong JSON trình soạn; chữ rỗng → đoạn trống. */
	protected static Map<String, Object> paragraph(Object... inline) {
		List<Object> content = new java.util.ArrayList<>();
		for (Object x : inline) {
			content.add(x instanceof String t ? Map.of("type", "text", "text", t) : x);
		}
		return content.isEmpty() ? Map.of("type", "paragraph") : Map.of("type", "paragraph", "content", content);
	}

	protected static Map<String, Object> citation(String refIds) {
		return Map.of("type", "citation", "attrs", Map.of("refIds", refIds));
	}

	/** JSON tài liệu: mỗi mục (sectionKey → các khối), tiêu đề = sectionKey. */
	protected static Map<String, Object> docJson(Map<String, List<Map<String, Object>>> sections) {
		List<Object> content = new java.util.ArrayList<>();
		sections.forEach((key, blocks) -> content.add(Map.of("type", "paperSection",
				"attrs", Map.of("sectionKey", key, "title", key), "content", blocks)));
		return Map.of("type", "doc", "content", content);
	}

	protected static Map<String, List<Map<String, Object>>> sectionsOf(Object... keyAndText) {
		Map<String, List<Map<String, Object>>> out = new LinkedHashMap<>();
		for (int i = 0; i < keyAndText.length; i += 2) {
			out.put((String) keyAndText[i], List.of(paragraph((String) keyAndText[i + 1])));
		}
		return out;
	}

	/** Mở tài liệu nhóm của {@code student} cho phần này; trả id tài liệu. */
	protected long openDocument(User student, long partId) throws Exception {
		return data(call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.get("/api/student/parts/" + partId + "/document"), accessToken(student))).get("documentId").asLong();
	}

	/** Một update Yjs giả (server không đọc nội dung) — như thành viên vừa gõ. */
	protected void typeSomething(User student, long documentId) {
		documentService.applyUpdate(new AuthPrincipal(student.getId(), student.getUsername(), Role.STUDENT, 0L),
				documentId, new UpdateMessage(b64("u" + System.nanoTime()), "test", 5, 0));
	}

	/** Snapshot gộp tới thay đổi mới nhất, kèm JSON + chữ thuần như client gửi. */
	protected void snapshot(User student, long documentId, Map<String, Object> doc, String text) {
		long lastSeq = documentService.state(new AuthPrincipal(student.getId(), student.getUsername(), Role.STUDENT, 0L),
				documentId).lastSeq();
		documentService.saveSnapshot(student.getId(), documentId,
				new SnapshotRequest(b64("snapshot"), lastSeq, json.writeValueAsString(doc), text));
	}

	/** Gõ + lưu snapshot: tài liệu của nhóm có nội dung {@code doc}. */
	protected long writeDocument(User student, long partId, Map<String, Object> doc, String text) throws Exception {
		long id = openDocument(student, partId);
		typeSomething(student, id);
		snapshot(student, id, doc, text);
		return id;
	}

	private static String b64(String s) {
		return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
	}
}
