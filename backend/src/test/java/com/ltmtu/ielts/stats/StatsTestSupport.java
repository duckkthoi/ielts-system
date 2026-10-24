package com.ltmtu.ielts.stats;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Dựng dữ liệu cho test tiến độ / thống kê qua API thật: lớp, bài nhiều phần, nộp, chấm, trả bài. */
abstract class StatsTestSupport extends IntegrationTest {

	/** 27 từ: trên ngưỡng 20 từ của BR-18, chấm điểm nào cũng được. */
	protected static final String ESSAY = "In recent years, people is increasingly concerned about the environment. "
			+ "Governments should invest more in public transport because it reduces pollution and traffic congestion "
			+ "in big cities.";

	protected User teacherUser;
	protected String teacher;

	/** Bài vừa tạo: id bài, id các phần, và id tiêu chí theo (thứ tự phần, mã tiêu chí). */
	protected record Created(long id, List<Long> parts, List<Map<String, Long>> criteria) {

		long part(int index) {
			return parts.get(index);
		}

		long criterion(int partIndex, String code) {
			return criteria.get(partIndex).get(code);
		}
	}

	@BeforeEach
	void setUpTeacher() throws Exception {
		teacherUser = newUser(Role.TEACHER);
		teacher = accessToken(teacherUser);
	}

	/** Lấy lại token giáo viên sau khi tua đồng hồ (token cũ có thể đã hết hạn theo đồng hồ giả). */
	protected void reloginTeacher() throws Exception {
		teacher = accessToken(teacherUser);
	}

	protected long createClass(String token, User... students) throws Exception {
		long id = data(call(postJson("/api/teacher/classes",
				Map.of("code", "st-" + UUID.randomUUID().toString().substring(0, 6), "name", "Lớp thống kê")), token)
				.andExpect(apiStatus(201))).get("id").asLong();
		if (students.length > 0) {
			List<String> codes = new ArrayList<>();
			for (User s : students) {
				codes.add(s.getStudentCode());
			}
			call(postJson("/api/teacher/classes/" + id + "/members", Map.of("studentCodes", codes)), token)
					.andExpect(apiStatus(200));
		}
		return id;
	}

	/** Một phần với các tiêu chí theo mã (bước 1, không checklist). */
	protected static Map<String, Object> part(String title, String taskType, String... codes) {
		List<Map<String, Object>> criteria = new ArrayList<>();
		for (String code : codes) {
			criteria.add(Map.of("code", code, "name", code, "step", "1", "checklist", List.of()));
		}
		return new HashMap<>(Map.of("title", title, "taskType", taskType, "prompt", "<p>Đề bài</p>",
				"criteria", criteria));
	}

	/** Tạo bài cho phép nộp muộn, hạn sau {@code dueIn}, rồi phát hành cho lớp và sinh viên. */
	protected Created publish(Duration dueIn, List<Long> classIds, List<User> students, List<Map<String, Object>> parts)
			throws Exception {
		JsonNode a = data(call(postJson("/api/teacher/assignments", Map.of(
				"title", "Bài thống kê", "dueAt", clock.instant().plus(dueIn).toString(),
				"allowLate", true, "maxRevisions", 2, "parts", parts)), teacher)
				.andExpect(apiStatus(201)));
		List<Long> partIds = new ArrayList<>();
		List<Map<String, Long>> criteria = new ArrayList<>();
		for (JsonNode p : a.get("parts")) {
			partIds.add(p.get("id").asLong());
			Map<String, Long> byCode = new LinkedHashMap<>();
			p.get("criteria").forEach(c -> byCode.put(c.get("code").asString(), c.get("id").asLong()));
			criteria.add(byCode);
		}
		long id = a.get("id").asLong();
		call(postJson("/api/teacher/assignments/" + id + "/publish",
				Map.of("classIds", classIds, "studentIds", students.stream().map(User::getId).toList())), teacher)
				.andExpect(apiStatus(200));
		return new Created(id, partIds, criteria);
	}

	/** Sinh viên nộp version {@code expectedVersion + 1}; trả về id bài nộp lấy từ hàng đợi chấm. */
	protected long submit(User student, long partId, int expectedVersion) throws Exception {
		call(postJson("/api/student/parts/" + partId + "/submit",
				Map.of("content", ESSAY, "expectedVersion", expectedVersion)), accessToken(student))
				.andExpect(apiStatus(201));
		for (JsonNode q : data(call(get("/api/teacher/submissions").param("partId", String.valueOf(partId)), teacher))) {
			if (q.get("student").get("id").asLong() == student.getId()) {
				return q.get("submissionId").asLong();
			}
		}
		throw new IllegalStateException("Không thấy bài nộp trong hàng đợi");
	}

	protected long submit(User student, long partId) throws Exception {
		return submit(student, partId, 0);
	}

	/** Nhận xét inline có mã lỗi trên đoạn {@code phrase} của {@link #ESSAY}. */
	protected static Map<String, Object> inline(String phrase, String errorCode) {
		int start = ESSAY.indexOf(phrase);
		return Map.of("startOffset", start, "endOffset", start + phrase.length(), "errorCode", errorCode);
	}

	/** Lưu chấm: điểm theo id tiêu chí; {@code zeroReason} khác null = "Chấm 0". */
	protected void grade(long submissionId, int versionNo, Map<Long, Integer> scores, String zeroReason,
			List<Map<String, Object>> inline) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("versionNo", versionNo);
		List<Map<String, Object>> list = new ArrayList<>();
		scores.forEach((criterionId, score) -> list.add(Map.of("criterionId", criterionId, "score", score)));
		body.put("scores", list);
		body.put("inlineComments", inline);
		if (zeroReason != null) {
			body.put("zeroReason", zeroReason);
		}
		call(put("/api/teacher/submissions/" + submissionId + "/grade").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(body)), teacher).andExpect(apiStatus(200));
	}

	protected void grade(long submissionId, Map<Long, Integer> scores) throws Exception {
		grade(submissionId, 1, scores, null, List.of());
	}

	protected void returnGrade(long submissionId) throws Exception {
		call(post("/api/teacher/submissions/" + submissionId + "/return"), teacher).andExpect(apiStatus(200));
	}

	protected JsonNode data(ResultActions r) throws Exception {
		return json.readTree(r.andReturn().getResponse().getContentAsString()).get("data");
	}

	protected ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}
}
