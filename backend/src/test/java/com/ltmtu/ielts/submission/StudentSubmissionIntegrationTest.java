package com.ltmtu.ielts.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** UC-23, UC-24, SD-06, SD-07, BR-07, BR-08, BR-09, BR-20, NFR-11, NFR-12 — làm bài và nộp bài. */
@RecordApplicationEvents
class StudentSubmissionIntegrationTest extends IntegrationTest {

	private static final String ESSAY = "In recent years, the question of whether university education should be free "
			+ "has become a well-known debate. I don't think it's simple.";

	@Autowired
	private SubmissionRepository submissionRepository;

	@Autowired
	private SubmissionVersionRepository versionRepository;

	@Autowired
	private ApplicationEvents events;

	private User teacher;
	private String teacherToken;
	private User student;
	private String studentToken;

	@BeforeEach
	void setUp() throws Exception {
		teacher = newUser(Role.TEACHER);
		teacherToken = accessToken(teacher);
		student = newUser(Role.STUDENT);
		studentToken = accessToken(student);
	}

	private JsonNode data(ResultActions r) throws Exception {
		return json.readTree(r.andReturn().getResponse().getContentAsString()).get("data");
	}

	/** Bài đã phát hành, giao trực tiếp cho {@code student}; trả về id các phần theo thứ tự. */
	private List<Long> publishedAssignment(Duration dueIn, boolean allowLate, int parts) throws Exception {
		List<Map<String, Object>> partList = new ArrayList<>();
		for (int i = 1; i <= parts; i++) {
			partList.add(Map.of("title", "Task " + i, "taskType", i == 1 ? "WRITING_TASK1" : "WRITING_TASK2", "prompt",
					"<p>Đề " + i + "</p>", "minWords", 150, "criteria", List.of(Map.of("code", "CC", "name", "CC"))));
		}
		Map<String, Object> body = new HashMap<>();
		body.put("title", "Bài luyện viết");
		body.put("dueAt", clock.instant().plus(dueIn).toString());
		body.put("allowLate", allowLate);
		body.put("parts", partList);
		JsonNode a = data(mvc.perform(postJson("/api/teacher/assignments", body)
				.header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))).andExpect(status().isCreated()));
		mvc.perform(postJson("/api/teacher/assignments/" + a.get("id").asLong() + "/publish",
				Map.of("studentIds", Set.of(student.getId()))).header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
				.andExpect(status().isOk());
		List<Long> ids = new ArrayList<>();
		a.get("parts").forEach(p -> ids.add(p.get("id").asLong()));
		return ids;
	}

	private ResultActions saveDraft(long partId, String content) throws Exception {
		return mvc.perform(put("/api/student/parts/" + partId + "/draft").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("content", content)))
				.header(HttpHeaders.AUTHORIZATION, bearer(studentToken)));
	}

	private ResultActions submit(long partId, String content, int expectedVersion) throws Exception {
		return mvc.perform(postJson("/api/student/parts/" + partId + "/submit",
				Map.of("content", content, "expectedVersion", expectedVersion))
				.header(HttpHeaders.AUTHORIZATION, bearer(studentToken)));
	}

	private JsonNode getDraft(long partId) throws Exception {
		return data(mvc.perform(get("/api/student/parts/" + partId + "/draft")
				.header(HttpHeaders.AUTHORIZATION, bearer(studentToken))).andExpect(status().isOk()));
	}

	@Test
	@DisplayName("UC-23: lưu nháp → nạp lại đúng nội dung, thời điểm lưu, số từ; trạng thái DRAFT")
	void draftRoundTrip() throws Exception {
		long partId = publishedAssignment(Duration.ofDays(3), false, 1).getFirst();
		assertThat(getDraft(partId).get("source").asString()).isEqualTo("EMPTY");

		saveDraft(partId, "first attempt").andExpect(status().isOk()).andExpect(jsonPath("$.data.wordCount").value(2));
		saveDraft(partId, ESSAY).andExpect(status().isOk());

		JsonNode d = getDraft(partId);
		assertThat(d.get("source").asString()).isEqualTo("DRAFT");
		assertThat(d.get("content").asString()).isEqualTo(ESSAY);
		assertThat(d.get("wordCount").asInt()).isEqualTo(WordCounter.count(ESSAY));
		assertThat(d.get("status").asString()).isEqualTo("DRAFT");
		assertThat(d.get("savedAt").asString()).isEqualTo(clock.instant().toString());
	}

	@Test
	@DisplayName("UC-24 + BR-09: nộp từng phần độc lập; server đếm từ; version bất biến; nháp bị xoá; phát sự kiện")
	void submitPartsIndependently() throws Exception {
		List<Long> parts = publishedAssignment(Duration.ofDays(3), false, 2);
		saveDraft(parts.get(0), "draft").andExpect(status().isOk());

		submit(parts.get(0), ESSAY, 0)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.versionNo").value(1))
				.andExpect(jsonPath("$.data.late").value(false))
				.andExpect(jsonPath("$.data.wordCount").value(WordCounter.count(ESSAY)));

		Submission s = submissionRepository.findByPartIdAndStudentId(parts.get(0), student.getId()).orElseThrow();
		assertThat(s.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
		assertThat(s.getDraftContent()).isNull();
		assertThat(versionRepository.findBySubmissionIdAndVersionNo(s.getId(), 1).orElseThrow().getContent())
				.isEqualTo(ESSAY);
		// Phần 2 không bị ảnh hưởng.
		assertThat(getDraft(parts.get(1)).get("status").asString()).isEqualTo("NOT_STARTED");

		assertThat(events.stream(SubmissionEvents.Submitted.class)).singleElement().satisfies(e -> {
			assertThat(e.partId()).isEqualTo(parts.get(0));
			assertThat(e.teacherId()).isEqualTo(teacher.getId());
			assertThat(e.versionNo()).isEqualTo(1);
		});

		mvc.perform(get("/api/student/parts/" + parts.get(0) + "/versions").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
				.andExpect(jsonPath("$.data", hasSize(1)));
		mvc.perform(get("/api/student/parts/" + parts.get(0) + "/versions/1").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
				.andExpect(jsonPath("$.data.content").value(ESSAY));
	}

	@Test
	@DisplayName("Đã nộp → không lưu nháp / nộp lại được (409 ALREADY_SUBMITTED); expectedVersion sai → 409 VERSION_CONFLICT")
	void cannotResubmitOrSaveAfterSubmit() throws Exception {
		long partId = publishedAssignment(Duration.ofDays(3), false, 1).getFirst();
		submit(partId, ESSAY, 0).andExpect(status().isCreated());

		submit(partId, ESSAY, 1).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_SUBMITTED"));
		submit(partId, ESSAY, 0).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
		saveDraft(partId, "sửa lén").andExpect(status().isConflict());
	}

	@Test
	@DisplayName("SD-07: 2 request nộp ĐỒNG THỜI cùng expectedVersion → đúng 1 thành công, chỉ 1 version được tạo")
	void concurrentSubmits_createExactlyOneVersion() throws Exception {
		long partId = publishedAssignment(Duration.ofDays(3), false, 1).getFirst();
		saveDraft(partId, "nháp").andExpect(status().isOk()); // để có sẵn dòng submissions → kiểm tra FOR UPDATE

		CountDownLatch start = new CountDownLatch(1);
		Callable<Integer> task = () -> {
			start.await();
			return submit(partId, ESSAY, 0).andReturn().getResponse().getStatus();
		};
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> a = pool.submit(task);
			Future<Integer> b = pool.submit(task);
			start.countDown();
			assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(201, 409);
		} finally {
			pool.shutdown();
		}
		Submission s = submissionRepository.findByPartIdAndStudentId(partId, student.getId()).orElseThrow();
		assertThat(versionRepository.findBySubmissionIdOrderByVersionNoDesc(s.getId())).hasSize(1);
		assertThat(s.getCurrentVersion()).isEqualTo(1);
	}

	@Test
	@DisplayName("Lần nộp đầu tiên đồng thời khi chưa có dòng bài làm → vẫn chỉ 1 version (UNIQUE part + student)")
	void concurrentFirstSubmits() throws Exception {
		long partId = publishedAssignment(Duration.ofDays(3), false, 1).getFirst();
		CountDownLatch start = new CountDownLatch(1);
		Callable<Integer> task = () -> {
			start.await();
			return submit(partId, ESSAY, 0).andReturn().getResponse().getStatus();
		};
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> a = pool.submit(task);
			Future<Integer> b = pool.submit(task);
			start.countDown();
			assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(201, 409);
		} finally {
			pool.shutdown();
		}
	}

	@Test
	@DisplayName("BR-08: quá hạn + không cho nộp muộn → 422 DEADLINE_PASSED (nộp và lưu nháp)")
	void deadlinePassed_notAllowed() throws Exception {
		long partId = publishedAssignment(Duration.ofHours(1), false, 1).getFirst();
		clock.advance(Duration.ofHours(1).plusSeconds(1));
		studentToken = accessToken(student);

		submit(partId, ESSAY, 0).andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("DEADLINE_PASSED"));
		saveDraft(partId, "x").andExpect(status().isUnprocessableContent());
	}

	@Test
	@DisplayName("BR-08 + NFR-12: quá hạn + cho nộp muộn → nhận bài, đánh dấu muộn theo giờ server")
	void lateAllowed_markedLate() throws Exception {
		long partId = publishedAssignment(Duration.ofHours(1), true, 1).getFirst();
		submit(partId, ESSAY, 0).andExpect(jsonPath("$.data.late").value(false));

		long part2 = publishedAssignment(Duration.ofHours(1), true, 1).getFirst();
		clock.advance(Duration.ofHours(2));
		studentToken = accessToken(student);
		submit(part2, ESSAY, 0).andExpect(status().isCreated()).andExpect(jsonPath("$.data.late").value(true));
	}

	@Test
	@DisplayName("BR-07: hạn riêng của phần được ưu tiên hơn hạn của bài")
	void partDueOverridesAssignmentDue() throws Exception {
		List<Long> parts = publishedAssignment(Duration.ofDays(7), false, 1);
		// Lấy id bài tập rồi đặt hạn riêng cho phần sớm hơn.
		long assignmentId = submissionRepositoryAssignmentId(parts.getFirst());
		Instant partDue = clock.instant().plus(Duration.ofHours(1));
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.patch("/api/teacher/assignments/" + assignmentId + "/settings").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("partDueAts", Map.of(String.valueOf(parts.getFirst()), partDue.toString()))))
				.header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))).andExpect(status().isOk());

		clock.advance(Duration.ofHours(2));
		studentToken = accessToken(student);
		submit(parts.getFirst(), ESSAY, 0).andExpect(status().isUnprocessableContent());
	}

	@Autowired
	private com.ltmtu.ielts.assignment.AssignmentPartRepository partRepository;

	private long submissionRepositoryAssignmentId(long partId) {
		return partRepository.findWithAssignment(partId).orElseThrow().getAssignment().getId();
	}

	@Test
	@DisplayName("BR-04: sinh viên không được giao / bài chưa mở → 404; bài đã đóng → 409; giáo viên gọi → 403")
	void accessRules() throws Exception {
		long partId = publishedAssignment(Duration.ofDays(3), false, 1).getFirst();
		String outsider = accessToken(newUser(Role.STUDENT));
		mvc.perform(get("/api/student/parts/" + partId + "/draft").header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
				.andExpect(status().isNotFound());
		mvc.perform(postJson("/api/student/parts/" + partId + "/submit", Map.of("content", ESSAY, "expectedVersion", 0))
				.header(HttpHeaders.AUTHORIZATION, bearer(outsider))).andExpect(status().isNotFound());
		mvc.perform(get("/api/student/parts/" + partId + "/draft").header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
				.andExpect(status().isForbidden());

		long assignmentId = submissionRepositoryAssignmentId(partId);
		mvc.perform(post("/api/teacher/assignments/" + assignmentId + "/close").header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
				.andExpect(status().isOk());
		submit(partId, ESSAY, 0).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
		getDraft(partId); // bài đã đóng vẫn xem được
	}

	@Test
	@DisplayName("Nội dung rỗng → 400; thiếu expectedVersion → 400")
	void validation() throws Exception {
		long partId = publishedAssignment(Duration.ofDays(3), false, 1).getFirst();
		submit(partId, "   ", 0).andExpect(status().isBadRequest());
		mvc.perform(postJson("/api/student/parts/" + partId + "/submit", Map.of("content", ESSAY))
				.header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.expectedVersion").exists());
	}
}
