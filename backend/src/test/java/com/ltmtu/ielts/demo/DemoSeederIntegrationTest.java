package com.ltmtu.ielts.demo;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import com.ltmtu.ielts.assignment.AssignmentService;
import com.ltmtu.ielts.assignment.PublishService;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.submission.StudentSubmissionService;
import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.DemoAccountService;

import tools.jackson.databind.JsonNode;

/**
 * Dữ liệu mẫu của profile {@code demo} (bước 20). Bean {@link DemoSeeder} chỉ có ở profile demo nên test tự tạo, mỗi
 * lần chạy một tiền tố riêng (DB test dùng chung giữa các lần chạy).
 */
class DemoSeederIntegrationTest extends IntegrationTest {

	private static final String PASSWORD = "Demo@2026";

	@Autowired
	private DemoAccountService accounts;
	@Autowired
	private ClassroomService classrooms;
	@Autowired
	private AssignmentService assignments;
	@Autowired
	private PublishService publishing;
	@Autowired
	private StudentSubmissionService submissions;
	@Autowired
	private PlatformTransactionManager txManager;
	@Autowired
	private AppProperties props;

	private DemoSeeder seeder;
	private String prefix;

	@BeforeEach
	void setUp() {
		seeder = new DemoSeeder(accounts, classrooms, assignments, publishing, submissions, txManager, clock, props);
		prefix = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
	}

	@Test
	@DisplayName("Nạp 1 giáo viên, 2 lớp, bài đã giao có bài nộp, bài nháp, lớp kiểm thử tải; mật khẩu không bắt đổi")
	void seedsDemoData() throws Exception {
		assertThat(seeder.seed(new AppProperties.Demo(prefix, PASSWORD, 7, 3))).isTrue();

		JsonNode teacher = login("gv." + prefix.toLowerCase(), PASSWORD);
		assertThat(teacher.get("user").get("mustChangePassword").asBoolean()).isFalse();
		String token = teacher.get("accessToken").asString();

		JsonNode classes = data(token, "/api/teacher/classes?size=50");
		List<String> codes = new ArrayList<>();
		classes.forEach(c -> codes.add(c.get("code").asString() + ":" + c.get("memberCount").asLong()));
		assertThat(codes).containsExactlyInAnyOrder(prefix + "-A:7", prefix + "-B:7", prefix + "-LOAD:3");

		JsonNode list = data(token, "/api/teacher/assignments?sort=createdAt,asc");
		assertThat(list).hasSize(2);
		JsonNode week1 = list.get(0);
		assertThat(week1.get("status").asString()).isEqualTo("PUBLISHED");
		assertThat(week1.get("partCount").asInt()).isEqualTo(2);
		assertThat(week1.get("recipientCount").asLong()).isEqualTo(14);
		assertThat(week1.get("pendingGradingCount").asLong()).isEqualTo(6);
		assertThat(list.get(1).get("status").asString()).isEqualTo("DRAFT");

		// Sinh viên mẫu đăng nhập được ngay, thấy bài đã giao; sinh viên lớp tải cũng vậy.
		JsonNode student = login(prefix + "001", PASSWORD);
		assertThat(student.get("user").get("mustChangePassword").asBoolean()).isFalse();
		JsonNode todo = data(student.get("accessToken").asString(), "/api/student/assignments?tab=TODO");
		assertThat(todo.get("items")).hasSize(1);
		assertThat(login(prefix + "-T0003", PASSWORD).get("accessToken").asString()).isNotBlank();
	}

	@Test
	@DisplayName("Khởi động lại với dữ liệu mẫu đã có thì bỏ qua, không tạo trùng")
	void secondRunIsNoop() throws Exception {
		AppProperties.Demo cfg = new AppProperties.Demo(prefix, PASSWORD, 6, 0);
		assertThat(seeder.seed(cfg)).isTrue();
		assertThat(seeder.seed(cfg)).isFalse();

		String token = login("gv." + prefix.toLowerCase(), PASSWORD).get("accessToken").asString();
		assertThat(data(token, "/api/teacher/classes?size=50")).hasSize(2);
		assertThat(data(token, "/api/teacher/assignments")).hasSize(2);
	}

	private JsonNode data(String token, String url) throws Exception {
		String body = mvc.perform(get(url).header("Authorization", bearer(token))).andExpect(apiStatus(200))
				.andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("data");
	}
}
