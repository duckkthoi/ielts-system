package com.ltmtu.ielts.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/**
 * Thống kê một bài (UC-19, FR-63). Bài 2 phần: Task 1 (TA, GRA, trọng số 1), Task 2 (TR, GRA, trọng số 2). Giao
 * trực tiếp cho A, B, C, E và cho lớp gồm D:
 * <ul>
 * <li>A: Task 1 = 6 / 6 (mã lỗi VT ×2, SP, GOOD), Task 2 = 7 / 6 → 6.5 (VT); đã trả cả hai → band bài 6.33;</li>
 * <li>B: Task 1 = 5 / 5 (ART), Task 2 = 5 / 5; đã trả cả hai → band bài 5.0;</li>
 * <li>C: Task 1 = 7 / 7 rồi bị yêu cầu sửa, bản sửa đang chấm nháp 9 / 9 (không tính); Task 2 nộp muộn, chưa chấm;</li>
 * <li>D: Task 1 "Chấm 0" đã trả; Task 2 chưa nộp. E: chưa làm gì.</li>
 * </ul>
 */
class AssignmentStatsIntegrationTest extends StatsTestSupport {

	private User a;
	private long classId;
	private long assignmentId;

	@BeforeEach
	void setUp() throws Exception {
		a = newUser(Role.STUDENT);
		User b = newUser(Role.STUDENT);
		User c = newUser(Role.STUDENT);
		User d = newUser(Role.STUDENT);
		User e = newUser(Role.STUDENT);
		classId = createClass(teacher, d);
		Created x = publish(Duration.ofDays(3), List.of(classId), List.of(a, b, c, e),
				List.of(part("Task 1", "WRITING_TASK1", "TA", "GRA"), part("Task 2", "WRITING_TASK2", "TR", "GRA")));
		assignmentId = x.id();
		long t1 = x.part(0);
		long t2 = x.part(1);

		long a1 = submit(a, t1);
		grade(a1, 1, scores(x, 0, "TA", 6, "GRA", 6), null, List.of(inline("people is", "VT"),
				inline("reduces", "VT"), inline("environment", "SP"), inline("public transport", "GOOD")));
		returnGrade(a1);
		long a2 = submit(a, t2);
		grade(a2, 1, scores(x, 1, "TR", 7, "GRA", 6), null, List.of(inline("people is", "VT")));
		returnGrade(a2);

		long b1 = submit(b, t1);
		grade(b1, 1, scores(x, 0, "TA", 5, "GRA", 5), null, List.of(inline("Governments", "ART")));
		returnGrade(b1);
		long b2 = submit(b, t2);
		grade(b2, scores(x, 1, "TR", 5, "GRA", 5));
		returnGrade(b2);

		long c1 = submit(c, t1);
		grade(c1, scores(x, 0, "TA", 7, "GRA", 7));
		call(postJson("/api/teacher/submissions/" + c1 + "/request-revision",
				Map.of("dueAt", clock.instant().plus(Duration.ofDays(10)).toString())), teacher)
				.andExpect(status().isOk());
		submit(c, t1, 1);
		grade(c1, 2, scores(x, 0, "TA", 9, "GRA", 9), null, List.of());

		long d1 = submit(d, t1);
		grade(d1, 1, Map.of(), "NOT_ENGLISH", List.of());
		returnGrade(d1);

		clock.advance(Duration.ofDays(4));
		reloginTeacher();
		submit(c, t2);
	}

	@Test
	@DisplayName("UC-19: số nộp, tỉ lệ đúng hạn, band trung bình phần / tiêu chí, phân bố band — chỉ kết quả đã trả (BR-13), kể cả \"Chấm 0\"")
	void perPart() throws Exception {
		JsonNode s = stats(null);
		assertThat(s.get("students").asInt()).isEqualTo(5);

		JsonNode t1 = s.get("perPart").get(0);
		assertThat(t1.get("orderNo").asInt()).isEqualTo(1);
		assertThat(t1.get("submittedCount").asInt()).isEqualTo(4);
		assertThat(t1.get("lateCount").asInt()).isZero();
		assertThat(t1.get("onTimeRate").asDouble()).isEqualTo(1.0);
		assertThat(t1.get("returnedCount").asInt()).isEqualTo(4);
		// 6, 5, 7 (bản trả của C, không phải bản nháp 9), 0
		assertThat(t1.get("averageBand").decimalValue()).isEqualByComparingTo("4.50");
		assertThat(criterion(t1, "TA").get("average").decimalValue()).isEqualByComparingTo("4.50");
		assertThat(criterion(t1, "TA").get("count").asInt()).isEqualTo(4);
		assertThat(distribution(t1.get("bandDistribution")))
				.containsExactly(Map.entry("0.0", 1), Map.entry("5.0", 1), Map.entry("6.0", 1), Map.entry("7.0", 1));

		JsonNode t2 = s.get("perPart").get(1);
		assertThat(t2.get("taskType").asString()).isEqualTo("WRITING_TASK2");
		assertThat(t2.get("submittedCount").asInt()).isEqualTo(3);
		assertThat(t2.get("lateCount").asInt()).isEqualTo(1);
		assertThat(t2.get("onTimeRate").asDouble()).isCloseTo(2.0 / 3, offset(1e-9));
		assertThat(t2.get("returnedCount").asInt()).isEqualTo(2);
		assertThat(t2.get("averageBand").decimalValue()).isEqualByComparingTo("5.75");
		assertThat(criterion(t2, "TR").get("average").decimalValue()).isEqualByComparingTo("6.00");
		assertThat(criterion(t2, "GRA").get("average").decimalValue()).isEqualByComparingTo("5.50");
		assertThat(distribution(t2.get("bandDistribution"))).containsExactly(Map.entry("5.0", 1), Map.entry("6.5", 1));
	}

	@Test
	@DisplayName("BR-12: band cả bài chỉ tính sinh viên đã được trả mọi phần, Task 2 trọng số 2")
	void overall() throws Exception {
		JsonNode o = stats(null).get("overall");
		assertThat(o.get("count").asInt()).isEqualTo(2);
		// A: (6 × 1 + 6.5 × 2) / 3 = 6.333…; B: 5.0
		assertThat(o.get("averageBand").decimalValue()).isEqualByComparingTo("5.67");
		assertThat(distribution(o.get("bandDistribution"))).containsExactly(Map.entry("5.0", 1), Map.entry("6.5", 1));
	}

	@Test
	@DisplayName("Mã lỗi hay gặp: đếm số lần và số sinh viên, kèm tên và nhóm tiêu chí, bỏ mã khen GOOD")
	void topErrors() throws Exception {
		JsonNode errors = stats(null).get("topErrors");
		assertThat(errors).hasSize(3);
		assertThat(errors.get(0).get("code").asString()).isEqualTo("VT");
		assertThat(errors.get(0).get("name").asString()).isEqualTo("Thì động từ");
		assertThat(errors.get(0).get("criterion").asString()).isEqualTo("GRA");
		assertThat(errors.get(0).get("count").asInt()).isEqualTo(3);
		assertThat(errors.get(0).get("studentCount").asInt()).isEqualTo(1);
		assertThat(errors.get(1).get("code").asString()).isEqualTo("ART");
		assertThat(errors.get(2).get("code").asString()).isEqualTo("SP");
	}

	@Test
	@DisplayName("Lọc theo lớp: chỉ thành viên lớp; phần chưa có dữ liệu trả null thay vì 0")
	void byClass() throws Exception {
		JsonNode s = stats(classId);
		assertThat(s.get("students").asInt()).isEqualTo(1);
		JsonNode t1 = s.get("perPart").get(0);
		assertThat(t1.get("submittedCount").asInt()).isEqualTo(1);
		assertThat(t1.get("averageBand").decimalValue()).isEqualByComparingTo("0.00");
		JsonNode t2 = s.get("perPart").get(1);
		assertThat(t2.get("submittedCount").asInt()).isZero();
		assertThat(t2.get("onTimeRate").isNull()).isTrue();
		assertThat(t2.get("averageBand").isNull()).isTrue();
		assertThat(criterion(t2, "TR").get("average").isNull()).isTrue();
		assertThat(t2.get("bandDistribution")).isEmpty();
		assertThat(s.get("overall").get("count").asInt()).isZero();
		assertThat(s.get("overall").get("averageBand").isNull()).isTrue();
		assertThat(s.get("topErrors")).isEmpty();
	}

	@Test
	@DisplayName("Phân quyền: bài của giáo viên khác → 404, lớp của giáo viên khác → 400, sinh viên → 403")
	void access() throws Exception {
		String other = accessToken(newUser(Role.TEACHER));
		call(get(url()), other).andExpect(status().isNotFound());
		call(get(url()).param("classId", String.valueOf(createClass(other))), teacher).andExpect(status().isBadRequest());
		call(get(url()), accessToken(a)).andExpect(status().isForbidden());
	}

	// ─── Hỗ trợ ─────────────────────────────────────────────────────────────

	private static Map<Long, Integer> scores(Created x, int part, String code1, int score1, String code2, int score2) {
		Map<Long, Integer> m = new LinkedHashMap<>();
		m.put(x.criterion(part, code1), score1);
		m.put(x.criterion(part, code2), score2);
		return m;
	}

	private String url() {
		return "/api/teacher/assignments/" + assignmentId + "/stats";
	}

	private JsonNode stats(Long classId) throws Exception {
		MockHttpServletRequestBuilder req = get(url());
		if (classId != null) {
			req.param("classId", String.valueOf(classId));
		}
		return data(call(req, teacher).andExpect(status().isOk()));
	}

	private static JsonNode criterion(JsonNode part, String code) {
		for (JsonNode c : part.get("criteria")) {
			if (code.equals(c.get("code").asString())) {
				return c;
			}
		}
		throw new AssertionError("Thiếu tiêu chí " + code);
	}

	/** Phân bố band dạng ("6.5" → số bài), giữ thứ tự trả về. */
	private static List<Map.Entry<String, Integer>> distribution(JsonNode list) {
		List<Map.Entry<String, Integer>> out = new ArrayList<>();
		list.forEach(n -> out.add(Map.entry(n.get("band").decimalValue().setScale(1).toPlainString(), n.get("count").asInt())));
		return out;
	}
}
