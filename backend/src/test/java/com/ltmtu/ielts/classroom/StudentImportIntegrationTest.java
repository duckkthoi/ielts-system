package com.ltmtu.ielts.classroom;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** UC-11, SD-03, BR-01..03 — import sinh viên từ file. */
class StudentImportIntegrationTest extends IntegrationTest {

	private User teacher;
	private String token;
	private long classId;
	private String classCode;

	@BeforeEach
	void setUpClass() throws Exception {
		teacher = newUser(Role.TEACHER);
		token = accessToken(teacher);
		classCode = "IMP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
		String body = mvc.perform(postJson("/api/teacher/classes", Map.of("code", classCode, "name", "Lớp import"))
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(apiStatus(201)).andReturn().getResponse().getContentAsString();
		classId = json.readTree(body).get("data").get("id").asLong();
	}

	/** Mã SV duy nhất cho mỗi lần chạy (DB test dùng chung). */
	private static String code() {
		return "SV" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
	}

	private ResultActions upload(String filename, byte[] content, boolean sendEmail, String asToken) throws Exception {
		return mvc.perform(multipart("/api/teacher/classes/" + classId + "/import")
				.file(new MockMultipartFile("file", filename, "application/octet-stream", content))
				.param("sendEmail", String.valueOf(sendEmail))
				.header(HttpHeaders.AUTHORIZATION, bearer(asToken)));
	}

	private JsonNode importCsv(String csv, boolean sendEmail) throws Exception {
		String body = upload("ds.csv", csv.getBytes(StandardCharsets.UTF_8), sendEmail, token)
				.andExpect(apiStatus(200)).andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("data");
	}

	/** Tải file tài khoản và đọc thành map: mã SV → mật khẩu tạm. */
	private Map<String, String> downloadCredentials(String credentialsToken) throws Exception {
		byte[] file = mvc.perform(get("/api/teacher/imports/" + credentialsToken + "/credentials")
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(apiStatus(200))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(".xlsx")))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
				.andReturn().getResponse().getContentAsByteArray();
		Map<String, String> result = new HashMap<>();
		DataFormatter f = new DataFormatter();
		try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(file))) {
			var sheet = wb.getSheetAt(0);
			for (int r = 1; r <= sheet.getLastRowNum(); r++) {
				var row = sheet.getRow(r);
				if (row != null && row.getCell(5) != null) {
					result.put(f.formatCellValue(row.getCell(1)), f.formatCellValue(row.getCell(5)));
				}
			}
		}
		return result;
	}

	@Test
	@DisplayName("BR-01..03: import đủ loại dòng — tạo mới, đã có, đã trong lớp, lỗi; dòng lỗi không chặn dòng khác")
	void mixedRows() throws Exception {
		User existing = newUser(Role.STUDENT);
		User alreadyMember = newUser(Role.STUDENT);
		mvc.perform(postJson("/api/teacher/classes/" + classId + "/members",
				Map.of("studentCodes", List.of(alreadyMember.getStudentCode())))
				.header(HttpHeaders.AUTHORIZATION, bearer(token)));
		String c1 = code();
		String c2 = code();
		// Giáo viên có tên đăng nhập trùng một mã SV hợp lệ → dòng đó phải báo lỗi, không ghi đè.
		User clash = newUser(Role.TEACHER);
		String clashCode = code();
		clash.setUsername(clashCode);
		userRepository.save(clash);

		JsonNode report = importCsv("MSSV;Họ và tên;Email\n"
				+ c1 + ";Nguyễn Văn An;an@sv.edu.vn\n"                       // dòng 2: tạo mới
				+ existing.getStudentCode().toLowerCase() + ";Tên khác;\n"     // dòng 3: đã có tài khoản (khác hoa thường)
				+ alreadyMember.getStudentCode() + ";X Y;\n"                   // dòng 4: đã trong lớp
				+ "21A!;Lê Cường;\n"                                           // dòng 5: mã sai
				+ c1.toLowerCase() + ";Trùng;\n"                               // dòng 6: trùng dòng 2
				+ c2 + ";Trần Thị Bình;\n"                                     // dòng 7: tạo mới (không email)
				+ clashCode + ";Mã trùng tên đăng nhập;\n", false);            // dòng 8: trùng tên đăng nhập

		assertThat(report.get("totalRows").asInt()).isEqualTo(7);
		assertThat(report.get("created").asInt()).isEqualTo(2);
		assertThat(report.get("existing").asInt()).isEqualTo(1);
		assertThat(report.get("skipped").asInt()).isEqualTo(1);
		assertThat(report.get("errors")).hasSize(3);
		assertThat(report.get("errors").get(0).get("row").asInt()).isEqualTo(5);
		assertThat(report.get("errors").get(1).get("reason").asString()).contains("dòng 2");
		assertThat(report.get("errors").get(2).get("row").asInt()).isEqualTo(8);
		assertThat(report.get("errors").get(2).get("reason").asString()).contains("tên đăng nhập");
		assertThat(report.get("emailQueued").asBoolean()).isFalse();
		assertThat(userRepository.findById(existing.getId()).orElseThrow().getFullName())
				.as("không ghi đè thông tin tài khoản đã có").isEqualTo(existing.getFullName());

		mvc.perform(get("/api/teacher/classes/" + classId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(jsonPath("$.data.memberCount").value(4));
	}

	@Test
	@DisplayName("UC-11: tải file tài khoản → đăng nhập được bằng mật khẩu tạm, bị bắt đổi mật khẩu; tải lần 2 → 404")
	void credentialsDownloadOnce_thenLogin() throws Exception {
		String c1 = code();
		JsonNode report = importCsv("ma_sv,ho_ten\n" + c1.toLowerCase() + ",Phạm Thu Dung\n", false);
		String credentialsToken = report.get("credentialsToken").asString();

		Map<String, String> creds = downloadCredentials(credentialsToken);
		assertThat(creds).containsOnlyKeys(c1);

		JsonNode session = login(c1.toLowerCase(), creds.get(c1));
		assertThat(session.get("user").get("username").asString()).isEqualTo(c1);
		assertThat(session.get("user").get("mustChangePassword").asBoolean()).isTrue();

		mvc.perform(get("/api/teacher/imports/" + credentialsToken + "/credentials")
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("File tài khoản: giáo viên khác không tải được; quá 15 phút → hết hạn")
	void credentialsOwnershipAndExpiry() throws Exception {
		String t1 = importCsv("ma_sv,ho_ten\n" + code() + ",Võ Minh Đức\n", false).get("credentialsToken").asString();
		String otherToken = accessToken(newUser(Role.TEACHER));
		mvc.perform(get("/api/teacher/imports/" + t1 + "/credentials").header(HttpHeaders.AUTHORIZATION,
				bearer(otherToken))).andExpect(apiStatus(404));
		downloadCredentials(t1); // người khác thử lấy không làm mất file của chủ

		String t2 = importCsv("ma_sv,ho_ten\n" + code() + ",Đặng Khánh Linh\n", false).get("credentialsToken").asString();
		clock.advance(ImportCredentialStore.TTL.plusSeconds(1));
		mvc.perform(get("/api/teacher/imports/" + t2 + "/credentials").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("Không có tài khoản mới → không có credentialsToken")
	void noNewAccounts_noToken() throws Exception {
		User existing = newUser(Role.STUDENT);
		JsonNode report = importCsv("ma_sv,ho_ten\n" + existing.getStudentCode() + ",A B\n", false);
		assertThat(report.get("created").asInt()).isZero();
		assertThat(report.get("credentialsToken").isNull()).isTrue();
	}

	@Test
	@DisplayName("FR-14: tick gửi email → chỉ sinh viên MỚI có email nhận thư, thư có tên đăng nhập + mật khẩu tạm")
	void sendEmail() throws Exception {
		String c1 = code();
		String c2 = code();
		String email = c1.toLowerCase() + "@sv.edu.vn";
		JsonNode report = importCsv("ma_sv,ho_ten,email\n" + c1 + ",Nguyễn Văn An," + email + "\n" + c2 + ",Không Email,\n",
				true);
		assertThat(report.get("emailQueued").asBoolean()).isTrue();
		String password = downloadCredentials(report.get("credentialsToken").asString()).get(c1);

		var mails = mail.awaitMatching(m -> List.of(m.getTo()).contains(email), 1, Duration.ofSeconds(5));
		assertThat(mails).hasSize(1);
		assertThat(mails.getFirst().getSubject()).isEqualTo("Tài khoản hệ thống IELTS Writing");
		assertThat(mails.getFirst().getText()).contains(c1, password, "Lớp import");
		assertThat(mail.sent()).noneMatch(m -> m.getText() != null && m.getText().contains(c2));
	}

	@Test
	@DisplayName("Import file .xlsx")
	void xlsxUpload() throws Exception {
		String c1 = code();
		byte[] file = StudentFileParserTest.xlsx(List.of(Map.of(0, "Mã SV", 1, "Họ tên"), Map.of(0, c1, 1, "Lê Hoàng Cường")));
		upload("danh-sach.xlsx", file, false, token)
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.created").value(1));
	}

	@Test
	@DisplayName("UC-11 E1: file sai định dạng / thiếu cột / rỗng → 400 BAD_FILE, không tạo gì")
	void badFiles() throws Exception {
		upload("ds.txt", "abc".getBytes(StandardCharsets.UTF_8), false, token)
				.andExpect(apiStatus(400)).andExpect(jsonPath("$.errorCode").value("BAD_FILE"));
		upload("ds.csv", "email\na@b.c\n".getBytes(StandardCharsets.UTF_8), false, token)
				.andExpect(apiStatus(400)).andExpect(jsonPath("$.errorMessage", containsString("ma_sv")));
		upload("ds.csv", new byte[0], false, token)
				.andExpect(apiStatus(400)).andExpect(jsonPath("$.errorCode").value("BAD_FILE"));
	}

	@Test
	@DisplayName("BR-15: import vào lớp của giáo viên khác → 404")
	void otherTeachersClass() throws Exception {
		String otherToken = accessToken(newUser(Role.TEACHER));
		upload("ds.csv", ("ma_sv,ho_ten\n" + code() + ",A B\n").getBytes(StandardCharsets.UTF_8), false, otherToken)
				.andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("Tải file mẫu .xlsx và .csv; file mẫu import lại được")
	void templates() throws Exception {
		byte[] xlsx = mvc.perform(get("/api/teacher/import-template").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(apiStatus(200))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("mau-danh-sach-sinh-vien.xlsx")))
				.andReturn().getResponse().getContentAsByteArray();
		assertThat(StudentFileParser.parse("mau.xlsx", new ByteArrayInputStream(xlsx))).hasSize(2);

		byte[] csv = mvc.perform(get("/api/teacher/import-template").param("format", "csv")
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(apiStatus(200)).andReturn().getResponse().getContentAsByteArray();
		assertThat(StudentFileParser.parse("mau.csv", new ByteArrayInputStream(csv)))
				.extracting(StudentFileParser.RawRow::fullName).containsExactly("Nguyễn Văn An", "Trần Thị Bình");
	}

	@Test
	@DisplayName("Đăng nhập không phân biệt hoa thường; tạo giáo viên trùng tên khác hoa thường → 409")
	void caseInsensitiveUsernames() throws Exception {
		User t = newUser(Role.TEACHER);
		login(t.getUsername().toUpperCase(), DEFAULT_PASSWORD);

		String adminToken = accessToken(newUser(Role.ADMIN));
		mvc.perform(postJson("/api/admin/teachers", Map.of("username", t.getUsername().toUpperCase(), "fullName", "X"))
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(apiStatus(409));
	}

	@Test
	@DisplayName("Thành viên lớp sau import hiển thị đúng và có cờ chưa đổi mật khẩu")
	void membersAfterImport() throws Exception {
		String c1 = code();
		importCsv("ma_sv,ho_ten\n" + c1 + ",Nguyễn Văn An\n", false);
		mvc.perform(get("/api/teacher/classes/" + classId + "/members").param("keyword", c1)
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(jsonPath("$.data", hasSize(1)))
				.andExpect(jsonPath("$.data[0].mustChangePassword").value(true));
	}
}
