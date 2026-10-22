package com.ltmtu.ielts.classroom;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

/** UC-10 — docs/05-api.md mục A.4. */
class ClassroomIntegrationTest extends IntegrationTest {

	private User teacher;
	private String teacherToken;

	@BeforeEach
	void loginAsTeacher() throws Exception {
		teacher = newUser(Role.TEACHER);
		teacherToken = accessToken(teacher);
	}

	private MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder req) {
		return req.header(HttpHeaders.AUTHORIZATION, bearer(token));
	}

	private static String uniqueCode() {
		return "k15-" + UUID.randomUUID().toString().substring(0, 6);
	}

	private long createClass(String token, String code, String name) throws Exception {
		String body = mvc.perform(as(token, postJson("/api/teacher/classes",
				Map.of("code", code, "name", name, "description", "Lớp luyện IELTS"))))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("data").get("id").asLong();
	}

	@Test
	@DisplayName("Tạo lớp → 201, mã lớp lưu viết hoa; trùng mã (khác hoa thường) → 409 DUPLICATE")
	void create_normalizesCode_andRejectsDuplicate() throws Exception {
		String code = uniqueCode();
		long id = createClass(teacherToken, code, "IELTS K15 – Lớp A");

		mvc.perform(as(teacherToken, get("/api/teacher/classes/" + id)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.code").value(code.toUpperCase()))
				.andExpect(jsonPath("$.data.memberCount").value(0));

		mvc.perform(as(teacherToken, postJson("/api/teacher/classes", Map.of("code", code.toUpperCase(), "name", "X"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE"));
	}

	@Test
	@DisplayName("Mã lớp sai định dạng / thiếu tên → 400 kèm lỗi từng trường")
	void create_validation() throws Exception {
		mvc.perform(as(teacherToken, postJson("/api/teacher/classes", Map.of("code", "có dấu", "name", ""))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.code").exists())
				.andExpect(jsonPath("$.fieldErrors.name").exists());
	}

	@Test
	@DisplayName("BR-15: giáo viên chỉ thấy lớp của mình; lớp của người khác → 404 khi xem / sửa / xoá")
	void ownership() throws Exception {
		User other = newUser(Role.TEACHER);
		String otherToken = accessToken(other);
		long mine = createClass(teacherToken, uniqueCode(), "Lớp của tôi");
		long theirs = createClass(otherToken, uniqueCode(), "Lớp người khác");

		mvc.perform(as(teacherToken, get("/api/teacher/classes").param("size", "100")))
				.andExpect(jsonPath("$.data[*].id", hasItem((int) mine)))
				.andExpect(jsonPath("$.data[*].id", not(hasItem((int) theirs))));

		mvc.perform(as(teacherToken, get("/api/teacher/classes/" + theirs))).andExpect(status().isNotFound());
		mvc.perform(as(teacherToken, put("/api/teacher/classes/" + theirs).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("code", uniqueCode(), "name", "Chiếm lớp")))))
				.andExpect(status().isNotFound());
		mvc.perform(as(teacherToken, delete("/api/teacher/classes/" + theirs))).andExpect(status().isNotFound());
		mvc.perform(as(teacherToken, get("/api/teacher/classes/" + theirs + "/members")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Sửa và xoá lớp; tìm lớp theo từ khoá")
	void updateSearchDelete() throws Exception {
		long id = createClass(teacherToken, uniqueCode(), "Tên cũ");
		String newCode = uniqueCode();

		mvc.perform(as(teacherToken, put("/api/teacher/classes/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("code", newCode, "name", "Lớp Writing nâng cao")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.code").value(newCode.toUpperCase()))
				.andExpect(jsonPath("$.data.description").isEmpty());

		mvc.perform(as(teacherToken, get("/api/teacher/classes").param("keyword", "writing nâng")))
				.andExpect(jsonPath("$.data[*].id", contains((int) id)));

		mvc.perform(as(teacherToken, delete("/api/teacher/classes/" + id))).andExpect(status().isOk());
		mvc.perform(as(teacherToken, get("/api/teacher/classes/" + id))).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Thêm sinh viên theo mã SV: đã có trong lớp / không tồn tại / không phải sinh viên được báo riêng")
	void addMembers() throws Exception {
		long id = createClass(teacherToken, uniqueCode(), "Lớp A");
		User s1 = newUser(Role.STUDENT);
		User s2 = newUser(Role.STUDENT);
		User notStudent = newUser(Role.TEACHER);

		mvc.perform(as(teacherToken, postJson("/api/teacher/classes/" + id + "/members",
				Map.of("studentCodes", List.of(s1.getStudentCode())))))
				.andExpect(jsonPath("$.data.added").value(1));

		mvc.perform(as(teacherToken, postJson("/api/teacher/classes/" + id + "/members",
				Map.of("studentCodes", List.of(s1.getStudentCode(), s2.getStudentCode(), "KHONGCO", notStudent.getUsername())))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.added").value(1))
				.andExpect(jsonPath("$.data.alreadyMember").value(1))
				.andExpect(jsonPath("$.data.notFound", containsInAnyOrder("KHONGCO", notStudent.getUsername())));

		mvc.perform(as(teacherToken, get("/api/teacher/classes/" + id)))
				.andExpect(jsonPath("$.data.memberCount").value(2));
	}

	@Test
	@DisplayName("Danh sách thành viên: tìm theo tên / mã, sắp xếp, xoá khỏi lớp; xoá người không thuộc lớp → 404")
	void membersListAndRemove() throws Exception {
		long id = createClass(teacherToken, uniqueCode(), "Lớp B");
		User s1 = newUser(Role.STUDENT);
		User s2 = newUser(Role.STUDENT);
		mvc.perform(as(teacherToken, postJson("/api/teacher/classes/" + id + "/members",
				Map.of("studentCodes", List.of(s1.getStudentCode(), s2.getStudentCode())))));

		mvc.perform(as(teacherToken, get("/api/teacher/classes/" + id + "/members").param("sort", "fullName,desc")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(2)))
				.andExpect(jsonPath("$.totalRecord").value(2));
		mvc.perform(as(teacherToken, get("/api/teacher/classes/" + id + "/members")
				.param("keyword", s2.getStudentCode().toLowerCase())))
				.andExpect(jsonPath("$.data[*].studentId", contains(s2.getId().intValue())));

		mvc.perform(as(teacherToken, delete("/api/teacher/classes/" + id + "/members/" + s1.getId())))
				.andExpect(status().isOk());
		mvc.perform(as(teacherToken, delete("/api/teacher/classes/" + id + "/members/" + s1.getId())))
				.andExpect(status().isNotFound());
		mvc.perform(as(teacherToken, get("/api/teacher/classes/" + id)))
				.andExpect(jsonPath("$.data.memberCount").value(1));
	}

	@Test
	@DisplayName("Tìm sinh viên để giao bài: chỉ trả tài khoản vai trò STUDENT")
	void searchStudents() throws Exception {
		User s = newUser(Role.STUDENT);
		mvc.perform(as(teacherToken, get("/api/teacher/students/search").param("keyword", s.getStudentCode())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", contains(s.getId().intValue())));
		mvc.perform(as(teacherToken, get("/api/teacher/students/search").param("keyword", "teacher_")))
				.andExpect(jsonPath("$.data", hasSize(0)));
	}

	@Test
	@DisplayName("Admin xem danh sách giáo viên có cột số lớp (classCount)")
	void adminSeesClassCount() throws Exception {
		createClass(teacherToken, uniqueCode(), "L1");
		createClass(teacherToken, uniqueCode(), "L2");
		String adminToken = accessToken(newUser(Role.ADMIN));

		mvc.perform(as(adminToken, get("/api/admin/teachers").param("keyword", teacher.getUsername())))
				.andExpect(jsonPath("$.data[0].classCount").value(2));
		mvc.perform(as(adminToken, get("/api/admin/users").param("role", "STUDENT").param("size", "5")))
				.andExpect(jsonPath("$.data[*].classCount", everyItem(org.hamcrest.Matchers.nullValue())));
	}

	@Test
	@DisplayName("Từ khoá chứa ký tự đặc biệt của LIKE (_ %) được tìm theo nghĩa đen")
	void likeWildcardsAreEscaped() throws Exception {
		String adminToken = accessToken(newUser(Role.ADMIN));
		// "_" không escape sẽ khớp mọi ký tự → "teacher_" sẽ khớp cả "teacherX…"; "%" không escape khớp tất cả.
		mvc.perform(as(adminToken, get("/api/admin/users").param("keyword", teacher.getUsername())))
				.andExpect(jsonPath("$.data[*].id", contains(teacher.getId().intValue())));
		mvc.perform(as(adminToken, get("/api/admin/users").param("keyword", "%")))
				.andExpect(jsonPath("$.data", hasSize(0)));
	}

	@Test
	@DisplayName("Sinh viên gọi API lớp của giáo viên → 403")
	void studentForbidden() throws Exception {
		String token = accessToken(newUser(Role.STUDENT));
		mvc.perform(as(token, get("/api/teacher/classes"))).andExpect(status().isForbidden());
	}
}
