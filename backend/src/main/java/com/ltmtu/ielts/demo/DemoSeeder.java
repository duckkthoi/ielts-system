package com.ltmtu.ielts.demo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.assignment.AssignmentService;
import com.ltmtu.ielts.assignment.PublishService;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentDetail;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.ChecklistItemRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.CriterionDto;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.CriterionRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.PartRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.RubricTemplateDto;
import com.ltmtu.ielts.assignment.dto.PublishDtos.TargetsRequest;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.ClassroomRequest;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.submission.StudentSubmissionService;
import com.ltmtu.ielts.user.DemoAccountService;
import com.ltmtu.ielts.user.DemoAccountService.NewAccount;
import com.ltmtu.ielts.user.Role;

import lombok.extern.slf4j.Slf4j;

/**
 * Nạp dữ liệu mẫu khi khởi động với profile {@code demo} (bước 20): 1 giáo viên, 2 lớp, {@code 2 × studentsPerClass}
 * sinh viên, 1 bài 2 phần đã giao (vài sinh viên đã nộp Task 1 để màn chấm / tiến độ có dữ liệu) và 1 bài nháp để
 * giáo viên phát hành trong buổi demo. {@code loadStudents > 0} thì tạo thêm lớp kiểm thử tải (bước 14).
 * <p>
 * Đã có giáo viên mẫu thì bỏ qua — khởi động lại không tạo trùng. Mọi thứ đi qua service nghiệp vụ (kiểm tra rubric,
 * phát hành, nộp bài, thông báo) và nằm trong <b>một</b> transaction: lỗi giữa chừng thì không để lại dữ liệu dở.
 */
@Slf4j
@Component
@Profile("demo")
public class DemoSeeder implements ApplicationRunner {

	/** Số sinh viên lớp A đã nộp Task 1 / đang viết nháp Task 2. */
	private static final int SUBMITTED_TASK1 = 6;
	private static final int DRAFTING_TASK2 = 3;

	private final DemoAccountService accounts;
	private final ClassroomService classrooms;
	private final AssignmentService assignments;
	private final PublishService publishing;
	private final StudentSubmissionService submissions;
	private final TransactionTemplate tx;
	private final Clock clock;
	private final AppProperties props;

	public DemoSeeder(DemoAccountService accounts, ClassroomService classrooms, AssignmentService assignments,
			PublishService publishing, StudentSubmissionService submissions, PlatformTransactionManager txManager,
			Clock clock, AppProperties props) {
		this.accounts = accounts;
		this.classrooms = classrooms;
		this.assignments = assignments;
		this.publishing = publishing;
		this.submissions = submissions;
		this.tx = new TransactionTemplate(txManager);
		this.clock = clock;
		this.props = props;
	}

	@Override
	public void run(ApplicationArguments args) {
		seed(props.demo());
	}

	/** @return {@code false} nếu dữ liệu mẫu với tiền tố này đã có từ trước. */
	public boolean seed(AppProperties.Demo cfg) {
		String p = cfg.prefix().toUpperCase(Locale.ROOT);
		String teacherUsername = "gv." + p.toLowerCase(Locale.ROOT);
		if (accounts.exists(teacherUsername)) {
			log.info("Dữ liệu mẫu '{}' đã có, bỏ qua", p);
			return false;
		}
		tx.executeWithoutResult(status -> seedAll(cfg, p, teacherUsername));
		log.info("Đã nạp dữ liệu mẫu '{}': 2 lớp × {} sinh viên, lớp kiểm thử tải {} sinh viên", p,
				cfg.studentsPerClass(), cfg.loadStudents());
		return true;
	}

	private void seedAll(AppProperties.Demo cfg, String p, String teacherUsername) {
		Long teacherId = accounts.createAll(List.of(new NewAccount(Role.TEACHER, teacherUsername,
				"Giáo viên mẫu " + p, teacherUsername + "@ielts.local", null)), cfg.password()).getFirst();

		int n = cfg.studentsPerClass();
		List<Long> classA = students(cfg, p, 1, n);
		List<Long> classB = students(cfg, p, n + 1, n);
		Long classAId = newClass(teacherId, p + "-A", "IELTS Writing — Lớp A", p, 1, n);
		Long classBId = newClass(teacherId, p + "-B", "IELTS Writing — Lớp B", p, n + 1, n);

		Instant now = clock.instant();
		AssignmentDetail week1 = assignments.create(teacherId, new AssignmentRequest(
				"Tuần 1 — Biểu đồ đường và bài luận thảo luận",
				"Làm đủ 2 phần. Task 1 tối thiểu 150 từ, Task 2 tối thiểu 250 từ.", null,
				now.plus(Duration.ofDays(7)), true, 2, false,
				List.of(part(teacherId, "Task 1 — Biểu đồ đường", TaskType.WRITING_TASK1, DemoTexts.TASK1_PROMPT, 150,
						20), part(teacherId, "Task 2 — Thảo luận hai quan điểm", TaskType.WRITING_TASK2,
								DemoTexts.TASK2_PROMPT, 250, 40)), null, null, null, null));
		publishing.publish(teacherId, week1.id(), new TargetsRequest(Set.of(classAId, classBId), Set.of()));

		Long task1 = week1.parts().get(0).id();
		Long task2 = week1.parts().get(1).id();
		for (int i = 0; i < Math.min(SUBMITTED_TASK1, n); i++) {
			submissions.submit(classA.get(i), task1, DemoTexts.TASK1_ANSWERS.get(i % DemoTexts.TASK1_ANSWERS.size()), 0);
		}
		for (int i = 0; i < Math.min(DRAFTING_TASK2, n); i++) {
			submissions.saveDraft(classA.get(i), task2, DemoTexts.TASK2_DRAFT, null);
		}

		// Bài nháp: giáo viên chỉnh hạn rồi phát hành ngay trong buổi demo (học sinh online nhận thông báo realtime).
		assignments.create(teacherId, new AssignmentRequest("Tuần 2 — Bài luận Task 2", "Bài luyện thêm.", null,
				now.plus(Duration.ofDays(10)), true, 2, false, List.of(part(teacherId, "Task 2 — Lợi và hại",
						TaskType.WRITING_TASK2, DemoTexts.TASK2_PROMPT_WEEK2, 250, 40)), null, null, null, null));
		log.info("Dữ liệu mẫu: giáo viên {}, lớp {} và {}, {} + {} sinh viên", teacherId, classAId, classBId,
				classA.size(), classB.size());

		if (cfg.loadStudents() > 0) {
			loadTestClass(cfg, p, teacherId);
		}
	}

	/** Lớp {@code <P>-LOAD} với sinh viên {@code <P>-T0001}… cho kịch bản k6 (loadtest/). */
	private void loadTestClass(AppProperties.Demo cfg, String p, Long teacherId) {
		List<NewAccount> list = new ArrayList<>();
		List<String> codes = new ArrayList<>();
		for (int i = 1; i <= cfg.loadStudents(); i++) {
			String code = "%s-T%04d".formatted(p, i);
			codes.add(code);
			list.add(new NewAccount(Role.STUDENT, code, "Sinh viên tải " + i,
					code.toLowerCase(Locale.ROOT) + "@ielts.local", code));
		}
		accounts.createAll(list, cfg.password());
		Long classId = classrooms.create(teacherId, new ClassroomRequest(p + "-LOAD", "Lớp kiểm thử tải",
				"Chỉ dùng cho k6 (bước 14)")).id();
		classrooms.addMembers(teacherId, classId, codes);
	}

	private List<Long> students(AppProperties.Demo cfg, String p, int from, int count) {
		List<NewAccount> list = new ArrayList<>(count);
		for (int i = from; i < from + count; i++) {
			String code = studentCode(p, i);
			list.add(new NewAccount(Role.STUDENT, code, DemoTexts.studentName(i),
					code.toLowerCase(Locale.ROOT) + "@ielts.local", code));
		}
		return accounts.createAll(list, cfg.password());
	}

	private Long newClass(Long teacherId, String code, String name, String p, int from, int count) {
		Long id = classrooms.create(teacherId, new ClassroomRequest(code, name, "Lớp dữ liệu mẫu")).id();
		List<String> codes = new ArrayList<>(count);
		for (int i = from; i < from + count; i++) {
			codes.add(studentCode(p, i));
		}
		classrooms.addMembers(teacherId, id, codes);
		return id;
	}

	private static String studentCode(String p, int n) {
		return "%s%03d".formatted(p, n);
	}

	/** Phần bài dùng rubric IELTS mẫu của hệ thống (4 tiêu chí, checklist Đạt / Giới hạn điểm — docs/08). */
	private PartRequest part(Long teacherId, String title, TaskType type, String prompt, int minWords, int minutes) {
		RubricTemplateDto template = assignments.templates(teacherId, type).stream().filter(RubricTemplateDto::system)
				.findFirst().orElseThrow(() -> new IllegalStateException("Thiếu rubric mẫu " + type));
		List<CriterionRequest> criteria = template.criteria().stream().map(DemoSeeder::criterion).toList();
		return new PartRequest(title, type, prompt, null, minWords, minutes, null, null, criteria, null, null, null);
	}

	private static CriterionRequest criterion(CriterionDto c) {
		List<ChecklistItemRequest> checklist = c.checklist().stream()
				.map(i -> new ChecklistItemRequest(i.content(), i.kind(), i.maxBand())).toList();
		return new CriterionRequest(c.code(), c.name(), c.description(), c.maxScore(), c.step(), c.weight(), checklist,
				null);
	}
}
