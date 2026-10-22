package com.ltmtu.ielts.grading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Sinh viên xem kết quả (BR-13, BR-21, docs/08 mục 4.5), band cả bài (BR-12), số liệu danh sách bài của giáo viên. */
class StudentResultIntegrationTest extends GradingTestSupport {

	private static Map<String, Object> inline(String phrase, String errorCode, String suggestion) {
		int start = ESSAY.indexOf(phrase);
		Map<String, Object> m = new HashMap<>(Map.of("startOffset", start, "endOffset", start + phrase.length()));
		if (errorCode != null) {
			m.put("errorCode", errorCode);
		}
		if (suggestion != null) {
			m.put("suggestion", suggestion);
		} else if (errorCode == null) {
			m.put("comment", "Ý hay");
		}
		return m;
	}

	/** Chấm đủ: CC bị giới hạn 5 vì "Không chia đoạn"; 6 nhận xét inline (2 SV, 1 ART, 1 SP, 1 GOOD, 1 không mã). */
	private void gradeWithFeedback(Fixture f) throws Exception {
		Map<String, Object> body = grade(1, scores(f, 6, 5, 6.5, 6), List.of(mark(f.item("CC", 0), true)), List.of(
				inline("people is", "SV", "people are"),
				inline("it reduces", "SV", null),
				inline("the environment", "ART", null),
				inline("congestion", "SP", null),
				inline("public transport", "GOOD", null),
				inline("big cities", null, null)));
		body.put("generalComment", "Bài khá, cần chia đoạn.");
		saveGrade(f.submissionId(), body).andExpect(status().isOk());
	}

	private JsonNode version(long partId, int versionNo) throws Exception {
		return data(call(get("/api/student/parts/" + partId + "/versions/" + versionNo), studentToken)
				.andExpect(status().isOk()));
	}

	@Test
	@DisplayName("BR-13: kết quả đang chấm nháp không lộ ra; trả bài rồi mới thấy band, điểm, nhận xét")
	void resultOnlyWhenReturned() throws Exception {
		Fixture f = submitted();
		gradeWithFeedback(f);
		call(get("/api/student/parts/" + f.partId() + "/versions"), studentToken)
				.andExpect(jsonPath("$.data[0].returned").value(false))
				.andExpect(jsonPath("$.data[0].band").isEmpty());
		assertThat(version(f.partId(), 1).get("result").isNull()).isTrue();

		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher).andExpect(status().isOk());
		call(get("/api/student/parts/" + f.partId() + "/versions"), studentToken)
				.andExpect(jsonPath("$.data[0].returned").value(true))
				.andExpect(jsonPath("$.data[0].band").value(6.0))
				.andExpect(jsonPath("$.data[0].returnedAt").value(clock.instant().toString()));
		JsonNode result = version(f.partId(), 1).get("result");
		assertThat(result.get("band").decimalValue()).isEqualByComparingTo("6.0");
		assertThat(result.get("generalComment").asString()).isEqualTo("Bài khá, cần chia đoạn.");
		assertThat(result.get("graderName").asString()).isEqualTo(teacherUser.getFullName());
		assertThat(result.get("suggestionsHidden").asBoolean()).isFalse();
	}

	@Test
	@DisplayName("docs/08 4.5: tiêu chí bị giới hạn kèm lý do, checklist đạt / chưa đạt, nhận xét inline có tên mã lỗi")
	void criteriaAndInline() throws Exception {
		Fixture f = submitted();
		gradeWithFeedback(f);
		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher).andExpect(status().isOk());
		JsonNode result = version(f.partId(), 1).get("result");

		JsonNode cc = result.get("criteria").get(1);
		assertThat(cc.get("code").asString()).isEqualTo("CC");
		assertThat(cc.get("score").decimalValue()).isEqualByComparingTo("5");
		assertThat(cc.get("limitedTo").decimalValue()).isEqualByComparingTo("5");
		assertThat(cc.get("limitReason").asString()).isEqualTo("Không chia đoạn");
		assertThat(cc.get("checklist").get(0).get("checked").asBoolean()).isTrue();
		assertThat(cc.get("checklist").get(1).get("checked").asBoolean()).isFalse();
		JsonNode tr = result.get("criteria").get(0);
		assertThat(tr.get("limitedTo").isNull()).isTrue();
		assertThat(tr.get("checklist").get(0).get("kind").asString()).isEqualTo("CHECK");

		JsonNode inline = result.get("inlineComments");
		assertThat(inline).hasSize(6);
		JsonNode sv = inline.get(0); // "people is" đứng đầu bài nhất
		assertThat(sv.get("errorCode").asString()).isEqualTo("SV");
		assertThat(sv.get("errorName").asString()).isEqualTo("Hoà hợp chủ ngữ – động từ");
		assertThat(sv.get("criterionCode").asString()).isEqualTo("GRA");
		assertThat(sv.get("quote").asString()).isEqualTo("people is");
		assertThat(sv.get("suggestion").asString()).isEqualTo("people are");
	}

	@Test
	@DisplayName("docs/08 4.5: bảng \"Lỗi trong bài\" đếm theo tiêu chí (thứ tự rubric) rồi theo mã, nhiều nhất trước; bỏ mã khen và nhận xét không mã")
	void errorSummary() throws Exception {
		Fixture f = submitted();
		gradeWithFeedback(f);
		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher).andExpect(status().isOk());
		JsonNode summary = version(f.partId(), 1).get("result").get("errorSummary");
		assertThat(summary).hasSize(3);
		assertThat(summary.get(0).get("criterionCode").asString()).isEqualTo("LR");
		assertThat(summary.get(0).get("errorCode").asString()).isEqualTo("SP");
		assertThat(summary.get(1).get("errorCode").asString()).isEqualTo("SV");
		assertThat(summary.get(1).get("count").asInt()).isEqualTo(2);
		assertThat(summary.get(1).get("errorName").asString()).isEqualTo("Hoà hợp chủ ngữ – động từ");
		assertThat(summary.get(2).get("errorCode").asString()).isEqualTo("ART");
		assertThat(summary.get(2).get("count").asInt()).isEqualTo(1);
	}

	@Test
	@DisplayName("BR-21: đang \"Cần sửa\" → ẩn gợi ý sửa, vẫn thấy mã lỗi; nộp bản sửa rồi thì gợi ý của bản cũ hiện lại")
	void suggestionsHiddenDuringRevision() throws Exception {
		Fixture f = submitted();
		gradeWithFeedback(f);
		call(postJson("/api/teacher/submissions/" + f.submissionId() + "/request-revision",
				Map.of("dueAt", clock.instant().plus(Duration.ofDays(2)).toString())), teacher).andExpect(status().isOk());

		JsonNode result = version(f.partId(), 1).get("result");
		assertThat(result.get("suggestionsHidden").asBoolean()).isTrue();
		assertThat(result.get("inlineComments").get(0).get("errorCode").asString()).isEqualTo("SV");
		assertThat(result.get("inlineComments").get(0).get("suggestion").isNull()).isTrue();

		call(postJson("/api/student/parts/" + f.partId() + "/submit", Map.of("content", ESSAY + " Sửa.", "expectedVersion", 1)),
				studentToken).andExpect(status().isCreated());
		result = version(f.partId(), 1).get("result");
		assertThat(result.get("suggestionsHidden").asBoolean()).isFalse();
		assertThat(result.get("inlineComments").get(0).get("suggestion").asString()).isEqualTo("people are");
		assertThat(version(f.partId(), 2).get("result").isNull()).isTrue();
	}

	@Test
	@DisplayName("BR-21: bài tập bật \"hiện gợi ý khi cần sửa\" → gợi ý hiện ngay")
	void suggestionsShownWhenEnabled() throws Exception {
		Fixture f = submitted();
		call(patch("/api/teacher/assignments/" + f.assignmentId() + "/settings").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("showSuggestionsOnRevision", true))), teacher)
				.andExpect(status().isOk());
		gradeWithFeedback(f);
		call(postJson("/api/teacher/submissions/" + f.submissionId() + "/request-revision",
				Map.of("dueAt", clock.instant().plus(Duration.ofDays(2)).toString())), teacher).andExpect(status().isOk());
		JsonNode result = version(f.partId(), 1).get("result");
		assertThat(result.get("suggestionsHidden").asBoolean()).isFalse();
		assertThat(result.get("inlineComments").get(0).get("suggestion").asString()).isEqualTo("people are");
	}

	// ─── Band cả bài + số liệu danh sách ─────────────────────────────────────

	private static Map<String, Object> part(String title, String taskType, String c1, String c2) {
		List<Map<String, Object>> criteria = List.of(
				new HashMap<>(Map.of("code", c1, "name", c1, "step", "0.5", "checklist", List.of())),
				new HashMap<>(Map.of("code", c2, "name", c2, "step", "0.5", "checklist", List.of())));
		return new HashMap<>(Map.of("title", title, "taskType", taskType, "prompt", "<p>Đề</p>", "criteria", criteria));
	}

	@Test
	@DisplayName("BR-12: band cả bài chỉ có khi mọi phần đã trả; Task 2 trọng số 2 — 6.25 & 6.75 → 6.583 → 6.5; số liệu danh sách giáo viên")
	void assignmentBandAndTeacherCounts() throws Exception {
		User other = newUser(Role.STUDENT);
		Map<String, Object> body = new HashMap<>(Map.of("title", "Bài 2 phần", "dueAt",
				clock.instant().plus(Duration.ofDays(3)).toString(), "parts",
				List.of(part("Task 1", "WRITING_TASK1", "TA", "CC"), part("Task 2", "WRITING_TASK2", "TR", "CC"))));
		JsonNode a = data(call(postJson("/api/teacher/assignments", body), teacher).andExpect(status().isCreated()));
		long assignmentId = a.get("id").asLong();
		call(postJson("/api/teacher/assignments/" + assignmentId + "/publish",
				Map.of("studentIds", Set.of(student.getId(), other.getId()))), teacher).andExpect(status().isOk());
		JsonNode p1 = a.get("parts").get(0);
		JsonNode p2 = a.get("parts").get(1);
		for (JsonNode p : List.of(p1, p2)) {
			call(postJson("/api/student/parts/" + p.get("id").asLong() + "/submit",
					Map.of("content", ESSAY, "expectedVersion", 0)), studentToken).andExpect(status().isCreated());
		}
		String otherToken = accessToken(other);
		call(postJson("/api/student/parts/" + p1.get("id").asLong() + "/submit", Map.of("content", ESSAY, "expectedVersion", 0)),
				otherToken).andExpect(status().isCreated());

		JsonNode row = data(call(get("/api/teacher/assignments").param("keyword", "Bài 2 phần"), teacher)).get(0);
		assertThat(row.get("recipientCount").asLong()).isEqualTo(2);
		assertThat(row.get("completedCount").asLong()).isEqualTo(1); // chỉ sinh viên nộp đủ 2 phần
		assertThat(row.get("pendingGradingCount").asLong()).isEqualTo(3);

		long[] subs = new long[2];
		for (int i = 0; i < 2; i++) {
			long partId = (i == 0 ? p1 : p2).get("id").asLong();
			JsonNode q = data(call(get("/api/teacher/submissions").param("partId", String.valueOf(partId))
					.param("keyword", student.getStudentCode()), teacher));
			subs[i] = q.get(0).get("submissionId").asLong();
		}
		JsonNode c1 = p1.get("criteria");
		JsonNode c2 = p2.get("criteria");
		saveGrade(subs[0], grade(1, List.of(score(c1.get(0).get("id").asLong(), 6), score(c1.get(1).get("id").asLong(), 6.5)),
				List.of(), List.of())).andExpect(status().isOk());
		call(post("/api/teacher/submissions/" + subs[0] + "/return"), teacher).andExpect(status().isOk());

		JsonNode partial = data(call(get("/api/student/assignments/" + assignmentId + "/result"), studentToken)
				.andExpect(status().isOk()));
		assertThat(partial.get("complete").asBoolean()).isFalse();
		assertThat(partial.get("band").isNull()).isTrue();
		assertThat(partial.get("note").asString()).startsWith("Chưa đủ phần");
		assertThat(partial.get("parts").get(0).get("band").decimalValue()).isEqualByComparingTo("6.5");
		assertThat(partial.get("parts").get(1).get("band").isNull()).isTrue();

		// Phần 2 mới chấm nháp: sinh viên vẫn chưa thấy (BR-13).
		saveGrade(subs[1], grade(1, List.of(score(c2.get(0).get("id").asLong(), 7), score(c2.get(1).get("id").asLong(), 6.5)),
				List.of(), List.of())).andExpect(status().isOk());
		call(get("/api/student/assignments/" + assignmentId + "/result"), studentToken)
				.andExpect(jsonPath("$.data.complete").value(false));
		call(post("/api/teacher/submissions/" + subs[1] + "/return"), teacher).andExpect(status().isOk());

		call(get("/api/student/assignments/" + assignmentId + "/result"), studentToken)
				.andExpect(jsonPath("$.data.complete").value(true))
				.andExpect(jsonPath("$.data.band").value(6.5))
				.andExpect(jsonPath("$.data.parts[0].band").value(6.5))
				.andExpect(jsonPath("$.data.parts[1].band").value(7.0))
				.andExpect(jsonPath("$.data.parts[1].weight").value(2))
				.andExpect(jsonPath("$.data.parts[1].versionNo").value(1))
				.andExpect(jsonPath("$.data.note").value(StudentResultService.ESTIMATE_NOTE));
		call(get("/api/teacher/assignments").param("keyword", "Bài 2 phần"), teacher)
				.andExpect(jsonPath("$.data[0].pendingGradingCount").value(1));

		// Sinh viên khác chưa có kết quả nào; người không được giao → 404.
		call(get("/api/student/assignments/" + assignmentId + "/result"), otherToken)
				.andExpect(jsonPath("$.data.complete").value(false))
				.andExpect(jsonPath("$.data.parts[0].band").isEmpty());
		String outsider = accessToken(newUser(Role.STUDENT));
		call(get("/api/student/assignments/" + assignmentId + "/result"), outsider).andExpect(status().isNotFound());
		call(get("/api/student/parts/" + p1.get("id").asLong() + "/versions/1"), outsider).andExpect(status().isNotFound());
	}
}
