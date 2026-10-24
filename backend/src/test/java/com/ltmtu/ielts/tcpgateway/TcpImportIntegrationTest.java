package com.ltmtu.ielts.tcpgateway;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;

import com.ltmtu.ielts.audit.AuditLogRepository;
import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

/**
 * Server TCP import sinh viên với socket thật tới cổng ngẫu nhiên (docs/05 phần C). Cấu hình test: 3 luồng, timeout
 * đọc 2 giây.
 */
class TcpImportIntegrationTest extends IntegrationTest {

	@Autowired
	private TcpImportServer server;

	@Autowired
	private AuditLogRepository auditRepository;

	private User teacherUser;
	private String teacher;
	private String classCode;
	private long classId;

	@BeforeEach
	void setUp() throws Exception {
		teacherUser = newUser(Role.TEACHER);
		teacher = accessToken(teacherUser);
		classCode = "TCP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
		classId = createClass(teacher, classCode);
	}

	@AfterEach
	void waitIdle() throws InterruptedException {
		// Các phiên của test trước phải kết thúc hẳn, để không chiếm chỗ của test sau (pool chỉ 3 luồng).
		long until = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		while (server.activeSessions() > 0 && System.nanoTime() < until) {
			Thread.sleep(20);
		}
	}

	@Test
	@DisplayName("Phiên đầy đủ: AUTH → PING → IMPORT … EMAIL → PROGRESS từng dòng, DONE, ERRLINE, CRED, END; tài khoản mới đăng nhập được; nhật ký TCP_IMPORT có IP")
	void fullImport() throws Exception {
		User existing = newUser(Role.STUDENT);
		String codeA = newCode();
		String codeB = newCode();
		String emailA = codeA.toLowerCase() + "@sv.test";
		String csv = "ma_sv,ho_ten,email\n" + codeA + ",Nguyễn Văn A," + emailA + "\n" + codeB + ",Trần Thị B,\n"
				+ existing.getStudentCode() + ",Đã có tài khoản,\n" + "21A,Lê C,\n";

		try (Client c = new Client()) {
			c.send("AUTH " + teacher);
			assertThat(c.read()).isEqualTo("OK");
			c.send("PING");
			assertThat(c.read()).isEqualTo("PONG");
			c.importCsv(classCode.toLowerCase(), csv, true);
			List<String> lines = c.readAll();
			assertThat(lines).startsWith("PROGRESS 1/4", "PROGRESS 2/4", "PROGRESS 3/4", "PROGRESS 4/4",
					"DONE created=2 existing=1 skipped=0 errors=1");
			assertThat(lines.get(5)).startsWith("ERRLINE 5 Mã sinh viên không hợp lệ");
			assertThat(lines.get(6)).startsWith("CRED " + codeA + "|Nguyễn Văn A|");
			assertThat(lines.get(7)).startsWith("CRED " + codeB + "|Trần Thị B|");
			assertThat(lines.get(8)).isEqualTo("END");
			assertThat(lines).hasSize(9);

			String passwordA = lines.get(6).split("\\|")[2];
			assertThat(login(codeA, passwordA).get("user").get("mustChangePassword").asBoolean()).isTrue();
		}
		mvc.perform(get("/api/teacher/classes/" + classId + "/members").header(HttpHeaders.AUTHORIZATION, bearer(teacher)))
				.andExpect(jsonPath("$.totalRecord").value(3));
		assertThat(mail.awaitMatching(m -> List.of(m.getTo()).contains(emailA), 1, Duration.ofSeconds(5))).hasSize(1);

		var logs = auditRepository.search(teacherUser.getId(), "TCP_IMPORT", null, null, null, PageRequest.of(0, 5));
		assertThat(logs.getContent()).singleElement().satisfies(l -> {
			assertThat(l.getTargetId()).isEqualTo(classId);
			assertThat(l.getIp()).isEqualTo("127.0.0.1");
			assertThat(l.getDetail()).contains("\"created\": 2").doesNotContain(codeA);
		});
	}

	@Test
	@DisplayName("Xác thực: token sai → ERR 401, sinh viên → ERR 403, tài khoản phải đổi mật khẩu → ERR 403, lệnh trước AUTH → ERR 400; đều đóng kết nối")
	void authErrors() throws Exception {
		assertThat(session("AUTH abc.def.ghi")).containsExactly("ERR 401 Token sai hoặc hết hạn");
		assertThat(session("AUTH " + accessToken(newUser(Role.STUDENT)))).singleElement().asString().startsWith("ERR 403");
		assertThat(session("AUTH " + login(newUser(Role.TEACHER, true).getUsername(), DEFAULT_PASSWORD)
				.get("accessToken").asString())).singleElement().asString().startsWith("ERR 403");
		assertThat(session("IMPORT " + classCode + " 10")).containsExactly("ERR 400 Cần AUTH trước");
	}

	@Test
	@DisplayName("Đã xác thực: lệnh sai → ERR 400 nhưng giữ kết nối; QUIT → BYE rồi đóng")
	void badCommandKeepsSession() throws Exception {
		try (Client c = new Client()) {
			c.send("AUTH " + teacher);
			assertThat(c.read()).isEqualTo("OK");
			c.send("HELLO");
			assertThat(c.read()).startsWith("ERR 400");
			c.send("IMPORT " + classCode + " abc");
			assertThat(c.read()).startsWith("ERR 400");
			c.send("PING");
			assertThat(c.read()).isEqualTo("PONG");
			c.send("QUIT");
			assertThat(c.readAll()).containsExactly("BYE");
		}
	}

	@Test
	@DisplayName("IMPORT bị từ chối trước khi nhận dữ liệu: lớp của giáo viên khác → 403, không có lớp → 404, quá 5 MB → 413; CSV thiếu cột → 415")
	void importErrors() throws Exception {
		String otherClass = "TCP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
		createClass(accessToken(newUser(Role.TEACHER)), otherClass);
		assertThat(session("AUTH " + teacher, "IMPORT " + otherClass + " 10")).last().asString().startsWith("ERR 403");
		assertThat(session("AUTH " + teacher, "IMPORT KHONG-CO-LOP 10")).last().asString().startsWith("ERR 404");
		assertThat(session("AUTH " + teacher, "IMPORT " + classCode + " 6000000")).last().asString().startsWith("ERR 413");

		try (Client c = new Client()) {
			c.send("AUTH " + teacher);
			c.read();
			c.importCsv(classCode, "ten,lop\nA,B\n", false);
			assertThat(c.readAll()).singleElement().asString().startsWith("ERR 415 File thiếu cột bắt buộc");
		}
	}

	@Test
	@DisplayName("Admin import được vào lớp của bất kỳ giáo viên nào")
	void adminImportsAnyClass() throws Exception {
		try (Client c = new Client()) {
			c.send("AUTH " + accessToken(newUser(Role.ADMIN)));
			assertThat(c.read()).isEqualTo("OK");
			c.importCsv(classCode, "ma_sv,ho_ten\n" + newCode() + ",Phạm D\n", false);
			assertThat(c.readAll()).contains("DONE created=1 existing=0 skipped=0 errors=0", "END");
		}
	}

	@Test
	@DisplayName("Không gửi gì trong thời gian chờ → ERR 408 rồi đóng")
	void readTimeout() throws Exception {
		try (Client c = new Client()) {
			c.send("AUTH " + teacher);
			assertThat(c.read()).isEqualTo("OK");
			assertThat(c.readAll()).containsExactly("ERR 408 Quá thời gian chờ dữ liệu");
		}
	}

	@Test
	@DisplayName("Đủ 3 phiên đang chạy → kết nối thứ 4 nhận ngay ERR 503 rồi bị đóng; phiên xong thì nhận lại bình thường")
	void busy() throws Exception {
		List<Client> busy = new ArrayList<>();
		try {
			for (int i = 0; i < 3; i++) {
				Client c = new Client();
				busy.add(c);
				c.send("AUTH " + teacher);
				assertThat(c.read()).isEqualTo("OK");
			}
			try (Client fourth = new Client()) {
				assertThat(fourth.readAll()).containsExactly("ERR 503 Server bận, hãy thử lại sau");
			}
		} finally {
			for (Client c : busy) {
				c.send("QUIT");
				c.close();
			}
		}
		waitIdle();
		assertThat(session("AUTH " + teacher, "PING", "QUIT")).containsExactly("OK", "PONG", "BYE");
	}

	// ─── Hỗ trợ ─────────────────────────────────────────────────────────────

	private long createClass(String token, String code) throws Exception {
		String body = mvc.perform(postJson("/api/teacher/classes", Map.of("code", code, "name", "Lớp TCP"))
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(apiStatus(201)).andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("data").get("id").asLong();
	}

	private static String newCode() {
		return "TC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
	}

	/** Gửi các dòng rồi đọc mọi phản hồi tới khi server đóng kết nối. */
	private List<String> session(String... lines) throws IOException {
		try (Client c = new Client()) {
			for (String line : lines) {
				c.send(line);
			}
			return c.readAll();
		}
	}

	private final class Client implements AutoCloseable {

		private final Socket socket;
		private final OutputStream out;
		private final BufferedReader in;

		Client() throws IOException {
			socket = new Socket("localhost", server.getLocalPort());
			socket.setSoTimeout(10_000);
			out = socket.getOutputStream();
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
		}

		void send(String line) throws IOException {
			out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
			out.flush();
		}

		void importCsv(String code, String csv, boolean email) throws IOException {
			byte[] data = csv.getBytes(StandardCharsets.UTF_8);
			send("IMPORT " + code + " " + data.length + (email ? " EMAIL" : ""));
			out.write(data);
			out.flush();
		}

		String read() throws IOException {
			return in.readLine();
		}

		List<String> readAll() throws IOException {
			List<String> lines = new ArrayList<>();
			String line;
			while ((line = in.readLine()) != null) {
				lines.add(line);
			}
			return lines;
		}

		@Override
		public void close() throws IOException {
			socket.close();
		}
	}
}
