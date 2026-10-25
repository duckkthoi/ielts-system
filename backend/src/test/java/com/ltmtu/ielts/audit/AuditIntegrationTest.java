package com.ltmtu.ielts.audit;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Nhật ký hệ thống (UC-07, FR-08): các thao tác được ghi, tra cứu và lọc ở API admin. */
class AuditIntegrationTest extends IntegrationTest {

	private static final AtomicInteger IP_SEQ = new AtomicInteger(1);

	private String admin;

	@BeforeEach
	void setUp() throws Exception {
		admin = accessToken(newUser(Role.ADMIN));
	}

	private JsonNode logs(MockHttpServletRequestBuilder req) throws Exception {
		String body = mvc.perform(req.header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(apiStatus(200)).andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("data");
	}

	private MockHttpServletRequestBuilder search(String... params) {
		MockHttpServletRequestBuilder req = get("/api/admin/audit-logs");
		for (int i = 0; i < params.length; i += 2) {
			req.param(params[i], params[i + 1]);
		}
		return req;
	}

	@Test
	@DisplayName("Đăng nhập: thành công / sai mật khẩu / bị chặn đều ghi, có IP và tên đăng nhập, không có mật khẩu")
	void login() throws Exception {
		User u = newUser(Role.TEACHER);
		String since = clock.instant().minusSeconds(1).toString();
		String ip = "10.88." + IP_SEQ.getAndIncrement() + "." + (System.nanoTime() % 200 + 1);
		for (int i = 0; i < 5; i++) {
			mvc.perform(postJson("/api/auth/login", Map.of("username", u.getUsername(), "password", "MatKhauSai!"))
					.with(r -> {
						r.setRemoteAddr(ip);
						return r;
					})).andExpect(apiStatus(401));
		}
		mvc.perform(postJson("/api/auth/login", Map.of("username", u.getUsername(), "password", DEFAULT_PASSWORD))
				.with(r -> {
					r.setRemoteAddr(ip);
					return r;
				})).andExpect(apiStatus(429));
		loginThrottle.clear();
		accessToken(u);

		JsonNode failed = logs(search("action", "LOGIN_FAILED", "ip", ip, "from", since));
		assertThat(failed).hasSize(5);
		assertThat(failed.get(0).get("actorId").asLong()).isEqualTo(u.getId());
		assertThat(failed.get(0).get("detail").get("username").asString()).isEqualTo(u.getUsername());
		assertThat(failed.get(0).get("detail").get("reason").asString()).isEqualTo("BAD_CREDENTIALS");
		assertThat(failed.toString()).doesNotContain("MatKhauSai");

		JsonNode blocked = logs(search("action", "LOGIN_BLOCKED", "ip", ip, "from", since));
		assertThat(blocked).hasSize(1);
		assertThat(blocked.get(0).get("actorId").isNull()).isTrue();

		JsonNode ok = logs(search("action", "LOGIN", "actorId", String.valueOf(u.getId())));
		assertThat(ok).hasSize(1);
		assertThat(ok.get(0).get("actorUsername").asString()).isEqualTo(u.getUsername());
		assertThat(ok.get(0).get("actorName").asString()).isEqualTo(u.getFullName());
		assertThat(ok.get(0).get("ip").asString()).isEqualTo("127.0.0.1");
	}

	@Test
	@DisplayName("Import HTTP, khoá / mở khoá tài khoản được ghi kèm đối tượng và số liệu tóm tắt (không có mã SV, tên)")
	void businessActions() throws Exception {
		User teacher = newUser(Role.TEACHER);
		String token = accessToken(teacher);
		String classBody = mvc.perform(postJson("/api/teacher/classes",
				Map.of("code", "AU-" + UUID.randomUUID().toString().substring(0, 6), "name", "Lớp nhật ký"))
				.header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn().getResponse().getContentAsString();
		long classId = json.readTree(classBody).get("data").get("id").asLong();
		String code = "AU" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		MockMultipartFile file = new MockMultipartFile("file", "ds.csv", "text/csv",
				("ma_sv,ho_ten\n" + code + ",Nguyễn Nhật Ký\n").getBytes(StandardCharsets.UTF_8));
		mvc.perform(multipart("/api/teacher/classes/" + classId + "/import").file(file)
				.header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(apiStatus(200));

		JsonNode imports = logs(search("actorId", String.valueOf(teacher.getId()), "action", "IMPORT_STUDENTS"));
		assertThat(imports).hasSize(1);
		assertThat(imports.get(0).get("targetType").asString()).isEqualTo("CLASSROOM");
		assertThat(imports.get(0).get("targetId").asLong()).isEqualTo(classId);
		assertThat(imports.get(0).get("detail").get("created").asInt()).isEqualTo(1);
		assertThat(imports.toString()).doesNotContain(code).doesNotContain("Nhật Ký");

		mvc.perform(post("/api/admin/users/" + teacher.getId() + "/lock").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(apiStatus(200));
		mvc.perform(post("/api/admin/users/" + teacher.getId() + "/unlock").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(apiStatus(200));
		assertThat(logs(search("action", "LOCK_USER")).get(0).get("targetId").asLong()).isEqualTo(teacher.getId());
		assertThat(logs(search("action", "UNLOCK_USER")).get(0).get("targetId").asLong()).isEqualTo(teacher.getId());
	}

	@Test
	@DisplayName("Lọc theo khoảng thời gian; khoảng sai → 400; không phải admin → 403")
	void filters() throws Exception {
		User u = newUser(Role.STUDENT);
		accessToken(u);
		Instant at = clock.instant();
		String actor = String.valueOf(u.getId());
		assertThat(logs(search("actorId", actor, "from", at.minusSeconds(1).toString(), "to", at.plusSeconds(1).toString())))
				.hasSize(1);
		assertThat(logs(search("actorId", actor, "from", at.plus(Duration.ofMinutes(1)).toString()))).isEmpty();
		assertThat(logs(search("actorId", actor)).get(0).get("action").asString()).isEqualTo("LOGIN");

		mvc.perform(search("from", at.toString(), "to", at.toString()).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(apiStatus(400));
		mvc.perform(search().header(HttpHeaders.AUTHORIZATION, bearer(accessToken(u)))).andExpect(apiStatus(403));
	}

	@Test
	@DisplayName("Ghi chung transaction: thao tác thất bại thì không có dòng nhật ký")
	void failedActionNotLogged() throws Exception {
		User teacher = newUser(Role.TEACHER);
		String token = accessToken(teacher);
		mvc.perform(postJson("/api/teacher/assignments/999999999/publish", Map.of("studentIds", List.of(1)))
				.header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(apiStatus(404));
		assertThat(logs(search("actorId", String.valueOf(teacher.getId()), "action", "PUBLISH_ASSIGNMENT"))).isEmpty();
	}
}
