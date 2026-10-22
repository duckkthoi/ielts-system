package com.ltmtu.ielts.grading;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/**
 * Dữ liệu chung cho test chấm bài: một bài Task 2 gồm 4 tiêu chí IELTS (TR, CC, LR bước 0.5, GRA), sinh viên đã
 * nộp version 1.
 */
abstract class GradingTestSupport extends IntegrationTest {

	/** 27 từ: trên ngưỡng 20 từ (BR-18) nhưng dưới yêu cầu 250 từ. */
	protected static final String ESSAY = "In recent years, people is increasingly concerned about the environment. "
			+ "Governments should invest more in public transport because it reduces pollution and traffic congestion "
			+ "in big cities.";

	protected User teacherUser;
	protected String teacher;
	protected User student;
	protected String studentToken;

	/** Bài đã nộp: id bài, phần, bài nộp, và tiêu chí (kèm checklist) theo phía giáo viên. */
	protected record Fixture(long assignmentId, long partId, long submissionId, JsonNode criteria) {

		long criterion(String code) {
			for (JsonNode c : criteria) {
				if (code.equals(c.get("code").asString())) {
					return c.get("id").asLong();
				}
			}
			throw new IllegalArgumentException(code);
		}

		long item(String code, int index) {
			for (JsonNode c : criteria) {
				if (code.equals(c.get("code").asString())) {
					return c.get("checklist").get(index).get("id").asLong();
				}
			}
			throw new IllegalArgumentException(code);
		}
	}

	@BeforeEach
	void setUpUsers() throws Exception {
		teacherUser = newUser(Role.TEACHER);
		teacher = accessToken(teacherUser);
		student = newUser(Role.STUDENT);
		studentToken = accessToken(student);
	}

	protected JsonNode data(ResultActions r) throws Exception {
		return json.readTree(r.andReturn().getResponse().getContentAsString()).get("data");
	}

	protected ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	protected MockHttpServletRequestBuilder putJson(String url, Object body) {
		return put(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
	}

	private static Map<String, Object> criterion(String code, String name, String step, List<Map<String, Object>> checklist) {
		return new HashMap<>(Map.of("code", code, "name", name, "step", step, "checklist", checklist));
	}

	private static Map<String, Object> item(String content, String kind, Integer maxBand) {
		Map<String, Object> m = new HashMap<>(Map.of("content", content, "kind", kind));
		if (maxBand != null) {
			m.put("maxBand", maxBand);
		}
		return m;
	}

	/** Phát hành bài cho {@link #student}, sinh viên nộp {@code essay}; trả về dữ liệu để chấm. */
	protected Fixture submitted(String essay, Duration dueIn, int maxRevisions) throws Exception {
		List<Map<String, Object>> criteria = List.of(
				criterion("TR", "Task Response", "1", List.of(item("Trả lời đủ các phần của đề", "CHECK", null),
						item("Các phần chính của đề chưa được trả lời đầy đủ", "LIMIT", 5))),
				criterion("CC", "Coherence and Cohesion", "1", List.of(item("Không chia đoạn", "LIMIT", 5),
						item("Toàn bộ bài lạc đề", "LIMIT", 2))),
				criterion("LR", "Lexical Resource", "0.5", List.of()),
				criterion("GRA", "Grammatical Range and Accuracy", "1", List.of()));
		Map<String, Object> part = new HashMap<>(Map.of("title", "Task 2", "taskType", "WRITING_TASK2",
				"prompt", "<p>Some people think…</p>", "minWords", 250, "criteria", criteria));
		Map<String, Object> body = new HashMap<>(Map.of("title", "Bài chấm thử", "dueAt",
				clock.instant().plus(dueIn).toString(), "maxRevisions", maxRevisions, "parts", List.of(part)));
		JsonNode a = data(call(postJson("/api/teacher/assignments", body), teacher).andExpect(status().isCreated()));
		long assignmentId = a.get("id").asLong();
		call(postJson("/api/teacher/assignments/" + assignmentId + "/publish", Map.of("studentIds", Set.of(student.getId()))),
				teacher).andExpect(status().isOk());
		long partId = a.get("parts").get(0).get("id").asLong();
		call(postJson("/api/student/parts/" + partId + "/submit", Map.of("content", essay, "expectedVersion", 0)),
				studentToken).andExpect(status().isCreated());
		JsonNode queue = data(call(get("/api/teacher/submissions").param("partId", String.valueOf(partId)), teacher)
				.andExpect(status().isOk()));
		return new Fixture(assignmentId, partId, queue.get(0).get("submissionId").asLong(),
				a.get("parts").get(0).get("criteria"));
	}

	protected Fixture submitted() throws Exception {
		return submitted(ESSAY, Duration.ofDays(3), 2);
	}

	protected static Map<String, Object> score(long criterionId, Object score) {
		Map<String, Object> m = new HashMap<>();
		m.put("criterionId", criterionId);
		m.put("score", score);
		return m;
	}

	protected static Map<String, Object> mark(long itemId, boolean checked) {
		return Map.of("checklistItemId", itemId, "checked", checked);
	}

	/** Điểm 4 tiêu chí theo thứ tự TR, CC, LR, GRA. */
	protected static List<Map<String, Object>> scores(Fixture f, Object tr, Object cc, Object lr, Object gra) {
		return new ArrayList<>(List.of(score(f.criterion("TR"), tr), score(f.criterion("CC"), cc),
				score(f.criterion("LR"), lr), score(f.criterion("GRA"), gra)));
	}

	protected static Map<String, Object> grade(int versionNo, List<?> scores, List<?> checklist, List<?> inline) {
		Map<String, Object> m = new HashMap<>();
		m.put("versionNo", versionNo);
		m.put("scores", scores);
		m.put("checklist", checklist);
		m.put("inlineComments", inline);
		return m;
	}

	protected ResultActions saveGrade(long submissionId, Map<String, Object> body) throws Exception {
		return call(putJson("/api/teacher/submissions/" + submissionId + "/grade", body), teacher);
	}
}
