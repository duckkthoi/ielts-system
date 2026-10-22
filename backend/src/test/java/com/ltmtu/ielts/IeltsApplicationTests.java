package com.ltmtu.ielts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;

class IeltsApplicationTests extends IntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	@DisplayName("Khởi động: Flyway chạy đủ migration, JPA khớp schema")
	void contextLoads_migrationsApplied() {
		Integer failed = jdbc.queryForObject(
				"SELECT count(*) FROM flyway_schema_history WHERE success = false", Integer.class);
		assertThat(failed).isZero();
		assertThat(jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE version IS NOT NULL",
				String.class)).contains("1", "2", "3");
	}

	@Test
	@DisplayName("Khởi động: tự tạo tài khoản admin nếu chưa có")
	void adminSeeded() {
		assertThat(userRepository.existsByRole(Role.ADMIN)).isTrue();
	}

	@Test
	@DisplayName("Seed V2: có rubric mẫu IELTS Task 1 và Task 2, mỗi mẫu 4 tiêu chí kèm checklist")
	void ieltsRubricTemplatesSeeded() {
		var rows = jdbc.queryForList("""
				SELECT t.task_type, count(DISTINCT c.id) AS criteria, count(i.id) AS items
				FROM rubric_templates t
				JOIN rubric_template_criteria c ON c.template_id = t.id
				LEFT JOIN rubric_template_checklist_items i ON i.criterion_id = c.id
				WHERE t.owner_id IS NULL
				GROUP BY t.task_type ORDER BY t.task_type""");
		assertThat(rows).hasSize(2);
		assertThat(rows).allSatisfy(r -> {
			assertThat(((Number) r.get("criteria")).intValue()).isEqualTo(4);
			assertThat(((Number) r.get("items")).intValue()).isGreaterThanOrEqualTo(16);
		});
	}
}
