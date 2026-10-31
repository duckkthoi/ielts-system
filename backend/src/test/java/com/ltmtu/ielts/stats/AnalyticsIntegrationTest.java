package com.ltmtu.ielts.stats;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** H5: thống kê tổng hợp có bộ lọc — docs/05 mục A.9b. */
class AnalyticsIntegrationTest extends StatsTestSupport {

	private JsonNode overview(String token, MockHttpServletRequestBuilder req) throws Exception {
		return data(call(req, token));
	}

	@Test
	@DisplayName("H5: không có dữ liệu → số 0, tỉ lệ null (không chia cho 0), biểu đồ rỗng, đủ 30 ngày")
	void empty() throws Exception {
		JsonNode o = overview(teacher, get("/api/teacher/analytics"));
		assertThat(o.get("units").get("total").asLong()).isZero();
		assertThat(o.get("units").get("completion").get("rate").isNull()).isTrue();
		assertThat(o.get("kpis").get("onTime").get("current").get("denominator").asLong()).isZero();
		assertThat(o.get("bandHistogram")).isEmpty();
		assertThat(o.get("heatmap")).isEmpty();
		assertThat(o.get("submissionsByDay")).hasSize(30);
		assertThat(o.get("users").isNull()).as("giáo viên không thấy khối người dùng").isTrue();
		assertThat(o.get("period").get("zone").asString()).isEqualTo("Asia/Ho_Chi_Minh");
	}

	@Test
	@DisplayName("H5: đếm theo (sinh viên, phần) — nháp không tính là đã nộp, quá hạn, tỉ lệ có tử / mẫu; band + histogram")
	void unitsAndScores() throws Exception {
		User p = newUser(Role.STUDENT);
		User q = newUser(Role.STUDENT);
		User r = newUser(Role.STUDENT);
		long classId = createClass(teacher, p, q, r);
		Created x = publish(Duration.ofDays(2), List.of(classId), List.of(),
				List.of(part("Task 2", "WRITING_TASK2", "TR")));
		long sp = submit(p, x.part(0));
		grade(sp, Map.of(x.criterion(0, "TR"), 7));
		returnGrade(sp);
		call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/student/parts/" + x.part(0)
				+ "/draft").contentType("application/json").content("{\"content\":\"nháp\",\"draftVersion\":null}"),
				accessToken(q)).andExpect(apiStatus(200));

		JsonNode o = overview(teacher, get("/api/teacher/analytics").param("classId", String.valueOf(classId)));
		JsonNode units = o.get("units");
		assertThat(units.get("total").asLong()).isEqualTo(3);
		assertThat(units.get("returned").asLong()).isEqualTo(1);
		assertThat(units.get("inProgress").asLong()).as("nháp là đang làm, không phải đã nộp").isEqualTo(1);
		assertThat(units.get("notStarted").asLong()).isEqualTo(1);
		assertThat(units.get("completion").get("numerator").asLong()).isEqualTo(1);
		assertThat(units.get("completion").get("denominator").asLong()).isEqualTo(3);
		assertThat(o.get("kpis").get("submissions").get("current").asLong()).isEqualTo(1);
		assertThat(o.get("bandSummary").get("count").asLong()).isEqualTo(1);
		assertThat(o.get("bandHistogram").get(0).get("value").decimalValue()).isEqualByComparingTo("7");
		assertThat(o.get("byAssignment").get(0).get("assignmentId").asLong()).isEqualTo(x.id());

		clock.advance(Duration.ofDays(3));
		reloginTeacher();
		JsonNode later = overview(teacher, get("/api/teacher/analytics").param("classId", String.valueOf(classId)));
		assertThat(later.get("units").get("overdue").asLong()).as("q (nháp) và r (chưa làm) quá hạn").isEqualTo(2);

		JsonNode alerts = data(call(get("/api/teacher/analytics/students").param("classId", String.valueOf(classId)),
				teacher));
		assertThat(alerts).hasSize(2);
		assertThat(alerts.get(0).get("alerts").toString()).contains("Quá hạn");
	}

	@Test
	@DisplayName("H5: giáo viên chỉ xem lớp / bài của mình (lớp người khác 400); không thấy bài của giáo viên khác; admin xem toàn hệ thống")
	void scope() throws Exception {
		User s = newUser(Role.STUDENT);
		long classId = createClass(teacher, s);
		publish(Duration.ofDays(2), List.of(classId), List.of(), List.of(part("Task 2", "WRITING_TASK2", "TR")));

		String other = accessToken(newUser(Role.TEACHER));
		call(get("/api/teacher/analytics").param("classId", String.valueOf(classId)), other).andExpect(apiStatus(400));
		assertThat(overview(other, get("/api/teacher/analytics")).get("units").get("total").asLong()).isZero();

		String admin = accessToken(newUser(Role.ADMIN));
		JsonNode all = overview(admin, get("/api/admin/analytics").param("teacherId",
				String.valueOf(teacherUser.getId())));
		assertThat(all.get("units").get("total").asLong()).isEqualTo(1);
		assertThat(all.get("users").get("byRole").get("TEACHER").asLong()).isPositive();
		call(get("/api/admin/analytics"), teacher).andExpect(apiStatus(403));
		call(get("/api/teacher/analytics").param("from", "2026-10-10").param("to", "2026-10-01"), teacher)
				.andExpect(apiStatus(400));
	}

	@Test
	@DisplayName("H5: xuất CSV có BOM + tên cột tiếng Việt + múi giờ; XLSX tải được")
	void export() throws Exception {
		User s = newUser(Role.STUDENT);
		long classId = createClass(teacher, s);
		publish(Duration.ofDays(2), List.of(classId), List.of(), List.of(part("Task 2", "WRITING_TASK2", "TR")));

		byte[] csv = call(get("/api/teacher/analytics/export").param("format", "CSV"), teacher)
				.andReturn().getResponse().getContentAsByteArray();
		assertThat(csv[0]).isEqualTo((byte) 0xEF);
		String text = new String(csv, StandardCharsets.UTF_8);
		assertThat(text).contains("Mã SV", "Họ tên", "Asia/Ho_Chi_Minh", s.getFullName());

		byte[] xlsx = call(get("/api/teacher/analytics/export"), teacher).andReturn().getResponse()
				.getContentAsByteArray();
		assertThat(xlsx[0]).isEqualTo((byte) 'P');
	}
}
