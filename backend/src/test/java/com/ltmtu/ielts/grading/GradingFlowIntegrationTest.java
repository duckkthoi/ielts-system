package com.ltmtu.ielts.grading;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static com.ltmtu.ielts.support.ApiStatus.apiStatusOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

import com.ltmtu.ielts.user.Role;

import tools.jackson.databind.JsonNode;

/** UC-17 hàng đợi chấm, UC-18 trả bài / yêu cầu sửa (BR-10, BR-13), sinh viên nộp lại. */
@RecordApplicationEvents
class GradingFlowIntegrationTest extends GradingTestSupport {

	@Autowired
	private ApplicationEvents events;

	private void gradeFully(Fixture f, int versionNo) throws Exception {
		saveGrade(f.submissionId(), grade(versionNo, scores(f, 6, 7, 6.5, 6), List.of(), List.of()))
				.andExpect(apiStatus(200));
	}

	private List<Long> queueIds(String token, String... params) throws Exception {
		var req = get("/api/teacher/submissions");
		for (int i = 0; i < params.length; i += 2) {
			req.param(params[i], params[i + 1]);
		}
		List<Long> ids = new ArrayList<>();
		data(call(req, token).andExpect(apiStatus(200))).forEach(n -> ids.add(n.get("submissionId").asLong()));
		return ids;
	}

	private ResultActions requestRevision(long submissionId, Instant dueAt)
			throws Exception {
		return call(postJson("/api/teacher/submissions/" + submissionId + "/request-revision",
				Map.of("note", "  Phát triển thêm ý 2  ", "dueAt", dueAt.toString())), teacher);
	}

	@Test
	@DisplayName("Hàng đợi: mặc định bài chờ chấm, lọc theo bài / phần / lớp / từ khoá / trạng thái; chỉ bài của tôi")
	void queue() throws Exception {
		Fixture f = submitted();
		JsonNode row = data(call(get("/api/teacher/submissions").param("assignmentId", String.valueOf(f.assignmentId())),
				teacher).andExpect(apiStatus(200))).get(0);
		assertThat(row.get("submissionId").asLong()).isEqualTo(f.submissionId());
		assertThat(row.get("status").asString()).isEqualTo("SUBMITTED");
		assertThat(row.get("student").get("studentCode").asString()).isEqualTo(student.getStudentCode());
		assertThat(row.get("wordCount").asInt()).isEqualTo(27);
		assertThat(row.get("late").asBoolean()).isFalse();
		assertThat(row.get("gradeStatus").isNull()).isTrue();

		assertThat(queueIds(teacher, "partId", String.valueOf(f.partId()))).containsExactly(f.submissionId());
		assertThat(queueIds(teacher, "keyword", student.getStudentCode().toLowerCase())).containsExactly(f.submissionId());
		assertThat(queueIds(accessToken(newUser(Role.TEACHER)))).doesNotContain(f.submissionId());

		// Lọc theo lớp: sinh viên chưa vào lớp → không có; vào lớp rồi → có.
		String code = "L" + UUID.randomUUID().toString().substring(0, 8);
		long classId = data(call(postJson("/api/teacher/classes", Map.of("code", code, "name", "Lớp chấm")), teacher)
				.andExpect(apiStatus(201))).get("id").asLong();
		assertThat(queueIds(teacher, "classId", String.valueOf(classId))).isEmpty();
		call(postJson("/api/teacher/classes/" + classId + "/members", Map.of("studentCodes", List.of(student.getStudentCode()))),
				teacher).andExpect(apiStatus(200));
		assertThat(queueIds(teacher, "classId", String.valueOf(classId))).containsExactly(f.submissionId());

		// Đã lưu chấm → rời hàng "chờ chấm", sang GRADED kèm band.
		gradeFully(f, 1);
		assertThat(queueIds(teacher, "partId", String.valueOf(f.partId()))).isEmpty();
		call(get("/api/teacher/submissions").param("status", "GRADED").param("partId", String.valueOf(f.partId())), teacher)
				.andExpect(jsonPath("$.data[0].gradeStatus").value("DRAFT"))
				.andExpect(jsonPath("$.data[0].band").value(6.5))
				.andExpect(jsonPath("$.totalRecord").value(1));

		call(get("/api/teacher/submissions").param("status", "DRAFT"), teacher).andExpect(apiStatus(400));
		String otherTeacher = accessToken(newUser(Role.TEACHER));
		call(get("/api/teacher/submissions").param("classId", String.valueOf(classId)), otherTeacher)
				.andExpect(apiStatus(400));
	}

	@Test
	@DisplayName("UC-18 / BR-13: trả bài → kết quả RETURNED, bài nộp RETURNED, có sự kiện; sau đó không sửa / trả lại được")
	void returnGrade() throws Exception {
		Fixture f = submitted();
		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher)
				.andExpect(apiStatus(409))
				.andExpect(jsonPath("$.errorMessage").value("Bài này chưa được chấm"));
		gradeFully(f, 1);

		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher)
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.submission.status").value("RETURNED"))
				.andExpect(jsonPath("$.data.grade.status").value("RETURNED"))
				.andExpect(jsonPath("$.data.grade.returnedAt").value(clock.instant().toString()))
				.andExpect(jsonPath("$.data.editable").value(false))
				.andExpect(jsonPath("$.data.canReturn").value(false));
		assertThat(events.stream(GradingEvents.Returned.class)).singleElement().satisfies(e -> {
			assertThat(e.submissionId()).isEqualTo(f.submissionId());
			assertThat(e.studentId()).isEqualTo(student.getId());
			assertThat(e.band()).isEqualByComparingTo("6.5");
		});

		saveGrade(f.submissionId(), grade(1, scores(f, 9, 9, 9, 9), List.of(), List.of())).andExpect(apiStatus(409));
		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher).andExpect(apiStatus(409));
		requestRevision(f.submissionId(), clock.instant().plus(Duration.ofDays(2))).andExpect(apiStatus(409));
	}

	@Test
	@DisplayName("UC-18 A1–A3 + BR-07: yêu cầu sửa → sinh viên nạp lại bài cũ, nộp v2 theo hạn sửa (không muộn); giáo viên chấm v2, v1 chỉ xem")
	void revisionCycle() throws Exception {
		Fixture f = submitted(ESSAY, Duration.ofDays(1), 2);
		gradeFully(f, 1);
		Instant revisionDue = clock.instant().plus(Duration.ofDays(3));
		requestRevision(f.submissionId(), revisionDue)
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.submission.status").value("REVISION_REQUESTED"))
				.andExpect(jsonPath("$.data.submission.revisionNote").value("Phát triển thêm ý 2"))
				.andExpect(jsonPath("$.data.submission.revisionDueAt").value(revisionDue.toString()))
				.andExpect(jsonPath("$.data.grade.status").value("RETURNED"));
		assertThat(events.stream(GradingEvents.RevisionRequested.class)).singleElement()
				.satisfies(e -> assertThat(e.revisionDueAt()).isEqualTo(revisionDue));
		assertThat(queueIds(teacher, "status", "REVISION_REQUESTED")).contains(f.submissionId());

		// Quá hạn bài gốc nhưng còn trong hạn sửa.
		clock.advance(Duration.ofDays(2));
		studentToken = accessToken(student);
		call(get("/api/student/parts/" + f.partId() + "/draft"), studentToken)
				.andExpect(jsonPath("$.data.source").value("LAST_VERSION"))
				.andExpect(jsonPath("$.data.content").value(ESSAY));
		call(postJson("/api/student/parts/" + f.partId() + "/submit", Map.of("content", ESSAY + " Bản sửa.", "expectedVersion", 1)),
				studentToken)
				.andExpect(apiStatus(201))
				.andExpect(jsonPath("$.data.versionNo").value(2))
				.andExpect(jsonPath("$.data.late").value(false));

		teacher = accessToken(teacherUser);
		assertThat(queueIds(teacher, "partId", String.valueOf(f.partId()))).containsExactly(f.submissionId());
		call(get("/api/teacher/submissions/" + f.submissionId()), teacher)
				.andExpect(jsonPath("$.data.version.versionNo").value(2))
				.andExpect(jsonPath("$.data.submission.revisionsUsed").value(1))
				.andExpect(jsonPath("$.data.grade").isEmpty())
				.andExpect(jsonPath("$.data.versions[1].versionNo").value(1))
				.andExpect(jsonPath("$.data.versions[1].gradeStatus").value("RETURNED"))
				.andExpect(jsonPath("$.data.versions[1].band").value(6.5));
		call(get("/api/teacher/submissions/" + f.submissionId()).param("versionNo", "1"), teacher)
				.andExpect(jsonPath("$.data.version.content").value(ESSAY))
				.andExpect(jsonPath("$.data.grade.band").value(6.5))
				.andExpect(jsonPath("$.data.editable").value(false));
		call(get("/api/teacher/submissions/" + f.submissionId() + "/versions"), teacher)
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].versionNo").value(2));
		saveGrade(f.submissionId(), grade(1, scores(f, 7, 7, 7, 7), List.of(), List.of())).andExpect(apiStatus(409));
		gradeFully(f, 2);
		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher).andExpect(apiStatus(200));
	}

	@Test
	@DisplayName("BR-10: hết lượt sửa → 422 REVISION_LIMIT, chỉ trả bài được; canRequestRevision = false")
	void revisionLimit() throws Exception {
		Fixture f = submitted(ESSAY, Duration.ofDays(3), 0);
		gradeFully(f, 1);
		call(get("/api/teacher/submissions/" + f.submissionId()), teacher)
				.andExpect(jsonPath("$.data.canReturn").value(true))
				.andExpect(jsonPath("$.data.canRequestRevision").value(false));
		requestRevision(f.submissionId(), clock.instant().plus(Duration.ofDays(2)))
				.andExpect(apiStatus(422))
				.andExpect(jsonPath("$.errorCode").value("REVISION_LIMIT"));
		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher).andExpect(apiStatus(200));
	}

	@Test
	@DisplayName("UC-18 E2: hạn sửa ở quá khứ / thiếu hạn → 400; bài tập đã đóng → 409")
	void revisionPreconditions() throws Exception {
		Fixture f = submitted();
		gradeFully(f, 1);
		requestRevision(f.submissionId(), clock.instant().minusSeconds(1)).andExpect(apiStatus(400));
		call(postJson("/api/teacher/submissions/" + f.submissionId() + "/request-revision", Map.of("note", "x")), teacher)
				.andExpect(apiStatus(400));
		call(post("/api/teacher/assignments/" + f.assignmentId() + "/close"), teacher).andExpect(apiStatus(200));
		call(get("/api/teacher/submissions/" + f.submissionId()), teacher)
				.andExpect(jsonPath("$.data.canRequestRevision").value(false));
		requestRevision(f.submissionId(), clock.instant().plus(Duration.ofDays(2)))
				.andExpect(apiStatus(409));
		// Bài đã đóng vẫn chấm và trả được (docs/06 mục 1).
		call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher).andExpect(apiStatus(200));
	}

	@Test
	@DisplayName("SD-08: 2 request \"Trả bài\" đồng thời → đúng 1 thành công, chỉ 1 sự kiện (khoá dòng bài nộp)")
	void concurrentReturn() throws Exception {
		Fixture f = submitted();
		gradeFully(f, 1);
		CountDownLatch start = new CountDownLatch(1);
		Callable<Integer> task = () -> {
			start.await();
			return apiStatusOf(
					call(post("/api/teacher/submissions/" + f.submissionId() + "/return"), teacher).andReturn());
		};
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> a = pool.submit(task);
			Future<Integer> b = pool.submit(task);
			start.countDown();
			assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(200, 409);
		} finally {
			pool.shutdown();
		}
	}
}
