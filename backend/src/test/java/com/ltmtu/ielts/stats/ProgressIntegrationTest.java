package com.ltmtu.ielts.stats;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/**
 * Ma trận tiến độ (UC-16). Dữ liệu dựng sẵn: bài 2 phần (Task 1, Task 2), hạn sau 3 ngày, cho nộp muộn; giao cho
 * lớp (B, C) và trực tiếp (A, D). Sau đó tua giờ qua hạn 1 ngày:
 * <ul>
 * <li>A: Task 1 đã trả (band 7.0), Task 2 đã chấm nhưng chưa trả;</li>
 * <li>B: Task 1 "Cần sửa" (hạn sửa còn xa), Task 2 đang nháp → quá hạn;</li>
 * <li>C: chưa làm gì → cả 2 phần quá hạn;</li>
 * <li>D: Task 1 "Chấm 0" đã trả, Task 2 nộp muộn.</li>
 * </ul>
 */
class ProgressIntegrationTest extends StatsTestSupport {

	private User a;
	private User b;
	private User c;
	private User d;
	private long classId;
	private long assignmentId;
	private long task1;
	private long task2;

	@BeforeEach
	void setUp() throws Exception {
		a = newUser(Role.STUDENT);
		b = newUser(Role.STUDENT);
		c = newUser(Role.STUDENT);
		d = newUser(Role.STUDENT);
		classId = createClass(teacher, b, c);
		Created created = publish(Duration.ofDays(3), List.of(classId), List.of(a, d),
				List.of(part("Task 1", "WRITING_TASK1", "TA"), part("Task 2", "WRITING_TASK2", "TR")));
		assignmentId = created.id();
		task1 = created.part(0);
		task2 = created.part(1);
		long ta = created.criterion(0, "TA");
		long tr = created.criterion(1, "TR");

		// A: Task 1 trả bài 7.0, Task 2 chấm nháp 6.0.
		long a1 = submit(a, task1);
		grade(a1, Map.of(ta, 7));
		returnGrade(a1);
		grade(submit(a, task2), Map.of(tr, 6));

		// B: Task 1 bị yêu cầu sửa (hạn sửa còn xa), Task 2 mới lưu nháp.
		long b1 = submit(b, task1);
		grade(b1, Map.of(ta, 5));
		call(postJson("/api/teacher/submissions/" + b1 + "/request-revision",
				Map.of("note", "Thêm overview", "dueAt", clock.instant().plus(Duration.ofDays(10)).toString())), teacher)
				.andExpect(apiStatus(200));
		call(put("/api/student/parts/" + task2 + "/draft").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("content", "Nháp dở"))), accessToken(b))
				.andExpect(apiStatus(200));

		// D: Task 1 "Chấm 0" rồi trả.
		long d1 = submit(d, task1);
		grade(d1, 1, Map.of(), "NOT_ENGLISH", List.of());
		returnGrade(d1);

		// Qua hạn 1 ngày: D nộp Task 2 muộn.
		clock.advance(Duration.ofDays(4));
		reloginTeacher();
		submit(d, task2);
	}

	// ─── Test ───────────────────────────────────────────────────────────────

	@Test
	@DisplayName("UC-16: ma trận đủ sinh viên × phần — trạng thái, muộn (BR-08), quá hạn theo hạn áp dụng (BR-07), band chỉ khi đã trả (BR-13)")
	void matrix() throws Exception {
		JsonNode m = progress(Map.of());

		assertThat(m.get("parts")).hasSize(2);
		assertThat(m.get("parts").get(0).get("id").asLong()).isEqualTo(task1);
		assertThat(m.get("parts").get(1).get("taskType").asString()).isEqualTo("WRITING_TASK2");
		assertThat(m.get("rows")).hasSize(4);

		JsonNode ra = row(m, a);
		assertCell(ra.get(0), "RETURNED", false, false, "7.0");
		assertThat(ra.get(0).get("submissionId").isNull()).isFalse();
		assertThat(ra.get(0).get("currentVersion").asInt()).isEqualTo(1);
		assertCell(ra.get(1), "GRADED", false, false, null);

		JsonNode rb = row(m, b);
		assertCell(rb.get(0), "REVISION_REQUESTED", false, false, "5.0");
		assertCell(rb.get(1), "DRAFT", false, true, null);
		assertThat(rb.get(1).get("currentVersion").asInt()).isZero();

		JsonNode rc = row(m, c);
		assertCell(rc.get(0), "NOT_STARTED", false, true, null);
		assertThat(rc.get(0).get("submissionId").isNull()).isTrue();
		assertCell(rc.get(1), "NOT_STARTED", false, true, null);

		JsonNode rd = row(m, d);
		assertCell(rd.get(0), "RETURNED", false, false, "0.0");
		assertCell(rd.get(1), "SUBMITTED", true, false, null);

		JsonNode s = m.get("summary");
		assertThat(s.get("students").asInt()).isEqualTo(4);
		assertThat(s.get("completedCount").asInt()).isEqualTo(2);
		assertThat(s.get("completedRate").asDouble()).isEqualTo(0.5);
		assertThat(s.get("lateCount").asInt()).isEqualTo(1);
		assertThat(s.get("overdueCount").asInt()).isEqualTo(2);
	}

	@Test
	@DisplayName("UC-16: lọc theo lớp (tóm tắt theo lớp) và theo trạng thái (tóm tắt không đổi)")
	void filters() throws Exception {
		JsonNode byClass = progress(Map.of("classId", String.valueOf(classId)));
		assertThat(ids(byClass)).containsExactlyInAnyOrder(b.getId(), c.getId());
		assertThat(byClass.get("summary").get("students").asInt()).isEqualTo(2);
		assertThat(byClass.get("summary").get("completedCount").asInt()).isZero();

		JsonNode drafts = progress(Map.of("status", "DRAFT"));
		assertThat(ids(drafts)).containsExactly(b.getId());
		assertThat(drafts.get("summary").get("students").asInt()).isEqualTo(4);

		JsonNode notStarted = progress(Map.of("status", "NOT_STARTED"));
		assertThat(ids(notStarted)).containsExactly(c.getId());
	}

	@Test
	@DisplayName("Rời lớp: người đã có bài làm vẫn ở trong ma trận, người chưa làm gì thì không")
	void leftClass() throws Exception {
		call(delete("/api/teacher/classes/" + classId + "/members/" + b.getId()), teacher).andExpect(apiStatus(200));
		call(delete("/api/teacher/classes/" + classId + "/members/" + c.getId()), teacher).andExpect(apiStatus(200));

		assertThat(ids(progress(Map.of()))).containsExactlyInAnyOrder(a.getId(), b.getId(), d.getId());
	}

	@Test
	@DisplayName("Phân quyền: bài của giáo viên khác → 404, lớp của giáo viên khác → 400, sinh viên → 403, trạng thái sai → 400")
	void access() throws Exception {
		String other = accessToken(newUser(Role.TEACHER));
		call(get(url()), other).andExpect(apiStatus(404));

		long otherClass = createClass(other);
		call(get(url()).param("classId", String.valueOf(otherClass)), teacher).andExpect(apiStatus(400));

		call(get(url()), accessToken(a)).andExpect(apiStatus(403));
		call(get(url()).param("status", "LAZY"), teacher).andExpect(apiStatus(400));
	}

	// ─── Hỗ trợ ─────────────────────────────────────────────────────────────

	private String url() {
		return "/api/teacher/assignments/" + assignmentId + "/progress";
	}

	private JsonNode progress(Map<String, String> params) throws Exception {
		MockHttpServletRequestBuilder req = get(url());
		params.forEach(req::param);
		return data(call(req, teacher).andExpect(apiStatus(200)));
	}

	private static JsonNode row(JsonNode matrix, User student) {
		for (JsonNode r : matrix.get("rows")) {
			if (r.get("student").get("id").asLong() == student.getId()) {
				assertThat(r.get("student").get("studentCode").asString()).isEqualTo(student.getStudentCode());
				return r.get("cells");
			}
		}
		throw new AssertionError("Thiếu hàng của sinh viên " + student.getId());
	}

	private static List<Long> ids(JsonNode matrix) {
		List<Long> ids = new ArrayList<>();
		matrix.get("rows").forEach(r -> ids.add(r.get("student").get("id").asLong()));
		return ids;
	}

	private static void assertCell(JsonNode cell, String status, boolean late, boolean overdue, String score) {
		assertThat(cell.get("status").asString()).isEqualTo(status);
		assertThat(cell.get("late").asBoolean()).as("late").isEqualTo(late);
		assertThat(cell.get("overdue").asBoolean()).as("overdue").isEqualTo(overdue);
		if (score == null) {
			assertThat(cell.get("score").isNull()).as("score").isTrue();
		} else {
			assertThat(cell.get("score").decimalValue()).isEqualByComparingTo(score);
		}
	}
}
