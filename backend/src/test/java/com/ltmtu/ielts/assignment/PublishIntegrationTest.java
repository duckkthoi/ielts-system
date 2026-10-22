package com.ltmtu.ielts.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** UC-14, UC-15, BR-04, BR-05 — phát hành, giao bài, vòng đời, bài sinh viên được thấy, upload ảnh. */
@RecordApplicationEvents
class PublishIntegrationTest extends IntegrationTest {

	@Autowired
	private AssignmentRepository assignmentRepository;

	@Autowired
	private ApplicationEvents events;

	private User teacher;
	private String token;

	@BeforeEach
	void login() throws Exception {
		teacher = newUser(Role.TEACHER);
		token = accessToken(teacher);
	}

	private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req) {
		return req.header(HttpHeaders.AUTHORIZATION, bearer(token));
	}

	private JsonNode data(ResultActions r) throws Exception {
		return json.readTree(r.andReturn().getResponse().getContentAsString()).get("data");
	}

	/** Lớp của giáo viên hiện tại với các thành viên cho trước. */
	private long classWith(User... members) throws Exception {
		String code = "PUB-" + UUID.randomUUID().toString().substring(0, 6);
		long id = data(mvc.perform(as(postJson("/api/teacher/classes", Map.of("code", code, "name", "Lớp " + code))))
				.andExpect(status().isCreated())).get("id").asLong();
		if (members.length > 0) {
			List<String> codes = new ArrayList<>();
			for (User m : members) {
				codes.add(m.getStudentCode());
			}
			mvc.perform(as(postJson("/api/teacher/classes/" + id + "/members", Map.of("studentCodes", codes))))
					.andExpect(status().isOk());
		}
		return id;
	}

	/** Bài nháp đủ điều kiện phát hành (1 phần, 1 tiêu chí có mục LIMIT, hạn sau 7 ngày). */
	private long readyDraft(Instant openAt) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("title", "Tuần 5 – Writing practice");
		body.put("dueAt", clock.instant().plus(Duration.ofDays(7)).toString());
		if (openAt != null) {
			body.put("openAt", openAt.toString());
		}
		Map<String, Object> limitItem = Map.of("content", "Không chia đoạn", "kind", "LIMIT", "maxBand", 5);
		Map<String, Object> criterion = Map.of("code", "CC", "name", "Coherence and Cohesion", "checklist",
				List.of(limitItem));
		body.put("parts", List.of(Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>Đề</p>",
				"criteria", List.of(criterion))));
		return data(mvc.perform(as(postJson("/api/teacher/assignments", body))).andExpect(status().isCreated()))
				.get("id").asLong();
	}

	private ResultActions publish(long id, Set<Long> classIds, Set<Long> studentIds) throws Exception {
		return mvc.perform(as(postJson("/api/teacher/assignments/" + id + "/publish",
				Map.of("classIds", classIds, "studentIds", studentIds))));
	}

	@Test
	@DisplayName("UC-14: giao cho lớp + cá nhân trùng nhau → người nhận đã loại trùng; bài PUBLISHED; phát sự kiện")
	void publish_dedupesRecipients_andPublishesEvent() throws Exception {
		User s1 = newUser(Role.STUDENT);
		User s2 = newUser(Role.STUDENT);
		User extra = newUser(Role.STUDENT);
		long classId = classWith(s1, s2);
		long id = readyDraft(null);

		publish(id, Set.of(classId), Set.of(s1.getId(), extra.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recipientCount").value(3));

		Assignment a = assignmentRepository.findById(id).orElseThrow();
		assertThat(a.getStatus()).isEqualTo(AssignmentEnums.AssignmentStatus.PUBLISHED);
		assertThat(a.getPublishedAt()).isNotNull();
		assertThat(events.stream(AssignmentEvents.Published.class))
				.singleElement()
				.satisfies(e -> {
					assertThat(e.assignmentId()).isEqualTo(id);
					assertThat(e.recipientIds()).containsExactlyInAnyOrder(s1.getId(), s2.getId(), extra.getId());
					assertThat(e.opensNow()).isTrue();
				});

		mvc.perform(as(get("/api/teacher/assignments/" + id + "/targets")))
				.andExpect(jsonPath("$.data.classes", hasSize(1)))
				.andExpect(jsonPath("$.data.classes[0].memberCount").value(2))
				.andExpect(jsonPath("$.data.students", hasSize(2)))
				.andExpect(jsonPath("$.data.recipientCount").value(3));

		publish(id, Set.of(classId), Set.of()).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Chưa đủ điều kiện → 400 NOT_READY liệt kê mọi vấn đề; bài vẫn là nháp")
	void readiness() throws Exception {
		Map<String, Object> body = Map.of("title", "Thiếu đủ thứ", "parts",
				List.of(Map.of("title", "P1", "taskType", "CUSTOM", "prompt", "<p>x</p>")));
		long id = data(mvc.perform(as(postJson("/api/teacher/assignments", body)))).get("id").asLong();

		publish(id, Set.of(), Set.of(newUser(Role.STUDENT).getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("NOT_READY"))
				.andExpect(jsonPath("$.message", containsString("chưa có tiêu chí")))
				.andExpect(jsonPath("$.message", containsString("chưa đặt hạn nộp")));
		assertThat(assignmentRepository.findById(id).orElseThrow().isDraft()).isTrue();
	}

	@Test
	@DisplayName("Không có người nhận (lớp rỗng) → 400, và các target vừa thêm bị huỷ (rollback)")
	void noRecipients_rollsBack() throws Exception {
		long emptyClass = classWith();
		long id = readyDraft(null);

		publish(id, Set.of(emptyClass), Set.of())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("chưa có sinh viên nào")));
		mvc.perform(as(get("/api/teacher/assignments/" + id + "/targets")))
				.andExpect(jsonPath("$.data.classes", hasSize(0)));
		assertThat(events.stream(AssignmentEvents.Published.class)).isEmpty();
	}

	@Test
	@DisplayName("Giao cho lớp của giáo viên khác / tài khoản không phải sinh viên → 400")
	void invalidTargets() throws Exception {
		long id = readyDraft(null);
		String otherToken = accessToken(newUser(Role.TEACHER));
		long othersClass = data(mvc.perform(postJson("/api/teacher/classes",
				Map.of("code", "OTH-" + UUID.randomUUID().toString().substring(0, 6), "name", "X"))
				.header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))).get("id").asLong();

		publish(id, Set.of(othersClass), Set.of()).andExpect(status().isBadRequest());
		publish(id, Set.of(), Set.of(teacher.getId())).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("BR-04 + BR-05: sinh viên chỉ thấy bài đã phát hành, đã tới giờ mở, được giao trực tiếp hoặc qua lớp")
	void visibleForStudent() throws Exception {
		User member = newUser(Role.STUDENT);
		User direct = newUser(Role.STUDENT);
		User outsider = newUser(Role.STUDENT);
		User lateJoiner = newUser(Role.STUDENT);
		long classId = classWith(member);

		long draft = readyDraft(null);
		long openNow = readyDraft(null);
		publish(openNow, Set.of(classId), Set.of(direct.getId())).andExpect(status().isOk());
		long opensLater = readyDraft(clock.instant().plus(Duration.ofDays(1)));
		publish(opensLater, Set.of(classId), Set.of()).andExpect(status().isOk());

		assertThat(visibleIds(member)).contains(openNow).doesNotContain(draft, opensLater);
		assertThat(visibleIds(direct)).contains(openNow);
		assertThat(visibleIds(outsider)).doesNotContain(openNow, opensLater);
		assertThat(assignmentRepository.findVisibleForStudent(openNow, outsider.getId(), clock.instant())).isEmpty();

		// Thêm vào lớp sau khi phát hành vẫn thấy bài (giao theo lớp tính tại thời điểm truy vấn).
		mvc.perform(as(postJson("/api/teacher/classes/" + classId + "/members",
				Map.of("studentCodes", List.of(lateJoiner.getStudentCode())))));
		assertThat(visibleIds(lateJoiner)).contains(openNow);

		// Tới giờ mở thì thấy; bài đã đóng vẫn thấy (để xem kết quả).
		clock.advance(Duration.ofDays(1).plusMinutes(1));
		assertThat(visibleIds(member)).contains(openNow, opensLater);
		token = accessToken(teacher); // access token cũ đã quá 30 phút sau khi tua đồng hồ
		mvc.perform(as(post("/api/teacher/assignments/" + openNow + "/close"))).andExpect(status().isOk());
		assertThat(visibleIds(member)).contains(openNow);
	}

	private List<Long> visibleIds(User student) {
		return assignmentRepository.findVisibleForStudent(student.getId(), clock.instant()).stream()
				.map(Assignment::getId).toList();
	}

	@Test
	@DisplayName("Mở trong tương lai → sự kiện ghi opensNow = false (thông báo do job gửi lúc mở — SD-05)")
	void futureOpen_eventNotOpenNow() throws Exception {
		long id = readyDraft(clock.instant().plus(Duration.ofHours(3)));
		publish(id, Set.of(), Set.of(newUser(Role.STUDENT).getId())).andExpect(status().isOk());
		assertThat(events.stream(AssignmentEvents.Published.class)).singleElement()
				.satisfies(e -> assertThat(e.opensNow()).isFalse());
	}

	@Test
	@DisplayName("UC-14 A2: giao thêm → chỉ người MỚI vào sự kiện; bài nháp / đã đóng → 409")
	void addMoreTargets() throws Exception {
		User s1 = newUser(Role.STUDENT);
		User s2 = newUser(Role.STUDENT);
		long id = readyDraft(null);
		mvc.perform(as(put("/api/teacher/assignments/" + id + "/targets").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("studentIds", List.of(s1.getId()))))))
				.andExpect(status().isConflict());
		publish(id, Set.of(), Set.of(s1.getId())).andExpect(status().isOk());

		mvc.perform(as(put("/api/teacher/assignments/" + id + "/targets").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("studentIds", List.of(s1.getId(), s2.getId()))))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.newRecipients").value(1))
				.andExpect(jsonPath("$.data.recipientCount").value(2));
		assertThat(events.stream(AssignmentEvents.RecipientsAdded.class)).singleElement()
				.satisfies(e -> assertThat(e.newRecipientIds()).containsExactly(s2.getId()));

		mvc.perform(as(post("/api/teacher/assignments/" + id + "/close")));
		mvc.perform(as(put("/api/teacher/assignments/" + id + "/targets").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("studentIds", List.of(s2.getId()))))))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Đóng / mở lại đúng trạng thái; sai trạng thái → 409. Lớp hiển thị số bài đang mở")
	void closeReopen() throws Exception {
		User s = newUser(Role.STUDENT);
		long classId = classWith(s);
		long id = readyDraft(null);
		mvc.perform(as(post("/api/teacher/assignments/" + id + "/close"))).andExpect(status().isConflict());
		publish(id, Set.of(classId), Set.of()).andExpect(status().isOk());
		mvc.perform(as(get("/api/teacher/classes/" + classId))).andExpect(jsonPath("$.data.openAssignmentCount").value(1));

		mvc.perform(as(post("/api/teacher/assignments/" + id + "/close")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CLOSED"));
		mvc.perform(as(get("/api/teacher/classes/" + classId))).andExpect(jsonPath("$.data.openAssignmentCount").value(0));
		mvc.perform(as(post("/api/teacher/assignments/" + id + "/close"))).andExpect(status().isConflict());
		mvc.perform(as(post("/api/teacher/assignments/" + id + "/reopen")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PUBLISHED"));
		mvc.perform(as(post("/api/teacher/assignments/" + id + "/reopen"))).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("UC-15: nhân bản → bài nháp mới, đủ phần + rubric (kể cả mục LIMIT), không chép người nhận và mốc thời gian")
	void duplicate() throws Exception {
		long id = readyDraft(null);
		publish(id, Set.of(), Set.of(newUser(Role.STUDENT).getId())).andExpect(status().isOk());

		JsonNode copy = data(mvc.perform(as(post("/api/teacher/assignments/" + id + "/duplicate")))
				.andExpect(status().isCreated()));
		assertThat(copy.get("id").asLong()).isNotEqualTo(id);
		assertThat(copy.get("status").asString()).isEqualTo("DRAFT");
		assertThat(copy.get("title").asString()).endsWith("(bản sao)");
		assertThat(copy.get("dueAt").isNull()).isTrue();
		JsonNode item = copy.get("parts").get(0).get("criteria").get(0).get("checklist").get(0);
		assertThat(item.get("kind").asString()).isEqualTo("LIMIT");
		assertThat(item.get("maxBand").decimalValue()).isEqualByComparingTo("5");
		mvc.perform(as(get("/api/teacher/assignments/" + copy.get("id").asLong() + "/targets")))
				.andExpect(jsonPath("$.data.recipientCount").value(0));
	}

	@Test
	@DisplayName("Upload ảnh đề: PNG thật → URL /uploads/yyyy/MM/…; xem công khai; file giả mạo đuôi → 400; sinh viên → 403")
	void uploadImage() throws Exception {
		byte[] png = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R' };
		String url = data(mvc.perform(multipart("/api/files")
				.file(new MockMultipartFile("file", "bieu-do.png", "image/png", png))
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.url", matchesPattern("^/uploads/\\d{4}/\\d{2}/[0-9a-f-]{36}\\.png$"))))
				.get("url").asString();

		byte[] served = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
		assertThat(served).isEqualTo(png);

		mvc.perform(multipart("/api/files").file(new MockMultipartFile("file", "virus.png", "image/png",
				"<script>alert(1)</script>".getBytes())).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_FILE"));
		mvc.perform(multipart("/api/files").file(new MockMultipartFile("file", "a.png", "image/png", png))
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken(newUser(Role.STUDENT)))))
				.andExpect(status().isForbidden());
	}
}
