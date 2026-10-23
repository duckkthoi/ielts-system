package com.ltmtu.ielts.stats;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/**
 * Kết quả học tập của một sinh viên S (UC-28, FR-64):
 * <ul>
 * <li>X1 (giáo viên T, giao trực tiếp): Task 1 TA 6 / CC 5 → 5.5 (VT ×2, GOOD); Task 2 TR 7 / CC 6 → 6.5 (VT, ART);
 * đủ phần → band bài (5.5 + 6.5 × 2) / 3 = 6.17;</li>
 * <li>X2 (T, giao qua lớp): Task 1 nộp muộn, TA 4 đã trả; Task 2 nộp muộn, đang chấm nháp TR 9 (không tính, BR-13)
 * → chưa đủ phần;</li>
 * <li>X3 (giáo viên khác T2): Task 2 TR 8 (SP) đã trả — chỉ hiện ở phía sinh viên.</li>
 * </ul>
 */
class StudentStatsIntegrationTest extends StatsTestSupport {

	private User s;
	private long x1;
	private long x2;
	private long x3;
	private Instant x1ReturnedAt;

	@BeforeEach
	void setUp() throws Exception {
		s = newUser(Role.STUDENT);
		User teacher2 = newUser(Role.TEACHER);
		long classId = createClass(teacher, s);

		Created a1 = publish(Duration.ofDays(3), List.of(), List.of(s),
				List.of(part("Task 1", "WRITING_TASK1", "TA", "CC"), part("Task 2", "WRITING_TASK2", "TR", "CC")));
		Created a2 = publish(Duration.ofDays(1), List.of(classId), List.of(),
				List.of(part("Task 1", "WRITING_TASK1", "TA"), part("Task 2", "WRITING_TASK2", "TR")));
		x1 = a1.id();
		x2 = a2.id();

		long s11 = submit(s, a1.part(0));
		grade(s11, 1, scores(a1, 0, "TA", 6, "CC", 5), null,
				List.of(inline("people is", "VT"), inline("reduces", "VT"), inline("public transport", "GOOD")));
		returnGrade(s11);
		clock.advance(Duration.ofMinutes(1));
		long s12 = submit(s, a1.part(1));
		grade(s12, 1, scores(a1, 1, "TR", 7, "CC", 6), null,
				List.of(inline("people is", "VT"), inline("Governments", "ART")));
		returnGrade(s12);
		x1ReturnedAt = clock.instant();

		// X3 của giáo viên khác.
		clock.advance(Duration.ofMinutes(1));
		String mine = teacher;
		teacher = accessToken(teacher2);
		Created a3 = publish(Duration.ofDays(3), List.of(), List.of(s), List.of(part("Task 2", "WRITING_TASK2", "TR")));
		x3 = a3.id();
		long s3 = submit(s, a3.part(0));
		grade(s3, 1, Map.of(a3.criterion(0, "TR"), 8), null, List.of(inline("environment", "SP")));
		returnGrade(s3);
		teacher = mine;

		// X2: qua hạn mới nộp cả hai phần.
		clock.advance(Duration.ofDays(2));
		reloginTeacher();
		long s21 = submit(s, a2.part(0));
		grade(s21, Map.of(a2.criterion(0, "TA"), 4));
		returnGrade(s21);
		long s22 = submit(s, a2.part(1));
		grade(s22, Map.of(a2.criterion(1, "TR"), 9));
	}

	@Test
	@DisplayName("FR-64: giáo viên xem một sinh viên — chỉ bài của mình; số nộp / muộn; band theo thời gian; band bài chỉ khi đủ phần (BR-12)")
	void teacherView() throws Exception {
		JsonNode r = data(call(get("/api/teacher/stats/students/" + s.getId()), teacher).andExpect(apiStatus(200)));
		assertThat(r.get("student").get("studentCode").asString()).isEqualTo(s.getStudentCode());
		assertThat(r.get("submittedCount").asInt()).isEqualTo(4);
		assertThat(r.get("lateCount").asInt()).isEqualTo(2);
		assertThat(r.get("returnedCount").asInt()).isEqualTo(3);
		assertThat(r.get("averageBand").decimalValue()).isEqualByComparingTo("6.17");

		JsonNode assignments = r.get("assignments");
		assertThat(ids(assignments, "assignmentId")).containsExactly(x1, x2);
		assertThat(assignments.get(0).get("complete").asBoolean()).isTrue();
		assertThat(assignments.get(0).get("band").decimalValue()).isEqualByComparingTo("6.0");
		assertThat(assignments.get(0).get("returnedAt").asString()).isEqualTo(x1ReturnedAt.toString());
		assertThat(assignments.get(1).get("complete").asBoolean()).isFalse();
		assertThat(assignments.get(1).get("partCount").asInt()).isEqualTo(2);
		assertThat(assignments.get(1).get("returnedParts").asInt()).isEqualTo(1);
		assertThat(assignments.get(1).get("band").isNull()).isTrue();

		assertThat(bands(r.get("parts"))).containsExactly("5.5", "6.5", "4.0");

		assertThat(criteria(r)).containsExactly(Map.entry("TA", "5.00"), Map.entry("TR", "7.00"), Map.entry("CC", "5.50"));
		assertThat(codes(r)).containsExactly("VT", "ART");
		assertThat(r.get("errorProfile").get(0).get("count").asInt()).isEqualTo(3);
	}

	@Test
	@DisplayName("UC-28 / BR-13: sinh viên xem kết quả học tập — mọi bài được thấy (kể cả của giáo viên khác), chỉ điểm đã trả")
	void studentView() throws Exception {
		JsonNode r = data(call(get("/api/student/results"), accessToken(s)).andExpect(apiStatus(200)));
		assertThat(r.get("submittedCount").asInt()).isEqualTo(5);
		assertThat(r.get("lateCount").asInt()).isEqualTo(2);
		assertThat(r.get("returnedCount").asInt()).isEqualTo(4);
		// (6.1667 + 8) / 2
		assertThat(r.get("averageBand").decimalValue()).isEqualByComparingTo("7.08");
		assertThat(ids(r.get("assignments"), "assignmentId")).containsExactly(x1, x3, x2);
		assertThat(bands(r.get("parts"))).containsExactly("5.5", "6.5", "8.0", "4.0");
		assertThat(criteria(r)).containsExactly(Map.entry("TA", "5.00"), Map.entry("TR", "7.50"), Map.entry("CC", "5.50"));
		assertThat(codes(r)).containsExactly("VT", "ART", "SP");
	}

	@Test
	@DisplayName("Sinh viên chưa có bài nào: số liệu rỗng, trung bình null")
	void empty() throws Exception {
		User fresh = newUser(Role.STUDENT);
		JsonNode r = data(call(get("/api/student/results"), accessToken(fresh)).andExpect(apiStatus(200)));
		assertThat(r.get("submittedCount").asInt()).isZero();
		assertThat(r.get("averageBand").isNull()).isTrue();
		assertThat(r.get("assignments")).isEmpty();
		assertThat(r.get("errorProfile")).isEmpty();
	}

	@Test
	@DisplayName("Phân quyền: sinh viên không thuộc giáo viên / không phải sinh viên → 404; sai vai trò → 403")
	void access() throws Exception {
		User stranger = newUser(Role.STUDENT);
		call(get("/api/teacher/stats/students/" + stranger.getId()), teacher).andExpect(apiStatus(404));
		call(get("/api/teacher/stats/students/" + teacherUser.getId()), teacher).andExpect(apiStatus(404));

		String other = accessToken(newUser(Role.TEACHER));
		call(get("/api/teacher/stats/students/" + s.getId()), other).andExpect(apiStatus(404));

		call(get("/api/teacher/stats/students/" + s.getId()), accessToken(s)).andExpect(apiStatus(403));
		call(get("/api/student/results"), teacher).andExpect(apiStatus(403));
	}

	// ─── Hỗ trợ ─────────────────────────────────────────────────────────────

	private static Map<Long, Integer> scores(Created x, int part, String code1, int score1, String code2, int score2) {
		Map<Long, Integer> m = new LinkedHashMap<>();
		m.put(x.criterion(part, code1), score1);
		m.put(x.criterion(part, code2), score2);
		return m;
	}

	private static List<Long> ids(JsonNode list, String field) {
		List<Long> out = new ArrayList<>();
		list.forEach(n -> out.add(n.get(field).asLong()));
		return out;
	}

	private static List<String> bands(JsonNode parts) {
		List<String> out = new ArrayList<>();
		parts.forEach(n -> out.add(n.get("band").decimalValue().setScale(1).toPlainString()));
		return out;
	}

	private static List<Map.Entry<String, String>> criteria(JsonNode stats) {
		List<Map.Entry<String, String>> out = new ArrayList<>();
		stats.get("criteria").forEach(n -> out.add(Map.entry(n.get("code").asString(),
				n.get("average").decimalValue().setScale(2).toPlainString())));
		return out;
	}

	private static List<String> codes(JsonNode stats) {
		List<String> out = new ArrayList<>();
		stats.get("errorProfile").forEach(n -> out.add(n.get("code").asString()));
		return out;
	}
}
