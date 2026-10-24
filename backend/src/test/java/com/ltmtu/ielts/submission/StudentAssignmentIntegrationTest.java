package com.ltmtu.ielts.submission;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** UC-22 — danh sách bài được giao theo tab và chi tiết bài phía sinh viên. */
class StudentAssignmentIntegrationTest extends IntegrationTest {

	@Autowired
	private SubmissionRepository submissionRepository;

	private String teacherToken;
	private User student;
	private String studentToken;

	@BeforeEach
	void setUp() throws Exception {
		teacherToken = accessToken(newUser(Role.TEACHER));
		student = newUser(Role.STUDENT);
		studentToken = accessToken(student);
	}

	private JsonNode data(ResultActions r) throws Exception {
		return json.readTree(r.andReturn().getResponse().getContentAsString()).get("data");
	}

	/** Phát hành bài có {@code parts} phần cho sinh viên; trả về chi tiết bài (phía giáo viên). */
	private JsonNode publish(String title, Duration dueIn, boolean allowLate, int parts) throws Exception {
		List<Map<String, Object>> partList = new ArrayList<>();
		for (int i = 1; i <= parts; i++) {
			partList.add(Map.of("title", "Task " + i, "taskType", "WRITING_TASK2", "prompt", "<p>Đề " + i + "</p>",
					"minWords", 250, "suggestedMinutes", 40, "criteria", List.of(Map.of("code", "TR", "name", "Task Response",
							"checklist", List.of(Map.of("content", "Trả lời đủ các phần của đề", "kind", "CHECK"),
									Map.of("content", "Chưa trả lời đủ", "kind", "LIMIT", "maxBand", 5))))));
		}
		Map<String, Object> body = new HashMap<>(Map.of("title", title, "dueAt", clock.instant().plus(dueIn).toString(),
				"allowLate", allowLate, "parts", partList));
		JsonNode a = data(mvc.perform(postJson("/api/teacher/assignments", body)
				.header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))).andExpect(apiStatus(201)));
		mvc.perform(postJson("/api/teacher/assignments/" + a.get("id").asLong() + "/publish",
				Map.of("studentIds", Set.of(student.getId()))).header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
				.andExpect(apiStatus(200));
		return a;
	}

	private long partId(JsonNode assignment, int index) {
		return assignment.get("parts").get(index).get("id").asLong();
	}

	private void submit(long partId) throws Exception {
		mvc.perform(postJson("/api/student/parts/" + partId + "/submit", Map.of("content", "My essay text here.", "expectedVersion", 0))
				.header(HttpHeaders.AUTHORIZATION, bearer(studentToken))).andExpect(apiStatus(201));
	}

	private JsonNode list(String tab) throws Exception {
		var req = get("/api/student/assignments").header(HttpHeaders.AUTHORIZATION, bearer(studentToken));
		if (tab != null) {
			req.param("tab", tab);
		}
		return data(mvc.perform(req).andExpect(apiStatus(200)));
	}

	private static List<Long> ids(JsonNode list) {
		List<Long> ids = new ArrayList<>();
		list.get("items").forEach(i -> ids.add(i.get("id").asLong()));
		return ids;
	}

	@Test
	@DisplayName("Quy tắc tab: Cần làm / Sắp hết hạn / Quá hạn / Đã nộp / Cần sửa, kèm số đếm từng tab")
	void tabs() throws Exception {
		long todo = publish("A – còn 3 ngày", Duration.ofDays(3), false, 1).get("id").asLong();
		long dueSoon = publish("B – còn 5 giờ", Duration.ofHours(5), false, 1).get("id").asLong();
		long overdue = publish("C – còn 1 giờ, cho nộp muộn", Duration.ofHours(1), true, 1).get("id").asLong();
		JsonNode doneA = publish("D – đã nộp", Duration.ofDays(3), false, 1);
		submit(partId(doneA, 0));
		JsonNode halfA = publish("E – 2 phần, mới nộp 1", Duration.ofDays(3), false, 2);
		submit(partId(halfA, 0));
		JsonNode revA = publish("F – cần sửa", Duration.ofDays(3), false, 1);
		submit(partId(revA, 0));
		Submission rev = submissionRepository.findByPartIdAndStudentId(partId(revA, 0), student.getId()).orElseThrow();
		rev.setStatus(SubmissionStatus.REVISION_REQUESTED);
		rev.setRevisionDueAt(clock.instant().plus(Duration.ofDays(2)));
		rev.setRevisionNote("Phát triển thêm ý 2");
		submissionRepository.save(rev);
		JsonNode closedA = publish("G – đã đóng, chưa nộp", Duration.ofDays(3), false, 1);
		mvc.perform(post("/api/teacher/assignments/" + closedA.get("id").asLong() + "/close")
				.header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))).andExpect(apiStatus(200));

		clock.advance(Duration.ofHours(2)); // C quá hạn; B còn 3 giờ
		studentToken = accessToken(student);

		JsonNode all = list(null);
		assertThat(all.get("items")).hasSize(7);
		JsonNode counts = all.get("counts");
		assertThat(counts.get("TODO").asInt()).isEqualTo(3);        // A, B, E
		assertThat(counts.get("DUE_SOON").asInt()).isEqualTo(1);    // B
		assertThat(counts.get("OVERDUE").asInt()).isEqualTo(2);     // C, G
		assertThat(counts.get("SUBMITTED").asInt()).isEqualTo(1);   // D
		assertThat(counts.get("REVISION").asInt()).isEqualTo(1);    // F

		assertThat(ids(list("TODO"))).containsExactlyInAnyOrder(todo, dueSoon, halfA.get("id").asLong());
		assertThat(ids(list("DUE_SOON"))).containsExactly(dueSoon);
		assertThat(ids(list("OVERDUE"))).containsExactlyInAnyOrder(overdue, closedA.get("id").asLong());
		assertThat(ids(list("SUBMITTED"))).containsExactly(doneA.get("id").asLong());
		assertThat(ids(list("REVISION"))).containsExactly(revA.get("id").asLong());
		// Lọc theo tab không làm thay đổi số đếm.
		assertThat(list("REVISION").get("counts").get("TODO").asInt()).isEqualTo(3);

		// Sắp xếp theo hạn gần nhất của phần còn phải làm; bài đã nộp hết (không còn hạn) xếp cuối.
		assertThat(ids(all).getFirst()).isEqualTo(overdue);
		assertThat(ids(all).getLast()).isEqualTo(doneA.get("id").asLong());
	}

	@Test
	@DisplayName("Trạng thái từng phần: GRADED hiển thị như SUBMITTED (BR-13); cờ muộn; canSubmit")
	void partStatuses() throws Exception {
		JsonNode a = publish("Bài 2 phần", Duration.ofHours(1), true, 2);
		submit(partId(a, 0));
		Submission s = submissionRepository.findByPartIdAndStudentId(partId(a, 0), student.getId()).orElseThrow();
		s.setStatus(SubmissionStatus.GRADED);
		submissionRepository.save(s);
		clock.advance(Duration.ofHours(2));
		studentToken = accessToken(student);
		submit(partId(a, 1)); // nộp muộn

		JsonNode item = list(null).get("items").get(0);
		JsonNode p1 = item.get("parts").get(0);
		JsonNode p2 = item.get("parts").get(1);
		assertThat(p1.get("status").asString()).isEqualTo("SUBMITTED");
		assertThat(p1.get("late").asBoolean()).isFalse();
		assertThat(p1.get("canSubmit").asBoolean()).isFalse();
		assertThat(p2.get("late").asBoolean()).isTrue();
		assertThat(p2.get("currentVersion").asInt()).isEqualTo(1);
	}

	@Test
	@DisplayName("Chi tiết bài: đề, số từ, hạn áp dụng (hạn sửa khi Cần sửa), lời nhắn sửa, rubric + checklist, giờ server")
	void detail() throws Exception {
		JsonNode a = publish("Chi tiết", Duration.ofDays(3), false, 1);
		long id = a.get("id").asLong();
		submit(partId(a, 0));
		Instant revisionDue = clock.instant().plus(Duration.ofDays(5));
		Submission s = submissionRepository.findByPartIdAndStudentId(partId(a, 0), student.getId()).orElseThrow();
		s.setStatus(SubmissionStatus.REVISION_REQUESTED);
		s.setRevisionDueAt(revisionDue);
		s.setRevisionNote("Bổ sung ví dụ ở đoạn 2");
		submissionRepository.save(s);

		mvc.perform(get("/api/student/assignments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.title").value("Chi tiết"))
				.andExpect(jsonPath("$.data.serverTime").value(clock.instant().toString()))
				.andExpect(jsonPath("$.data.tabs[0]").value("REVISION"))
				.andExpect(jsonPath("$.data.parts[0].prompt").value("<p>Đề 1</p>"))
				.andExpect(jsonPath("$.data.parts[0].minWords").value(250))
				.andExpect(jsonPath("$.data.parts[0].status").value("REVISION_REQUESTED"))
				.andExpect(jsonPath("$.data.parts[0].effectiveDueAt").value(revisionDue.toString()))
				.andExpect(jsonPath("$.data.parts[0].revisionNote").value("Bổ sung ví dụ ở đoạn 2"))
				.andExpect(jsonPath("$.data.parts[0].canSubmit").value(true))
				.andExpect(jsonPath("$.data.parts[0].criteria[0].code").value("TR"))
				.andExpect(jsonPath("$.data.parts[0].criteria[0].checklist[1].kind").value("LIMIT"))
				.andExpect(jsonPath("$.data.parts[0].criteria[0].checklist[1].maxBand").value(5));
	}

	@Test
	@DisplayName("BR-04: bài không được giao không có trong danh sách và xem chi tiết → 404")
	void notAssigned() throws Exception {
		long id = publish("Riêng", Duration.ofDays(3), false, 1).get("id").asLong();
		String outsider = accessToken(newUser(Role.STUDENT));
		JsonNode outsiderList = data(mvc.perform(get("/api/student/assignments").header(HttpHeaders.AUTHORIZATION, bearer(outsider))));
		assertThat(ids(outsiderList)).doesNotContain(id);
		mvc.perform(get("/api/student/assignments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
				.andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("Tab không hợp lệ → 400")
	void invalidTab() throws Exception {
		mvc.perform(get("/api/student/assignments").param("tab", "KHAC").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
				.andExpect(apiStatus(400));
	}
}
