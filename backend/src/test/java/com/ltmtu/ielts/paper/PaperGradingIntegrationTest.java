package com.ltmtu.ielts.paper;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/**
 * Chấm bài PAPER thang 10 theo rubric phân tích (B25b, BR-31): mức đạt khi soạn bài, chọn mức + điểm trong khoảng,
 * không áp quy tắc IELTS, cộng / trừ riêng từng thành viên.
 */
class PaperGradingIntegrationTest extends PaperTestSupport {

	private String teacher;
	private User a;
	private User b;
	private User outsider;
	private long assignmentId;
	private long submissionId;
	private JsonNode method;
	private JsonNode style;

	/** 4 mức kiểu rubric mẫu paper IT. */
	private static List<Map<String, Object>> levels() {
		return List.of(level("Xuất sắc", "9", "10"), level("Tốt", "7", "8.75"), level("Đạt", "5", "6.75"),
				level("Chưa đạt", "0", "4.75"));
	}

	private static Map<String, Object> level(String label, String min, String max) {
		return Map.of("label", label, "minScore", min, "maxScore", max, "descriptor", "Mô tả mức " + label);
	}

	private static Map<String, Object> criterion(String name, String weight, List<Map<String, Object>> levels) {
		Map<String, Object> c = new HashMap<>();
		c.put("name", name);
		c.put("weight", weight);
		c.put("checklist", List.of());
		c.put("levels", levels);
		return c;
	}

	private Map<String, Object> stageWith(List<Map<String, Object>> criteria) {
		Map<String, Object> p = new HashMap<>(stage("Bản cuối", List.of(section("Abstract", true))));
		p.put("criteria", criteria);
		return p;
	}

	@BeforeEach
	void setUp() throws Exception {
		documentService.clear();
		teacher = accessToken(newUser(Role.TEACHER));
		a = newUser(Role.STUDENT);
		b = newUser(Role.STUDENT);
		outsider = newUser(Role.STUDENT);
		assignmentId = publishedPaper(teacher, Map.of("parts", List.of(stageWith(List.of(
				criterion("Phương pháp", "2", levels()), criterion("Văn phong", "1", levels()))))), a, b, outsider);
		JsonNode part = data(call(get("/api/teacher/assignments/" + assignmentId), teacher)).get("parts").get(0);
		method = part.get("criteria").get(0);
		style = part.get("criteria").get(1);
		call(postJson("/api/teacher/assignments/" + assignmentId + "/groups",
				Map.of("name", "Nhóm AB", "studentIds", List.of(a.getId(), b.getId()))), teacher).andExpect(apiStatus(200));
		// Bài ngắn (3 từ) — bài PAPER không bị giới hạn điểm ≤ 20 từ của IELTS.
		writeDocument(a, part.get("id").asLong(), docJson(sectionsOf("abstract", "Short but fine.")), "Abstract Short");
		call(postJson("/api/student/parts/" + part.get("id").asLong() + "/submit", Map.of("expectedVersion", 0)),
				accessToken(a)).andExpect(apiStatus(200));
		submissionId = data(call(get("/api/teacher/submissions").param("assignmentId", String.valueOf(assignmentId)),
				teacher)).get(0).get("submissionId").asLong();
	}

	private long levelId(JsonNode criterion, String label) {
		for (JsonNode l : criterion.get("levels")) {
			if (l.get("label").asString().equals(label)) {
				return l.get("id").asLong();
			}
		}
		throw new AssertionError("Không có mức " + label);
	}

	private static Map<String, Object> score(JsonNode criterion, String score, Long levelId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("criterionId", criterion.get("id").asLong());
		m.put("score", score);
		m.put("levelId", levelId);
		return m;
	}

	private ResultActions saveGrade(List<Map<String, Object>> scores, List<Map<String, Object>> adjustments)
			throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("versionNo", 1);
		body.put("scores", scores);
		body.put("memberAdjustments", adjustments);
		return call(put("/api/teacher/submissions/" + submissionId + "/grade").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(body)), teacher);
	}

	@Test
	@DisplayName("BR-31: tiêu chí bài PAPER lưu được mức đạt và bước 0,25 (đọc lại từ DB vẫn 0.25, không bị làm tròn 0.3); nhân bản giữ mức")
	void levelsAndQuarterStepPersist() throws Exception {
		assertThat(method.get("step").decimalValue()).isEqualByComparingTo("0.25");
		assertThat(method.get("levels")).hasSize(4);
		assertThat(method.get("levels").get(1).get("maxScore").decimalValue()).isEqualByComparingTo("8.75");
		assertThat(method.get("levels").get(1).get("descriptor").asString()).isEqualTo("Mô tả mức Tốt");

		JsonNode copy = data(call(post("/api/teacher/assignments/" + assignmentId + "/duplicate"), teacher));
		JsonNode copied = data(call(get("/api/teacher/assignments/" + copy.get("id").asLong()), teacher)).get("parts")
				.get(0).get("criteria").get(0);
		assertThat(copied.get("levels")).hasSize(4);
		assertThat(copied.get("levels").get(3).get("label").asString()).isEqualTo("Chưa đạt");
	}

	@Test
	@DisplayName("BR-31: mức đạt sai → 400 (bài luận có mức, 1 mức, chồng khoảng, vượt điểm tối đa, không là bội số của bước)")
	void invalidLevels() throws Exception {
		Map<String, Object> essay = new HashMap<>();
		essay.put("title", "Essay");
		essay.put("dueAt", clock.instant().plusSeconds(86_400).toString());
		Map<String, Object> essayCriterion = new HashMap<>(criterion("TR", "1", levels()));
		essayCriterion.put("code", "TR");
		essay.put("parts", List.of(Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>x</p>",
				"criteria", List.of(essayCriterion))));
		call(postJson("/api/teacher/assignments", essay), teacher).andExpect(apiStatus(400))
				.andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.containsString("chỉ bài Paper")));

		for (List<Map<String, Object>> bad : List.of(List.of(level("Một", "0", "10")),
				List.of(level("A", "5", "8"), level("B", "7", "10")),
				List.of(level("A", "0", "4"), level("B", "5", "10.25")),
				List.of(level("A", "0", "4.1"), level("B", "5", "10")))) {
			call(postJson("/api/teacher/assignments", paperBody(Map.of("parts",
					List.of(stageWith(List.of(criterion("X", "1", bad))))))), teacher).andExpect(apiStatus(400));
		}
	}

	@Test
	@DisplayName("BR-31: chọn mức → điểm phải trong khoảng (422 LEVEL_MISMATCH); điểm cả bài là trung bình có trọng số, không làm tròn kiểu IELTS, không giới hạn ≤ 20 từ; hàng đợi ghi tên nhóm")
	void gradeWithLevels() throws Exception {
		saveGrade(List.of(score(method, "9", levelId(method, "Tốt"))), null).andExpect(apiStatus(422))
				.andExpect(jsonPath("$.errorCode").value("LEVEL_MISMATCH"));
		saveGrade(List.of(score(method, "7", levelId(style, "Tốt"))), null).andExpect(apiStatus(400));

		JsonNode grade = data(saveGrade(List.of(score(method, "7.25", levelId(method, "Tốt")),
				score(style, "8.5", levelId(style, "Tốt"))), null));
		// (7,25 × 2 + 8,5 × 1) / 3 = 7,6667 → hiện 7.67 (kiểu IELTS sẽ là 7.5)
		assertThat(grade.get("band").decimalValue()).isEqualByComparingTo("7.67");
		assertThat(grade.get("scores").get(0).get("levelId").asLong()).isEqualTo(levelId(method, "Tốt"));
		assertThat(grade.get("scores").get(0).get("score").decimalValue()).isEqualByComparingTo("7.25");
		assertThat(grade.get("scores").get(0).get("maxAllowed").decimalValue()).isEqualByComparingTo("10");

		JsonNode screen = data(call(get("/api/teacher/submissions/" + submissionId), teacher));
		assertThat(screen.get("assignment").get("kind").asString()).isEqualTo("PAPER");
		assertThat(screen.get("warnings")).isEmpty();
		assertThat(screen.get("criteria").get(0).get("levels")).hasSize(4);
		assertThat(screen.get("members")).hasSize(2);
		// Đọc lại từ DB: 7.25 không bị làm tròn
		assertThat(screen.get("grade").get("scores").get(0).get("score").decimalValue()).isEqualByComparingTo("7.25");

		// B25c-3: hàng đợi ghi tên nhóm + điểm thang 10 (không làm tròn kiểu IELTS); tìm được theo tên nhóm
		JsonNode row = data(call(get("/api/teacher/submissions").param("status", "GRADED").param("keyword", "nhóm ab"),
				teacher)).get(0);
		assertThat(row.get("submissionId").asLong()).isEqualTo(submissionId);
		assertThat(row.get("kind").asString()).isEqualTo("PAPER");
		assertThat(row.get("groupName").asString()).isEqualTo("Nhóm AB");
		assertThat(row.get("band").decimalValue()).isEqualByComparingTo("7.67");
	}

	@Test
	@DisplayName("Cộng / trừ riêng thành viên: ±2 bước 0,25, có lý do, chỉ thành viên nhóm; điểm thành viên kẹp [0, 10]; lưu lại thì cập nhật tại chỗ")
	void memberAdjustments() throws Exception {
		List<Map<String, Object>> scores = List.of(score(method, "9.5", levelId(method, "Xuất sắc")),
				score(style, "9.5", null));
		saveGrade(scores, List.of(Map.of("studentId", outsider.getId(), "delta", "1", "reason", "x")))
				.andExpect(apiStatus(400));
		saveGrade(scores, List.of(Map.of("studentId", a.getId(), "delta", "2.25", "reason", "x"))).andExpect(apiStatus(400));
		saveGrade(scores, List.of(Map.of("studentId", a.getId(), "delta", "1", "reason", " "))).andExpect(apiStatus(400));

		JsonNode grade = data(saveGrade(scores, List.of(Map.of("studentId", a.getId(), "delta", "1", "reason",
				"Viết phần phương pháp"), Map.of("studentId", b.getId(), "delta", "-0.75", "reason", "Ít tham gia"))));
		Map<Long, JsonNode> byStudent = new HashMap<>();
		grade.get("adjustments").forEach(x -> byStudent.put(x.get("studentId").asLong(), x));
		assertThat(byStudent.get(a.getId()).get("score").decimalValue()).isEqualByComparingTo("10");
		assertThat(byStudent.get(b.getId()).get("score").decimalValue()).isEqualByComparingTo("8.75");

		JsonNode again = data(saveGrade(scores, List.of(Map.of("studentId", b.getId(), "delta", "-1", "reason",
				"Ít tham gia, nộp muộn phần việc"))));
		assertThat(again.get("adjustments")).hasSize(1);
		assertThat(again.get("adjustments").get(0).get("delta").decimalValue()).isEqualByComparingTo("-1");
		assertThat(again.get("adjustments").get(0).get("reason").asString()).isEqualTo("Ít tham gia, nộp muộn phần việc");
	}

	@Test
	@DisplayName("Trả bài PAPER: thông báo ghi \"Điểm: x / 10\" cho mọi thành viên")
	void returnNotifiesScoreOutOfTen() throws Exception {
		saveGrade(List.of(score(method, "7.25", null), score(style, "8.5", null)), null).andExpect(apiStatus(200));
		call(post("/api/teacher/submissions/" + submissionId + "/return"), teacher).andExpect(apiStatus(200));
		for (User member : List.of(a, b)) {
			JsonNode n = data(call(get("/api/notifications"), accessToken(member))).get(0);
			assertThat(n.get("type").asString()).isEqualTo("GRADE_RETURNED");
			assertThat(n.get("body").asString()).isEqualTo("Điểm: 7.67 / 10");
		}
	}

	@Test
	@DisplayName("B25e / BR-31: sinh viên thấy điểm nhóm thang 10 và điểm riêng của mình (đã cộng / trừ, kèm lý do); tổng bài theo điểm riêng")
	void studentSeesOwnScore() throws Exception {
		saveGrade(List.of(score(method, "7.25", null), score(style, "8.5", null)),
				List.of(Map.of("studentId", b.getId(), "delta", "-1", "reason", "Ít tham gia"))).andExpect(apiStatus(200));
		call(post("/api/teacher/submissions/" + submissionId + "/return"), teacher).andExpect(apiStatus(200));
		long partId = data(call(get("/api/teacher/assignments/" + assignmentId), teacher)).get("parts").get(0).get("id")
				.asLong();

		JsonNode ra = data(call(get("/api/student/parts/" + partId + "/versions/1"), accessToken(a))).get("result");
		assertThat(ra.get("band").decimalValue()).isEqualByComparingTo("7.67");
		assertThat(ra.get("myScore").decimalValue()).isEqualByComparingTo("7.67");
		assertThat(ra.get("myAdjustment").isNull()).isTrue();

		JsonNode rb = data(call(get("/api/student/parts/" + partId + "/versions/1"), accessToken(b))).get("result");
		assertThat(rb.get("band").decimalValue()).isEqualByComparingTo("7.67");
		assertThat(rb.get("myScore").decimalValue()).isEqualByComparingTo("6.67");
		assertThat(rb.get("myAdjustment").decimalValue()).isEqualByComparingTo("-1");
		assertThat(rb.get("myAdjustmentReason").asString()).isEqualTo("Ít tham gia");

		JsonNode total = data(call(get("/api/student/assignments/" + assignmentId + "/result"), accessToken(b)));
		assertThat(total.get("parts").get(0).get("band").decimalValue()).isEqualByComparingTo("6.67");
		assertThat(total.get("band").decimalValue()).isEqualByComparingTo("6.67");
	}
}
