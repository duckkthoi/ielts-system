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

class StudentActivityIntegrationTest extends StatsTestSupport {

	private User student;
	private String studentToken;
	private long assignmentId;

	@BeforeEach
	void setUpActivity() throws Exception {
		student = newUser(Role.STUDENT);
		studentToken = accessToken(student);
		assignmentId = publish(Duration.ofDays(3), List.of(), List.of(student),
				List.of(part("Task 2", "WRITING_TASK2", "TR"))).id();
	}

	@Test
	@DisplayName("FR-54: sinh viên ghi nhận vào/rời màn và giáo viên chủ bài xem được lịch sử")
	void recordsAndTeacherReadsActivity() throws Exception {
		JsonNode recorded = data(call(postJson("/api/student/assignments/" + assignmentId + "/activity",
				Map.of("type", "TAB_HIDDEN")), studentToken).andExpect(apiStatus(200)));

		assertThat(recorded.get("studentId").asLong()).isEqualTo(student.getId());
		assertThat(recorded.get("studentCode").asString()).isEqualTo(student.getStudentCode());
		assertThat(recorded.get("type").asString()).isEqualTo("TAB_HIDDEN");

		JsonNode feed = data(call(get("/api/teacher/assignments/" + assignmentId + "/activity"), teacher)
				.andExpect(apiStatus(200)));
		assertThat(feed).hasSize(1);
		assertThat(feed.get(0).get("fullName").asString()).isEqualTo(student.getFullName());
		assertThat(feed.get(0).get("type").asString()).isEqualTo("TAB_HIDDEN");
	}

	@Test
	@DisplayName("FR-54: sinh viên không được ghi hoạt động cho bài của người khác")
	void studentCannotRecordActivityForAnotherAssignment() throws Exception {
		User outsider = newUser(Role.STUDENT);
		call(postJson("/api/student/assignments/" + assignmentId + "/activity", Map.of("type", "PAGE_ENTERED")),
				accessToken(outsider)).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("FR-54: giáo viên khác không được xem lịch sử hoạt động của bài")
	void otherTeacherCannotReadActivity() throws Exception {
		User otherTeacher = newUser(Role.TEACHER);
		call(get("/api/teacher/assignments/" + assignmentId + "/activity"), accessToken(otherTeacher))
				.andExpect(apiStatus(404));
	}
}
