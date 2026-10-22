package com.ltmtu.ielts.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;

import tools.jackson.databind.JsonNode;

/** UC-13, BR-06, BR-11, BR-16, docs/08 — soạn bài tập nháp và rubric. */
class AssignmentIntegrationTest extends IntegrationTest {

	@Autowired
	private AssignmentRepository assignmentRepository;

	private String token;

	@BeforeEach
	void login() throws Exception {
		token = accessToken(newUser(Role.TEACHER));
	}

	private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req) {
		return req.header(HttpHeaders.AUTHORIZATION, bearer(token));
	}

	private ResultActions create(Object body) throws Exception {
		return mvc.perform(as(postJson("/api/teacher/assignments", body)));
	}

	private JsonNode createOk(Object body) throws Exception {
		return json.readTree(create(body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
				.get("data");
	}

	private ResultActions putJson(String url, Object body) throws Exception {
		return mvc.perform(as(put(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))));
	}

	private ResultActions patchJson(String url, Object body) throws Exception {
		return mvc.perform(as(patch(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))));
	}

	/** Lấy rubric mẫu của hệ thống và chuyển về dạng CriterionRequest (giống client "nạp rubric mẫu"). */
	private List<Map<String, Object>> templateCriteria(String taskType) throws Exception {
		String body = mvc.perform(as(get("/api/teacher/rubric-templates").param("taskType", taskType)))
				.andReturn().getResponse().getContentAsString();
		JsonNode tpl = json.readTree(body).get("data").get(0);
		List<Map<String, Object>> criteria = new ArrayList<>();
		for (JsonNode c : tpl.get("criteria")) {
			List<Map<String, Object>> items = new ArrayList<>();
			for (JsonNode i : c.get("checklist")) {
				Map<String, Object> item = new HashMap<>();
				item.put("content", i.get("content").asString());
				item.put("kind", i.get("kind").asString());
				if (!i.get("maxBand").isNull()) {
					item.put("maxBand", i.get("maxBand").decimalValue());
				}
				items.add(item);
			}
			criteria.add(Map.of("code", c.get("code").asString(), "name", c.get("name").asString(), "checklist", items));
		}
		return criteria;
	}

	private Map<String, Object> part(String title, String taskType, String prompt, List<Map<String, Object>> criteria) {
		Map<String, Object> p = new HashMap<>();
		p.put("title", title);
		p.put("taskType", taskType);
		p.put("prompt", prompt);
		p.put("minWords", "WRITING_TASK1".equals(taskType) ? 150 : 250);
		p.put("criteria", criteria);
		return p;
	}

	private Map<String, Object> assignment(String title, List<Map<String, Object>> parts) {
		Map<String, Object> a = new HashMap<>();
		a.put("title", title);
		a.put("dueAt", Instant.now().plus(Duration.ofDays(7)).toString());
		a.put("parts", parts);
		return a;
	}

	private static Map<String, Object> criterion(Map<String, Object> overrides) {
		Map<String, Object> c = new HashMap<>(Map.of("name", "Tiêu chí", "checklist", List.of()));
		c.putAll(overrides);
		return c;
	}

	@Nested
	@DisplayName("Rubric mẫu và bảng mô tả band (V4, docs/08)")
	class Templates {

		@Test
		@DisplayName("BR-11 + BR-16: rubric mẫu Task 2 có mã TR/CC/LR/GRA, bước 1, có mục Giới hạn điểm kèm band tối đa")
		void task2Template() throws Exception {
			String body = mvc.perform(as(get("/api/teacher/rubric-templates").param("taskType", "WRITING_TASK2")))
					.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
			JsonNode tpl = json.readTree(body).get("data").get(0);
			assertThat(tpl.get("system").asBoolean()).isTrue();
			List<String> codes = new ArrayList<>();
			tpl.get("criteria").forEach(c -> {
				codes.add(c.get("code").asString());
				assertThat(c.get("step").decimalValue()).isEqualByComparingTo("1");
			});
			assertThat(codes).containsExactly("TR", "CC", "LR", "GRA");

			JsonNode cc = tpl.get("criteria").get(1);
			JsonNode noParagraph = null;
			for (JsonNode i : cc.get("checklist")) {
				if (i.get("content").asString().contains("Không chia đoạn")) {
					noParagraph = i;
				}
			}
			assertThat(noParagraph).isNotNull();
			assertThat(noParagraph.get("kind").asString()).isEqualTo("LIMIT");
			assertThat(noParagraph.get("maxBand").decimalValue()).isEqualByComparingTo("5");
		}

		@Test
		@DisplayName("Rubric mẫu Task 1: TA có mục 'Không có tổng quan' giới hạn ở 5")
		void task1Template() throws Exception {
			mvc.perform(as(get("/api/teacher/rubric-templates").param("taskType", "WRITING_TASK1")))
					.andExpect(jsonPath("$.data[0].criteria[0].code").value("TA"))
					.andExpect(jsonPath("$.data[0].criteria[0].checklist[?(@.kind == 'LIMIT')].content",
							hasItem(containsString("tổng quan"))));
		}

		@Test
		@DisplayName("Bảng mô tả band thu gọn: Task 2 có TR/CC/LR/GRA (không có TA), đủ 10 mức, kèm link ielts.org")
		void descriptors() throws Exception {
			mvc.perform(as(get("/api/teacher/rubric-templates/descriptors").param("taskType", "WRITING_TASK2")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.source.url", containsString("ielts.org")))
					.andExpect(jsonPath("$.data.criteria.TR.bands.9").exists())
					.andExpect(jsonPath("$.data.criteria.GRA.bands.0").exists())
					.andExpect(jsonPath("$.data.criteria.TA").doesNotExist());
		}
	}

	@Nested
	@DisplayName("Tạo và sửa bài nháp")
	class Drafts {

		@Test
		@DisplayName("UC-13: tạo bài 2 phần từ rubric mẫu → nháp; Task 2 trọng số 2 (BR-12); mục Đạt không giữ maxBand")
		void createFromTemplates() throws Exception {
			List<Map<String, Object>> t1 = templateCriteria("WRITING_TASK1");
			List<Map<String, Object>> t2 = templateCriteria("WRITING_TASK2");
			// Client gửi nhầm maxBand cho mục "Đạt" → server bỏ đi.
			@SuppressWarnings("unchecked")
			var firstItem = new HashMap<>(((List<Map<String, Object>>) t2.get(0).get("checklist")).get(0));
			firstItem.put("maxBand", 3);
			var tr = new HashMap<>(t2.get(0));
			var trItems = new ArrayList<Map<String, Object>>();
			trItems.add(firstItem);
			tr.put("checklist", trItems);
			t2.set(0, tr);

			JsonNode a = createOk(assignment("Tuần 5 – Writing practice", List.of(
					part("Task 1 – Line graph", "WRITING_TASK1", "<p>The graph below shows…</p>", t1),
					part("Task 2 – Opinion essay", "WRITING_TASK2", "<p>Some people believe…</p>", t2))));

			assertThat(a.get("status").asString()).isEqualTo("DRAFT");
			assertThat(a.get("maxRevisions").asInt()).isEqualTo(2);
			assertThat(a.get("parts").get(0).get("weight").decimalValue()).isEqualByComparingTo("1");
			assertThat(a.get("parts").get(1).get("weight").decimalValue()).isEqualByComparingTo("2");
			assertThat(a.get("parts").get(1).get("criteria").get(0).get("step").decimalValue()).isEqualByComparingTo("1");
			assertThat(a.get("parts").get(1).get("criteria").get(0).get("checklist").get(0).get("maxBand").isNull()).isTrue();
			assertThat(a.get("parts").get(0).get("criteria")).hasSize(4);
		}

		@Test
		@DisplayName("Đề bài được lọc HTML: bỏ <script> và thuộc tính sự kiện, giữ định dạng cơ bản")
		void promptSanitized() throws Exception {
			JsonNode a = createOk(assignment("XSS", List.of(part("P1", "CUSTOM",
					"<p onclick=\"steal()\">Viết về <strong>gia đình</strong></p><script>alert(1)</script>", List.of()))));
			String prompt = a.get("parts").get(0).get("prompt").asString();
			assertThat(prompt).contains("<strong>gia đình</strong>").doesNotContain("script", "onclick");
		}

		@Test
		@DisplayName("Quy tắc rubric: bước khác 1/0.5, Giới hạn thiếu band / band vượt / band lẻ khi bước 1, lặp mã → 400")
		void rubricRules() throws Exception {
			List<Map<String, Object>> bad = List.of(
					criterion(Map.of("step", 0.3)),
					criterion(Map.of("checklist", List.of(Map.of("content", "X", "kind", "LIMIT")))),
					criterion(Map.of("maxScore", 9, "checklist", List.of(Map.of("content", "X", "kind", "LIMIT", "maxBand", 9.5)))),
					criterion(Map.of("checklist", List.of(Map.of("content", "X", "kind", "LIMIT", "maxBand", 4.5)))));
			for (Map<String, Object> c : bad) {
				create(assignment("Sai rubric", List.of(part("P1", "CUSTOM", "<p>Đề</p>", List.of(c)))))
						.andExpect(status().isBadRequest())
						.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
			}
			create(assignment("Lặp mã", List.of(part("P1", "WRITING_TASK2", "<p>Đề</p>",
					List.of(criterion(Map.of("code", "CC")), criterion(Map.of("code", "CC")))))))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message", containsString("bị lặp")));
			// Bước 0.5 thì band tối đa 4.5 hợp lệ.
			create(assignment("Bước 0.5", List.of(part("P1", "CUSTOM", "<p>Đề</p>", List.of(criterion(Map.of("step", 0.5,
					"checklist", List.of(Map.of("content", "X", "kind", "LIMIT", "maxBand", 4.5)))))))))
					.andExpect(status().isCreated());
		}

		@Test
		@DisplayName("Lỗi dữ liệu khác: mở ≥ hạn, link ảnh javascript:, đề chỉ có script, thiếu loại phần → 400")
		void otherValidation() throws Exception {
			Map<String, Object> wrongDates = assignment("Ngày sai", List.of());
			wrongDates.put("openAt", Instant.now().plus(Duration.ofDays(8)).toString());
			create(wrongDates).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", containsString("Thời điểm mở")));

			Map<String, Object> p = part("P1", "CUSTOM", "<p>Đề</p>", List.of());
			p.put("imageUrl", "javascript:alert(1)");
			create(assignment("Ảnh", List.of(p))).andExpect(status().isBadRequest());

			create(assignment("Script", List.of(part("P1", "CUSTOM", "<script>x()</script>", List.of()))))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", containsString("đề bài trống")));

			Map<String, Object> noType = part("P1", "CUSTOM", "<p>Đề</p>", List.of());
			noType.remove("taskType");
			create(assignment("Thiếu loại", List.of(noType)))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors['parts[0].taskType']").exists());
		}

		@Test
		@DisplayName("Thay toàn bộ bài nháp: đổi số phần và thứ tự vẫn lưu đúng (không vướng UNIQUE order_no)")
		void replaceDraft() throws Exception {
			JsonNode a = createOk(assignment("Bản 1", List.of(
					part("A", "WRITING_TASK1", "<p>a</p>", List.of(criterion(Map.of("code", "TA")))),
					part("B", "WRITING_TASK2", "<p>b</p>", List.of()))));
			long id = a.get("id").asLong();

			putJson("/api/teacher/assignments/" + id, assignment("Bản 2", List.of(
					part("B mới", "WRITING_TASK2", "<p>b2</p>", List.of(criterion(Map.of("code", "TR")), criterion(Map.of("code", "CC")))),
					part("A mới", "WRITING_TASK1", "<p>a2</p>", List.of()),
					part("C", "CUSTOM", "<p>c</p>", List.of()))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.title").value("Bản 2"))
					.andExpect(jsonPath("$.data.parts[*].title", contains("B mới", "A mới", "C")))
					.andExpect(jsonPath("$.data.parts[0].orderNo").value(1))
					.andExpect(jsonPath("$.data.parts[0].criteria[*].code", contains("TR", "CC")));
		}
	}

	@Nested
	@DisplayName("Trạng thái, quyền sở hữu, danh sách")
	class StateAndOwnership {

		@Test
		@DisplayName("BR-06: bài đã phát hành không thay toàn bộ / không xoá được (409); sửa thông tin chung được")
		void publishedRules() throws Exception {
			JsonNode a = createOk(assignment("Đã giao", List.of(part("P1", "WRITING_TASK2", "<p>Đề</p>", List.of()))));
			long id = a.get("id").asLong();
			long partId = a.get("parts").get(0).get("id").asLong();
			Assignment entity = assignmentRepository.findById(id).orElseThrow();
			entity.setStatus(AssignmentStatus.PUBLISHED);
			assignmentRepository.save(entity);

			putJson("/api/teacher/assignments/" + id, assignment("Sửa", List.of()))
					.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
			mvc.perform(as(delete("/api/teacher/assignments/" + id)))
					.andExpect(status().isConflict());

			String newDue = Instant.now().plus(Duration.ofDays(14)).toString();
			patchJson("/api/teacher/assignments/" + id + "/settings", Map.of("title", "Đã giao (gia hạn)", "dueAt", newDue,
					"allowLate", true, "partDueAts", Map.of(String.valueOf(partId), newDue)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.title").value("Đã giao (gia hạn)"))
					.andExpect(jsonPath("$.data.allowLate").value(true))
					.andExpect(jsonPath("$.data.parts[0].partDueAt").exists());

			patchJson("/api/teacher/assignments/" + id + "/settings", Map.of("partDueAts", Map.of("999999999", newDue)))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("Xoá bài nháp → 200, sau đó 404")
		void deleteDraft() throws Exception {
			long id = createOk(assignment("Nháp xoá", List.of())).get("id").asLong();
			mvc.perform(as(delete("/api/teacher/assignments/" + id))).andExpect(status().isOk());
			mvc.perform(as(get("/api/teacher/assignments/" + id))).andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("BR-15: bài của giáo viên khác → 404 ở xem / thay / sửa / xoá; không có trong danh sách")
		void ownership() throws Exception {
			long mine = createOk(assignment("Của tôi", List.of())).get("id").asLong();
			String other = accessToken(newUser(Role.TEACHER));
			for (MockHttpServletRequestBuilder req : List.of(get("/api/teacher/assignments/" + mine),
					put("/api/teacher/assignments/" + mine).contentType(MediaType.APPLICATION_JSON)
							.content(json.writeValueAsString(assignment("X", List.of()))),
					patch("/api/teacher/assignments/" + mine + "/settings").contentType(MediaType.APPLICATION_JSON).content("{}"),
					delete("/api/teacher/assignments/" + mine))) {
				mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(other))).andExpect(status().isNotFound());
			}
			mvc.perform(get("/api/teacher/assignments").param("size", "100").header(HttpHeaders.AUTHORIZATION, bearer(other)))
					.andExpect(jsonPath("$.data[*].id", not(hasItem((int) mine))));
		}

		@Test
		@DisplayName("Danh sách: lọc theo trạng thái + từ khoá, có số phần và totalRecord")
		void list() throws Exception {
			createOk(assignment("Tuần 6 – Bar chart", List.of(part("P", "WRITING_TASK1", "<p>x</p>", List.of()),
					part("Q", "WRITING_TASK2", "<p>y</p>", List.of()))));
			createOk(assignment("Tuần 7 – Map", List.of()));

			mvc.perform(as(get("/api/teacher/assignments").param("status", "DRAFT").param("keyword", "bar chart")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.totalRecord").value(1))
					.andExpect(jsonPath("$.data[0].partCount").value(2));
			mvc.perform(as(get("/api/teacher/assignments").param("status", "PUBLISHED")))
					.andExpect(jsonPath("$.totalRecord").value(0));
		}

		@Test
		@DisplayName("Sinh viên gọi API bài tập của giáo viên → 403")
		void studentForbidden() throws Exception {
			mvc.perform(get("/api/teacher/assignments").header(HttpHeaders.AUTHORIZATION, bearer(accessToken(newUser(Role.STUDENT)))))
					.andExpect(status().isForbidden());
		}
	}
}
