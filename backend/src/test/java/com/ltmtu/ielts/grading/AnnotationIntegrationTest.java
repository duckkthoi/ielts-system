package com.ltmtu.ielts.grading;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.ltmtu.ielts.user.Role;

import tools.jackson.databind.JsonNode;

/** Đánh dấu trên bài (V9): kiểu lỗi / lưu ý / dùng hay, đánh giá "đã sửa" khi so sánh 2 lần nộp, phân quyền. */
class AnnotationIntegrationTest extends GradingTestSupport {

	private static Map<String, Object> mark(String quote, String kind, String code, String comment) {
		int start = ESSAY.indexOf(quote);
		Map<String, Object> m = new HashMap<>(Map.of("startOffset", start, "endOffset", start + quote.length()));
		if (kind != null) {
			m.put("kind", kind);
		}
		if (code != null) {
			m.put("errorCode", code);
		}
		if (comment != null) {
			m.put("comment", comment);
		}
		return m;
	}

	private ResultActions resolve(long submissionId, long commentId, String resolution, String token) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("resolution", resolution);
		return call(patch("/api/teacher/submissions/" + submissionId + "/inline-comments/" + commentId)
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), token);
	}

	/** Lần 1 có 1 lỗi + 1 lưu ý, giáo viên yêu cầu sửa, sinh viên nộp lần 2. Trả về [id lỗi, id lưu ý] của lần 1. */
	private long[] twoVersions(Fixture f) throws Exception {
		JsonNode g = data(saveGrade(f.submissionId(), grade(1, scores(f, 6, 6, 6, 6), List.of(),
				List.of(mark("people is", "ERROR", "SV", null), mark("Governments", "HIGHLIGHT", null, null))))
				.andExpect(apiStatus(200)));
		call(postJson("/api/teacher/submissions/" + f.submissionId() + "/request-revision",
				Map.of("note", "Sửa lỗi đã đánh dấu", "dueAt", clock.instant().plus(Duration.ofDays(2)).toString())),
				teacher).andExpect(apiStatus(200));
		call(postJson("/api/student/parts/" + f.partId() + "/submit",
				Map.of("content", ESSAY.replace("people is", "people are"), "expectedVersion", 1)), studentToken)
				.andExpect(apiStatus(201));
		JsonNode list = g.get("inlineComments");
		return new long[] { list.get(0).get("id").asLong(), list.get(1).get("id").asLong() };
	}

	@Test
	@DisplayName("Kiểu đánh dấu: lưu ý / dùng hay được để trống; lỗi phải có nội dung; lưu ý không mang mã lỗi; mã khen tự thành GOOD")
	void kinds() throws Exception {
		Fixture f = submitted();
		JsonNode list = data(saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of(
				mark("people is", "HIGHLIGHT", null, null),
				mark("Governments", "GOOD", null, null),
				mark("public transport", null, "good", null),
				mark("pollution", null, "SP", null)))).andExpect(apiStatus(200))).get("inlineComments");
		assertThat(list).hasSize(4);
		assertThat(list.get(0).get("kind").asString()).isEqualTo("HIGHLIGHT");
		assertThat(list.get(1).get("kind").asString()).isEqualTo("GOOD");
		assertThat(list.get(2).get("kind").asString()).isEqualTo("GOOD"); // mã khen, không gửi kind
		assertThat(list.get(3).get("kind").asString()).isEqualTo("ERROR"); // mặc định
		assertThat(list.get(3).get("resolution").isNull()).isTrue();

		saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of(mark("people is", "ERROR", null, null))))
				.andExpect(apiStatus(400));
		saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of(mark("people is", "HIGHLIGHT", "SV", null))))
				.andExpect(apiStatus(400));
		saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of(mark("people is", "ERROR", "GR", null))))
				.andExpect(apiStatus(200)); // mã nhóm "Ngữ pháp" mới ở V9
	}

	@Test
	@DisplayName("So sánh: giáo viên đánh giá lỗi của lần nộp cũ (đã sửa / bỏ đánh giá); lần mới nhất → 409; lưu ý → 400")
	void resolution() throws Exception {
		Fixture f = submitted();
		long[] ids = twoVersions(f);
		resolve(f.submissionId(), ids[0], "FIXED", teacher).andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.resolution").value("FIXED"));
		call(get("/api/teacher/submissions/" + f.submissionId()).param("versionNo", "1"), teacher)
				.andExpect(jsonPath("$.data.grade.inlineComments[0].resolution").value("FIXED"));
		resolve(f.submissionId(), ids[0], null, teacher).andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.resolution").isEmpty());
		resolve(f.submissionId(), ids[1], "FIXED", teacher).andExpect(apiStatus(400));
		resolve(f.submissionId(), ids[0], "WRONG", teacher).andExpect(apiStatus(400));

		// Lỗi trên lần nộp mới nhất: chưa có bản sau để so sánh.
		JsonNode latest = data(saveGrade(f.submissionId(), grade(2, List.of(), List.of(),
				List.of(mark("pollution", "ERROR", "SP", null)))).andExpect(apiStatus(200)));
		resolve(f.submissionId(), latest.get("inlineComments").get(0).get("id").asLong(), "FIXED", teacher)
				.andExpect(apiStatus(409));
	}

	@Test
	@DisplayName("Phân quyền: giáo viên khác → 404; sinh viên → 403; nhận xét của bài nộp khác → 404")
	void resolutionOwnership() throws Exception {
		Fixture f = submitted();
		long[] ids = twoVersions(f);
		resolve(f.submissionId(), ids[0], "FIXED", accessToken(newUser(Role.TEACHER))).andExpect(apiStatus(404));
		resolve(f.submissionId(), ids[0], "FIXED", studentToken).andExpect(apiStatus(403));

		Fixture other = submitted();
		resolve(other.submissionId(), ids[0], "FIXED", teacher).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("BR-13: sinh viên thấy kiểu đánh dấu + đánh giá đã sửa của lần nộp đã trả; thống kê lỗi chỉ tính đánh dấu lỗi")
	void studentSees() throws Exception {
		Fixture f = submitted();
		long[] ids = twoVersions(f);
		resolve(f.submissionId(), ids[0], "FIXED", teacher).andExpect(apiStatus(200));
		JsonNode result = data(call(get("/api/student/parts/" + f.partId() + "/versions/1"), studentToken)
				.andExpect(apiStatus(200))).get("result");
		JsonNode inline = result.get("inlineComments");
		assertThat(inline).hasSize(2);
		assertThat(inline.get(0).get("kind").asString()).isEqualTo("ERROR");
		assertThat(inline.get(0).get("resolution").asString()).isEqualTo("FIXED");
		assertThat(inline.get(1).get("kind").asString()).isEqualTo("HIGHLIGHT");
		assertThat(result.get("errorSummary")).hasSize(1);
		assertThat(result.get("errorSummary").get(0).get("errorCode").asString()).isEqualTo("SV");

		// Lần 2 chưa trả bài → sinh viên chưa thấy kết quả.
		call(get("/api/student/parts/" + f.partId() + "/versions/2"), studentToken)
				.andExpect(jsonPath("$.data.result").isEmpty());
		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher).andExpect(apiStatus(409));
	}

	private static Map<String, Object> edit(String quote, String op, String text) {
		Map<String, Object> m = mark(quote, "ERROR", null, null);
		if (op != null) {
			m.put("edit", op);
		}
		if (text != null) {
			m.put("suggestion", text);
		}
		return m;
	}

	@Test
	@DisplayName("Sửa kiểu track changes (V10): thay thế / xoá / thêm chữ; gợi ý không kèm kiểu = thay thế; nhận xét (COMMENT) phải có nội dung")
	void edits() throws Exception {
		Fixture f = submitted();
		Map<String, Object> comment = mark("Governments", "COMMENT", null, "Câu chủ đề rõ");
		JsonNode list = data(saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of(
				edit("people is", "REPLACE", "people are"),
				edit("increasingly", "DELETE", null),
				edit("environment", "INSERT", " today"),
				edit("pollution", null, "air pollution"),
				comment))).andExpect(apiStatus(200))).get("inlineComments");
		assertThat(list.get(0).get("edit").asString()).isEqualTo("REPLACE");
		assertThat(list.get(1).get("edit").asString()).isEqualTo("DELETE");
		assertThat(list.get(1).get("suggestion").isNull()).isTrue();
		assertThat(list.get(2).get("edit").asString()).isEqualTo("INSERT");
		assertThat(list.get(3).get("kind").asString()).isEqualTo("COMMENT");
		assertThat(list.get(4).get("edit").asString()).isEqualTo("REPLACE"); // client cũ: chỉ có gợi ý sửa

		for (Map<String, Object> bad : List.of(
				edit("people is", "REPLACE", null), // thay thế mà không có chữ mới
				edit("people is", "INSERT", "  "),
				edit("people is", "DELETE", "x"), // xoá mà kèm chữ
				mark("people is", "COMMENT", null, null), // nhận xét rỗng
				mark("people is", "COMMENT", "SV", "x"))) { // nhận xét không mang mã lỗi
			saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of(bad))).andExpect(apiStatus(400));
		}
		Map<String, Object> highlightEdit = mark("people is", "HIGHLIGHT", null, null);
		highlightEdit.put("edit", "DELETE");
		saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of(highlightEdit))).andExpect(apiStatus(400));
	}

	@Test
	@DisplayName("BR-21: bài đang \"Cần sửa\" → sinh viên không thấy chữ sửa và kiểu sửa, vẫn thấy mã lỗi")
	void editsHiddenDuringRevision() throws Exception {
		Fixture f = submitted();
		Map<String, Object> e = edit("people is", "REPLACE", "people are");
		e.put("errorCode", "SV");
		saveGrade(f.submissionId(), grade(1, scores(f, 6, 6, 6, 6), List.of(), List.of(e, edit("increasingly", "DELETE", null))))
				.andExpect(apiStatus(200));
		call(postJson("/api/teacher/submissions/" + f.submissionId() + "/request-revision",
				Map.of("dueAt", clock.instant().plus(Duration.ofDays(2)).toString())), teacher).andExpect(apiStatus(200));
		JsonNode inline = data(call(get("/api/student/parts/" + f.partId() + "/versions/1"), studentToken)
				.andExpect(apiStatus(200))).get("result").get("inlineComments");
		assertThat(inline.get(0).get("errorCode").asString()).isEqualTo("SV");
		for (JsonNode n : inline) {
			assertThat(n.get("edit").isNull()).isTrue();
			assertThat(n.get("suggestion").isNull()).isTrue();
		}
	}
}
