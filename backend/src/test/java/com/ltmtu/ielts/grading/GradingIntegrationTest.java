package com.ltmtu.ielts.grading;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ltmtu.ielts.user.Role;

import tools.jackson.databind.JsonNode;

/** UC-17 — màn chấm và lưu nháp chấm: BR-11, BR-12, BR-17, BR-18, BR-19, BR-21. */
class GradingIntegrationTest extends GradingTestSupport {

	private static Map<String, Object> inline(int start, int end, String errorCode, String comment) {
		Map<String, Object> m = new HashMap<>(Map.of("startOffset", start, "endOffset", end));
		if (errorCode != null) {
			m.put("errorCode", errorCode);
		}
		if (comment != null) {
			m.put("comment", comment);
		}
		return m;
	}

	@Test
	@DisplayName("Màn chấm: bài làm, đề, tiêu chí kèm nhóm + checklist, cảnh báo thiếu số từ, chưa có kết quả, sửa được")
	void screen() throws Exception {
		Fixture f = submitted();
		call(get("/api/teacher/submissions/" + f.submissionId()), teacher)
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.submission.status").value("SUBMITTED"))
				.andExpect(jsonPath("$.data.submission.student.studentCode").value(student.getStudentCode()))
				.andExpect(jsonPath("$.data.submission.revisionsUsed").value(0))
				.andExpect(jsonPath("$.data.part.minWords").value(250))
				.andExpect(jsonPath("$.data.version.versionNo").value(1))
				.andExpect(jsonPath("$.data.version.content").value(ESSAY))
				.andExpect(jsonPath("$.data.version.wordCount").value(27))
				.andExpect(jsonPath("$.data.warnings[0].code").value("UNDER_MIN_WORDS"))
				.andExpect(jsonPath("$.data.criteria[0].code").value("TR"))
				.andExpect(jsonPath("$.data.criteria[0].group").value("TASK"))
				.andExpect(jsonPath("$.data.criteria[0].checklist[1].kind").value("LIMIT"))
				.andExpect(jsonPath("$.data.criteria[2].step").value(0.5))
				.andExpect(jsonPath("$.data.grade").isEmpty())
				.andExpect(jsonPath("$.data.versions[0].versionNo").value(1))
				.andExpect(jsonPath("$.data.editable").value(true))
				.andExpect(jsonPath("$.data.canReturn").value(false));
	}

	@Test
	@DisplayName("BR-15: bài nộp của giáo viên khác → 404 (xem, lưu chấm, trả bài); sinh viên → 403")
	void ownership() throws Exception {
		Fixture f = submitted();
		String other = accessToken(newUser(Role.TEACHER));
		call(get("/api/teacher/submissions/" + f.submissionId()), other).andExpect(apiStatus(404));
		call(putJson("/api/teacher/submissions/" + f.submissionId() + "/grade", grade(1, List.of(), List.of(), List.of())),
				other).andExpect(apiStatus(404));
		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), other).andExpect(apiStatus(404));
		call(get("/api/teacher/submissions/" + f.submissionId()), studentToken).andExpect(apiStatus(403));
	}

	@Test
	@DisplayName("UC-17 E1: lưu nháp khi chưa chấm đủ → được, band null, chưa trả được; trạng thái SUBMITTED → GRADED")
	void partialDraft() throws Exception {
		Fixture f = submitted();
		saveGrade(f.submissionId(), grade(1, List.of(score(f.criterion("TR"), 6)), List.of(), List.of()))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.complete").value(false))
				.andExpect(jsonPath("$.data.band").isEmpty())
				.andExpect(jsonPath("$.data.scores[0].score").value(6.0))
				.andExpect(jsonPath("$.data.scores[1].score").isEmpty());
		call(get("/api/teacher/submissions/" + f.submissionId()), teacher)
				.andExpect(jsonPath("$.data.submission.status").value("GRADED"))
				.andExpect(jsonPath("$.data.canReturn").value(false));
		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher)
				.andExpect(apiStatus(409))
				.andExpect(jsonPath("$.errorMessage").value(containsString("Coherence and Cohesion")));
	}

	@Test
	@DisplayName("BR-12: band phần do server tính — 6, 7, 6.5, 6 → 6.375 (chưa làm tròn) → hiển thị 6.5")
	void bandComputedByServer() throws Exception {
		Fixture f = submitted();
		saveGrade(f.submissionId(), grade(1, scores(f, 6, 7, 6.5, 6), List.of(), List.of()))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.complete").value(true))
				.andExpect(jsonPath("$.data.rawBand").value(6.375))
				.andExpect(jsonPath("$.data.band").value(6.5));
		call(get("/api/teacher/submissions/" + f.submissionId()), teacher)
				.andExpect(jsonPath("$.data.canReturn").value(true))
				.andExpect(jsonPath("$.data.versions[0].band").value(6.5))
				.andExpect(jsonPath("$.data.versions[0].gradeStatus").value("DRAFT"));
	}

	@Test
	@DisplayName("BR-11: điểm ngoài 0–9 hoặc sai bước (TR bước 1 mà cho 6.5) → 400; LR bước 0.5 thì 6.5 hợp lệ")
	void scoreStep() throws Exception {
		Fixture f = submitted();
		saveGrade(f.submissionId(), grade(1, List.of(score(f.criterion("TR"), 6.5)), List.of(), List.of()))
				.andExpect(apiStatus(400))
				.andExpect(jsonPath("$.errorMessage").value(containsString("bội số của 1")));
		saveGrade(f.submissionId(), grade(1, List.of(score(f.criterion("GRA"), 10)), List.of(), List.of()))
				.andExpect(apiStatus(400));
		saveGrade(f.submissionId(), grade(1, List.of(score(f.criterion("LR"), 6.5)), List.of(), List.of()))
				.andExpect(apiStatus(200));
	}

	@Test
	@DisplayName("BR-17: tick mục giới hạn → điểm vượt max_band bị 422 SCORE_LIMITED; nhiều mục lấy mức nhỏ nhất; ScoreDto báo giới hạn")
	void checklistLimit() throws Exception {
		Fixture f = submitted();
		List<Map<String, Object>> marks = List.of(mark(f.item("CC", 0), true));
		saveGrade(f.submissionId(), grade(1, scores(f, 6, 6, 6, 6), marks, List.of()))
				.andExpect(apiStatus(422))
				.andExpect(jsonPath("$.errorCode").value("SCORE_LIMITED"))
				.andExpect(jsonPath("$.errorMessage").value(containsString("Không chia đoạn")));
		saveGrade(f.submissionId(), grade(1, scores(f, 6, 5, 6, 6), marks, List.of()))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.scores[1].maxAllowed").value(5.0))
				.andExpect(jsonPath("$.data.scores[1].limitReason").value("Không chia đoạn"))
				.andExpect(jsonPath("$.data.scores[0].limitReason").isEmpty());

		List<Map<String, Object>> both = List.of(mark(f.item("CC", 0), true), mark(f.item("CC", 1), true));
		saveGrade(f.submissionId(), grade(1, scores(f, 6, 3, 6, 6), both, List.of()))
				.andExpect(apiStatus(422))
				.andExpect(jsonPath("$.errorMessage").value(containsString("tối đa 2")));
		// Mục CHECK không giới hạn điểm.
		saveGrade(f.submissionId(), grade(1, scores(f, 8, 6, 6, 6), List.of(mark(f.item("TR", 0), true)), List.of()))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.checklist[0].checked").value(true));
	}

	@Test
	@DisplayName("BR-18: bài ≤ 20 từ → cảnh báo SHORT_ESSAY, mọi tiêu chí tối đa 1")
	void shortEssay() throws Exception {
		Fixture f = submitted("Technology is good for people because it helps them.", Duration.ofDays(3), 2);
		call(get("/api/teacher/submissions/" + f.submissionId()), teacher)
				.andExpect(jsonPath("$.data.version.wordCount").value(9))
				.andExpect(jsonPath("$.data.warnings[0].code").value("SHORT_ESSAY"));
		saveGrade(f.submissionId(), grade(1, scores(f, 2, 1, 1, 1), List.of(), List.of()))
				.andExpect(apiStatus(422))
				.andExpect(jsonPath("$.errorCode").value("SCORE_LIMITED"));
		saveGrade(f.submissionId(), grade(1, scores(f, 1, 1, 1, 0), List.of(), List.of()))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.band").value(1.0));
	}

	@Test
	@DisplayName("BR-19: \"Chấm 0\" có lý do → mọi tiêu chí = 0, đủ để trả bài; kèm điểm khác 0 → 400; bỏ lý do thì chấm lại bình thường")
	void zeroGrade() throws Exception {
		Fixture f = submitted();
		Map<String, Object> body = grade(1, List.of(), List.of(), List.of());
		body.put("zeroReason", "NOT_ENGLISH");
		saveGrade(f.submissionId(), body)
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.zeroReason").value("NOT_ENGLISH"))
				.andExpect(jsonPath("$.data.complete").value(true))
				.andExpect(jsonPath("$.data.band").value(0.0))
				.andExpect(jsonPath("$.data.scores[3].score").value(0.0));

		Map<String, Object> bad = grade(1, List.of(score(f.criterion("TR"), 3)), List.of(), List.of());
		bad.put("zeroReason", "NO_ATTEMPT");
		saveGrade(f.submissionId(), bad).andExpect(apiStatus(400));
		bad.put("zeroReason", "LAZY");
		saveGrade(f.submissionId(), bad).andExpect(apiStatus(400));

		saveGrade(f.submissionId(), grade(1, scores(f, 5, 5, 5, 5), List.of(), List.of()))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.zeroReason").isEmpty())
				.andExpect(jsonPath("$.data.band").value(5.0));
	}

	@Test
	@DisplayName("BR-21: nhận xét inline — mã lỗi chuẩn hoá chữ hoa, tự gắn tiêu chí theo nhóm, đoạn trích lấy từ bài; lưu lại thì thay toàn bộ")
	void inlineComments() throws Exception {
		Fixture f = submitted();
		int start = ESSAY.indexOf("people is");
		int end = start + "people is".length();
		Map<String, Object> withSuggestion = inline(start, end, "sv", null);
		withSuggestion.put("suggestion", "people are");
		Map<String, Object> taskComment = inline(0, 15, "dev", "Mở bài chưa nêu quan điểm");
		Map<String, Object> plain = inline(ESSAY.indexOf("Governments"), ESSAY.indexOf("Governments") + 11, null, "Tốt");
		plain.put("quote", "Governments");
		JsonNode g = data(saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of(withSuggestion, taskComment, plain)))
				.andExpect(apiStatus(200)));
		JsonNode list = g.get("inlineComments");
		assertThat(list).hasSize(3);
		JsonNode first = list.get(0); // sắp theo vị trí
		assertThat(first.get("errorCode").asString()).isEqualTo("DEV");
		assertThat(first.get("criterionId").asLong()).isEqualTo(f.criterion("TR"));
		JsonNode sv = list.get(1);
		assertThat(sv.get("errorCode").asString()).isEqualTo("SV");
		assertThat(sv.get("quote").asString()).isEqualTo("people is");
		assertThat(sv.get("criterionId").asLong()).isEqualTo(f.criterion("GRA"));
		assertThat(sv.get("suggestion").asString()).isEqualTo("people are");
		assertThat(list.get(2).get("errorCode").isNull()).isTrue();
		assertThat(list.get(2).get("criterionId").isNull()).isTrue();

		// PUT thay toàn bộ: gửi danh sách rỗng thì nhận xét cũ bị xoá.
		saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of()))
				.andExpect(jsonPath("$.data.inlineComments").isEmpty());
	}

	@Test
	@DisplayName("BR-21: nhận xét inline sai — vị trí ngoài bài, đoạn trích lệch, không có nội dung, mã lỗi không tồn tại / của giáo viên khác → 400")
	void invalidInlineComments() throws Exception {
		Fixture f = submitted();
		String otherTeacher = accessToken(newUser(Role.TEACHER));
		call(postJson("/api/teacher/error-codes", Map.of("code", "XYZ", "name", "Mã của người khác")), otherTeacher)
				.andExpect(apiStatus(201));
		call(postJson("/api/teacher/error-codes", Map.of("code", "MINE", "name", "Mã của tôi", "criterion", "LR")), teacher)
				.andExpect(apiStatus(201));

		List<Map<String, Object>> bad = List.of(
				inline(0, ESSAY.length() + 1, "SP", null),
				inline(5, 5, "SP", null),
				inline(0, 5, null, "  "),
				inline(0, 5, "NOPE", null),
				inline(0, 5, "XYZ", null));
		for (Map<String, Object> c : bad) {
			saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of(c))).andExpect(apiStatus(400));
		}
		Map<String, Object> wrongQuote = inline(0, 2, "SP", null);
		wrongQuote.put("quote", "xx");
		saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of(wrongQuote)))
				.andExpect(apiStatus(400))
				.andExpect(jsonPath("$.errorMessage").value(containsString("không khớp")));

		saveGrade(f.submissionId(), grade(1, List.of(), List.of(), List.of(inline(0, 2, "mine", null))))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.inlineComments[0].criterionId").value(f.criterion("LR")));
	}

	@Test
	@DisplayName("Lưu chấm sai version / tiêu chí hoặc mục checklist của phần khác / gửi trùng tiêu chí → 409 / 400")
	void invalidReferences() throws Exception {
		Fixture f = submitted();
		Fixture other = submitted();
		saveGrade(f.submissionId(), grade(2, List.of(), List.of(), List.of())).andExpect(apiStatus(409));
		saveGrade(f.submissionId(), grade(1, List.of(score(other.criterion("TR"), 5)), List.of(), List.of()))
				.andExpect(apiStatus(400));
		saveGrade(f.submissionId(), grade(1, List.of(), List.of(mark(other.item("CC", 0), true)), List.of()))
				.andExpect(apiStatus(400));
		saveGrade(f.submissionId(),
				grade(1, List.of(score(f.criterion("TR"), 5), score(f.criterion("TR"), 6)), List.of(), List.of()))
				.andExpect(apiStatus(400));
		call(putJson("/api/teacher/submissions/" + f.submissionId() + "/grade", Map.of("scores", List.of())), teacher)
				.andExpect(apiStatus(400));
	}
}
