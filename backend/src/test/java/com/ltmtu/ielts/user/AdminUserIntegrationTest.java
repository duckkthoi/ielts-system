package com.ltmtu.ielts.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

/** UC-05, UC-06, UC-09 — docs/05-api.md mục A.3. */
class AdminUserIntegrationTest extends IntegrationTest {

	private User admin;
	private String adminToken;

	@BeforeEach
	void loginAsAdmin() throws Exception {
		admin = newUser(Role.ADMIN);
		adminToken = accessToken(admin);
	}

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder req) {
		return req.header(HttpHeaders.AUTHORIZATION, bearer(adminToken));
	}

	private JsonNode createTeacher(String username) throws Exception {
		String body = mvc.perform(asAdmin(postJson("/api/admin/teachers",
				Map.of("username", username, "fullName", "ThS. Nguyễn Thị Hạnh", "email", username + "@truong.edu.vn"))))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("data");
	}

	private static String uniqueName(String prefix) {
		return prefix + UUID.randomUUID().toString().substring(0, 8);
	}

	@Test
	@DisplayName("UC-05: tạo giáo viên → 201 + mật khẩu tạm; đăng nhập được và bị bắt đổi mật khẩu (BR-02)")
	void createTeacher_thenLoginWithTemporaryPassword() throws Exception {
		String username = uniqueName("gv.");
		JsonNode created = createTeacher(username);

		assertThat(created.get("teacher").get("role").asString()).isEqualTo("TEACHER");
		assertThat(created.get("teacher").get("mustChangePassword").asBoolean()).isTrue();
		String temp = created.get("temporaryPassword").asString();
		assertThat(temp).hasSize(8);

		JsonNode session = login(username, temp);
		assertThat(session.get("user").get("mustChangePassword").asBoolean()).isTrue();
	}

	@Test
	@DisplayName("UC-05: trùng tên đăng nhập → 409 DUPLICATE; tên sai định dạng → 400")
	void createTeacher_duplicateOrInvalid() throws Exception {
		String username = uniqueName("gv.");
		createTeacher(username);

		mvc.perform(asAdmin(postJson("/api/admin/teachers", Map.of("username", username, "fullName", "Khác"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE"));
		mvc.perform(asAdmin(postJson("/api/admin/teachers", Map.of("username", "có dấu cách", "fullName", "X"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.username").exists());
	}

	@Test
	@DisplayName("UC-05: sửa giáo viên; id không phải giáo viên → 404")
	void updateTeacher() throws Exception {
		long id = createTeacher(uniqueName("gv.")).get("teacher").get("id").asLong();
		mvc.perform(asAdmin(put("/api/admin/teachers/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("fullName", "TS. Trần Văn Minh", "email", "minh@truong.edu.vn")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fullName").value("TS. Trần Văn Minh"));

		User student = newUser(Role.STUDENT);
		mvc.perform(asAdmin(put("/api/admin/teachers/" + student.getId()).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("fullName", "X")))))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("UC-06: tra cứu theo từ khoá + vai trò, có phân trang và totalRecord")
	void searchUsers() throws Exception {
		User student = newUser(Role.STUDENT);

		mvc.perform(asAdmin(get("/api/admin/users").param("keyword", student.getStudentCode().toLowerCase())
				.param("role", "STUDENT").param("size", "5")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(1)))
				.andExpect(jsonPath("$.data[0].username").value(student.getUsername()))
				.andExpect(jsonPath("$.totalRecord").value(1));

		mvc.perform(asAdmin(get("/api/admin/teachers").param("size", "100")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].role", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("TEACHER"))));
	}

	@Test
	@DisplayName("Sắp xếp theo trường không cho phép → 400, không phải 500")
	void searchUsers_invalidSort() throws Exception {
		mvc.perform(asAdmin(get("/api/admin/users").param("sort", "passwordHash,asc")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("Khoá tài khoản → token đang dùng bị từ chối ngay, đăng nhập lại → 403; mở khoá → đăng nhập được")
	void lockAndUnlock() throws Exception {
		User student = newUser(Role.STUDENT);
		String studentToken = accessToken(student);

		mvc.perform(asAdmin(post("/api/admin/users/" + student.getId() + "/lock")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.active").value(false));

		mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
				.andExpect(status().isUnauthorized());
		mvc.perform(postJson("/api/auth/login", Map.of("username", student.getUsername(), "password", DEFAULT_PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

		mvc.perform(asAdmin(post("/api/admin/users/" + student.getId() + "/unlock")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.active").value(true));
		login(student.getUsername(), DEFAULT_PASSWORD);
	}

	@Test
	@DisplayName("Admin không được tự khoá chính mình → 409 INVALID_STATE")
	void cannotLockSelf() throws Exception {
		mvc.perform(asAdmin(post("/api/admin/users/" + admin.getId() + "/lock")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_STATE"));
	}

	@Test
	@DisplayName("Đặt lại mật khẩu → mật khẩu cũ hết dùng được, phiên cũ bị thu hồi, mật khẩu tạm đăng nhập được")
	void resetPassword() throws Exception {
		User teacher = newUser(Role.TEACHER);
		String oldToken = accessToken(teacher);

		String body = mvc.perform(asAdmin(post("/api/admin/users/" + teacher.getId() + "/reset-password")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String temp = json.readTree(body).get("data").get("temporaryPassword").asString();

		mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(oldToken)))
				.andExpect(status().isUnauthorized());
		mvc.perform(postJson("/api/auth/login", Map.of("username", teacher.getUsername(), "password", DEFAULT_PASSWORD)))
				.andExpect(status().isUnauthorized());
		assertThat(login(teacher.getUsername(), temp).get("user").get("mustChangePassword").asBoolean()).isTrue();
	}

	@Test
	@DisplayName("UC-06: chi tiết người dùng kèm số phiên đang hoạt động; id không tồn tại → 404")
	void userDetail() throws Exception {
		User teacher = newUser(Role.TEACHER);
		accessToken(teacher);
		accessToken(teacher);

		mvc.perform(asAdmin(get("/api/admin/users/" + teacher.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.username").value(teacher.getUsername()))
				.andExpect(jsonPath("$.data.activeSessions").value(2));
		mvc.perform(asAdmin(get("/api/admin/users/999999999")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("UC-09: thống kê hệ thống có đủ các mục, biểu đồ 14 ngày")
	void systemStats() throws Exception {
		mvc.perform(asAdmin(get("/api/admin/stats")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.usersByRole.ADMIN", greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.data.usersByRole.TEACHER").exists())
				.andExpect(jsonPath("$.data.usersByRole.STUDENT").exists())
				.andExpect(jsonPath("$.data.assignmentsByStatus.PUBLISHED").exists())
				.andExpect(jsonPath("$.data.submissionsByDay", hasSize(14)))
				.andExpect(jsonPath("$.data.activeSessions", greaterThanOrEqualTo(1)));
	}

	@Test
	@DisplayName("Giáo viên / sinh viên gọi API quản trị → 403")
	void nonAdminForbidden() throws Exception {
		for (Role role : new Role[] { Role.TEACHER, Role.STUDENT }) {
			String token = accessToken(newUser(role));
			mvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
					.andExpect(status().isForbidden());
		}
		mvc.perform(asAdmin(get("/api/admin/users").param("role", "ADMIN")))
				.andExpect(jsonPath("$.data[*].username", hasItem(admin.getUsername())));
	}
}
