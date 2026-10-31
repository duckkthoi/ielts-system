package com.ltmtu.ielts.user;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.ltmtu.ielts.support.IntegrationTest;

import tools.jackson.databind.JsonNode;

/** UC-05b: admin import giáo viên từ file — docs/05-api.md mục A.3. */
class TeacherImportIntegrationTest extends IntegrationTest {

	private ResultActions upload(String token, String filename, String content) throws Exception {
		return mvc.perform(multipart("/api/admin/teachers/import")
				.file(new MockMultipartFile("file", filename, "text/csv", content.getBytes(StandardCharsets.UTF_8)))
				.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	private JsonNode data(ResultActions result) throws Exception {
		return json.readTree(result.andExpect(apiStatus(200)).andReturn().getResponse().getContentAsString())
				.get("data");
	}

	@Test
	@DisplayName("UC-05b: chỉ admin import được")
	void onlyAdmin() throws Exception {
		String teacherToken = accessToken(newUser(Role.TEACHER));
		upload(teacherToken, "gv.csv", "ten_dang_nhap,ho_ten\ngv.x,X\n").andExpect(apiStatus(403));
	}

	@Test
	@DisplayName("UC-05b, BR-02: tạo tài khoản mới có mật khẩu tạm, bắt đổi mật khẩu; giáo viên đã có bị bỏ qua; dòng lỗi không làm hỏng dòng khác")
	void importRows() throws Exception {
		String adminToken = accessToken(newUser(Role.ADMIN));
		User existingTeacher = newUser(Role.TEACHER);
		User student = newUser(Role.STUDENT);
		String s = UUID.randomUUID().toString().substring(0, 8);
		String csv = """
				Tên đăng nhập;Họ và tên;Email
				gv.a%1$s;Nguyễn Văn A;a%1$s@truong.edu.vn
				GV.B%1$s;Trần Thị B;
				%2$s;Đã có;
				%3$s;Là sinh viên;
				gv a;Sai tên;
				gv.c%1$s;;
				gv.d%1$s;D;khong-phai-email
				gv.a%1$s;Trùng dòng;
				""".formatted(s, existingTeacher.getUsername().toUpperCase(), student.getUsername());

		JsonNode report = data(upload(adminToken, "gv.csv", csv));
		assertThat(report.get("totalRows").asInt()).isEqualTo(8);
		assertThat(report.get("created").asInt()).isEqualTo(2);
		assertThat(report.get("skipped").asInt()).isEqualTo(1);
		assertThat(report.get("errors")).hasSize(5);
		assertThat(report.get("errors").get(0).get("row").asInt()).isEqualTo(5);
		assertThat(report.get("errors").get(4).get("reason").asString()).contains("dòng 2");

		JsonNode first = report.get("accounts").get(0);
		assertThat(first.get("username").asString()).isEqualTo("gv.a" + s);
		String password = first.get("temporaryPassword").asString();
		assertThat(password).isNotBlank();

		User created = userRepository.findByUsernameIgnoreCase("gv.a" + s).orElseThrow();
		assertThat(created.getRole()).isEqualTo(Role.TEACHER);
		assertThat(created.isMustChangePassword()).isTrue();
		assertThat(created.getEmail()).isEqualTo("a" + s + "@truong.edu.vn");
		assertThat(userRepository.findByUsernameIgnoreCase("gv.b" + s).orElseThrow().getEmail()).isNull();
		JsonNode loggedIn = login("gv.a" + s, password);
		assertThat(loggedIn.get("user").get("mustChangePassword").asBoolean()).isTrue();

		JsonNode audit = data(mvc.perform(get("/api/admin/audit-logs").param("action", "IMPORT_TEACHERS")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))));
		assertThat(audit.get(0).toString()).contains("created").doesNotContain(password, "gv.a" + s);
	}

	@Test
	@DisplayName("UC-05b: file thiếu cột bắt buộc / sai đuôi → 400 BAD_FILE")
	void badFile() throws Exception {
		String adminToken = accessToken(newUser(Role.ADMIN));
		upload(adminToken, "gv.csv", "ma_sv,ho_ten\n1,A\n").andExpect(apiStatus(400));
		upload(adminToken, "gv.txt", "ten_dang_nhap,ho_ten\ngv.x,X\n").andExpect(apiStatus(400));
	}

	@Test
	@DisplayName("UC-05b: tải file mẫu Excel / CSV")
	void template() throws Exception {
		String adminToken = accessToken(newUser(Role.ADMIN));
		String csv = mvc.perform(get("/api/admin/teachers/import-template").param("format", "csv")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))).andReturn().getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		assertThat(csv).startsWith("﻿ten_dang_nhap,ho_ten,email");
		byte[] xlsx = mvc.perform(get("/api/admin/teachers/import-template")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))).andReturn().getResponse().getContentAsByteArray();
		assertThat(xlsx).startsWith((byte) 'P', (byte) 'K');
	}
}
