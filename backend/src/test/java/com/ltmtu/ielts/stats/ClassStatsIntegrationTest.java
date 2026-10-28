package com.ltmtu.ielts.stats;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/**
 * Thống kê lớp (FR-65). Lớp gồm P, Q, R, U; V được giao trực tiếp (không phải thành viên):
 * <ul>
 * <li>A1 (giao lớp + V, hạn 3 ngày): P, Q: Task 1 = 6, Task 2 = 7 → 6.67; R: Task 1 = 5 đã trả, Task 2 nộp muộn chưa
 * chấm; V: 9 / 9 (không tính);</li>
 * <li>A2 (giao lớp, hạn 5 ngày, một phần): P = 5, R = 4;</li>
 * <li>A3 (chỉ giao trực tiếp cho P) — không thuộc lớp, không tính.</li>
 * </ul>
 */
class ClassStatsIntegrationTest extends StatsTestSupport {

	private User p;
	private User q;
	private User r;
	private User u;
	private long classId;
	private long a1;
	private long a2;

	@BeforeEach
	void setUp() throws Exception {
		p = newUser(Role.STUDENT);
		q = newUser(Role.STUDENT);
		r = newUser(Role.STUDENT);
		u = newUser(Role.STUDENT);
		User v = newUser(Role.STUDENT);
		classId = createClass(teacher, p, q, r, u);

		Created x1 = publish(Duration.ofDays(3), List.of(classId), List.of(v),
				List.of(part("Task 1", "WRITING_TASK1", "TA"), part("Task 2", "WRITING_TASK2", "TR")));
		Created x2 = publish(Duration.ofDays(5), List.of(classId), List.of(), List.of(part("Task 2", "WRITING_TASK2", "TR")));
		Created x3 = publish(Duration.ofDays(5), List.of(), List.of(p), List.of(part("Task 2", "WRITING_TASK2", "TR")));
		a1 = x1.id();
		a2 = x2.id();

		for (User s : List.of(p, q)) {
			graded(s, x1, 0, "TA", 6);
			graded(s, x1, 1, "TR", 7);
		}
		graded(r, x1, 0, "TA", 5);
		graded(v, x1, 0, "TA", 9);
		graded(v, x1, 1, "TR", 9);
		graded(p, x2, 0, "TR", 5);
		graded(r, x2, 0, "TR", 4);
		graded(p, x3, 0, "TR", 9);

		clock.advance(Duration.ofDays(4));
		reloginTeacher();
		submit(r, x1.part(1));
	}

	private void graded(User s, Created x, int part, String code, int score) throws Exception {
		long id = submit(s, x.part(part));
		grade(id, Map.of(x.criterion(part, code), score));
		returnGrade(id);
	}

	@Test
	@DisplayName("FR-65: các bài giao cho lớp — nộp đủ / đã trả đủ / band trung bình, chỉ thành viên lớp, chỉ bài giao theo lớp")
	void assignments() throws Exception {
		JsonNode s = stats();
		assertThat(s.get("classroom").get("memberCount").asLong()).isEqualTo(4);
		// 6.67, 6.67, 5, 4
		assertThat(s.get("averageBand").decimalValue()).isEqualByComparingTo("5.58");

		JsonNode list = s.get("assignments");
		assertThat(list).hasSize(2);
		JsonNode first = list.get(0);
		assertThat(first.get("assignmentId").asLong()).isEqualTo(a1);
		assertThat(first.get("partCount").asInt()).isEqualTo(2);
		assertThat(first.get("completedCount").asInt()).isEqualTo(3);
		assertThat(first.get("completedRate").asDouble()).isEqualTo(0.75);
		assertThat(first.get("gradedCount").asInt()).isEqualTo(2);
		assertThat(first.get("averageBand").decimalValue()).isEqualByComparingTo("6.67");
		JsonNode second = list.get(1);
		assertThat(second.get("assignmentId").asLong()).isEqualTo(a2);
		assertThat(second.get("completedCount").asInt()).isEqualTo(2);
		assertThat(second.get("averageBand").decimalValue()).isEqualByComparingTo("4.50");
	}

	@Test
	@DisplayName("FR-65: bảng xếp hạng theo band trung bình các bài đủ phần; chưa có band → không xếp hạng, đứng cuối")
	void ranking() throws Exception {
		JsonNode rows = stats().get("ranking");
		assertThat(rows).hasSize(4);
		assertRow(rows.get(0), 1, q, "6.67", 1, 2, 0);
		assertRow(rows.get(1), 2, p, "5.83", 2, 3, 0);
		assertRow(rows.get(2), 3, r, "4.00", 1, 3, 1);
		assertThat(rows.get(3).get("rank").isNull()).isTrue();
		assertThat(rows.get(3).get("student").get("id").asLong()).isEqualTo(u.getId());
		assertThat(rows.get(3).get("averageBand").isNull()).isTrue();
		assertThat(rows.get(3).get("submittedParts").asInt()).isZero();

		// Ma trận điểm từng học sinh × từng bài: chỉ có band khi mọi phần của bài đã được trả.
		assertThat(rows.get(0).get("assignmentBands").get(String.valueOf(a1)).decimalValue())
				.isEqualByComparingTo("6.67");
		assertThat(rows.get(0).get("assignmentBands").get(String.valueOf(a2)).isNull()).isTrue();
		assertThat(rows.get(1).get("assignmentBands").get(String.valueOf(a1)).decimalValue())
				.isEqualByComparingTo("6.67");
		assertThat(rows.get(1).get("assignmentBands").get(String.valueOf(a2)).decimalValue())
				.isEqualByComparingTo("5.00");
		assertThat(rows.get(2).get("assignmentBands").get(String.valueOf(a1)).isNull()).isTrue();
		assertThat(rows.get(2).get("assignmentBands").get(String.valueOf(a2)).decimalValue())
				.isEqualByComparingTo("4.00");
	}

	@Test
	@DisplayName("Phân quyền: lớp của giáo viên khác → 404, sinh viên → 403")
	void access() throws Exception {
		call(get(url()), accessToken(newUser(Role.TEACHER))).andExpect(apiStatus(404));
		call(get(url()), accessToken(p)).andExpect(apiStatus(403));
	}

	private String url() {
		return "/api/teacher/stats/classes/" + classId;
	}

	private JsonNode stats() throws Exception {
		return data(call(get(url()), teacher).andExpect(apiStatus(200)));
	}

	private static void assertRow(JsonNode row, int rank, User student, String band, int graded, int submitted, int late) {
		assertThat(row.get("rank").asInt()).isEqualTo(rank);
		assertThat(row.get("student").get("id").asLong()).isEqualTo(student.getId());
		assertThat(row.get("averageBand").decimalValue()).isEqualByComparingTo(band);
		assertThat(row.get("gradedAssignments").asInt()).isEqualTo(graded);
		assertThat(row.get("submittedParts").asInt()).isEqualTo(submitted);
		assertThat(row.get("lateParts").asInt()).isEqualTo(late);
	}
}
