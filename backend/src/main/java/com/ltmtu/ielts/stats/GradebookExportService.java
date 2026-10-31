package com.ltmtu.ielts.stats;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.assignment.RubricCriterion;
import com.ltmtu.ielts.assignment.TeacherAssignmentAccess;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.grading.BandCalculator;
import com.ltmtu.ielts.grading.BandCalculator.Weighted;
import com.ltmtu.ielts.grading.PaperScoreRules;
import com.ltmtu.ielts.stats.GradebookWorkbook.Column;
import com.ltmtu.ielts.stats.GradebookWorkbook.Line;
import com.ltmtu.ielts.stats.GradebookWorkbook.PartValues;
import com.ltmtu.ielts.stats.dto.ProgressDtos.ProgressCell;
import com.ltmtu.ielts.stats.dto.ProgressDtos.ProgressMatrix;
import com.ltmtu.ielts.stats.dto.ProgressDtos.ProgressRow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Xuất bảng điểm một bài ra Excel (UC-19, FR-66). Danh sách sinh viên và trạng thái lấy từ ma trận tiến độ; điểm lấy
 * từ kết quả đã trả mới nhất của mỗi phần (cùng cách chọn với màn kết quả của sinh viên và các thống kê).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradebookExportService {

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

	private static final String SCORES_SQL = "WITH " + StatsSql.PEOPLE + ", " + StatsSql.LATEST_RETURNED + """

			SELECT l.student_id, l.part_id, l.overall_score, gcs.criterion_id, gcs.score
			FROM latest l LEFT JOIN grade_criterion_scores gcs ON gcs.grade_id = l.grade_id""";

	private final ProgressService progressService;
	private final TeacherAssignmentAccess assignmentAccess;
	private final ClassroomService classroomService;
	private final NamedParameterJdbcTemplate jdbc;
	private final Clock clock;
	private final AppProperties props;

	public record ExportFile(String filename, byte[] content) {
	}

	/** FR-66: bảng điểm của bài, tuỳ chọn một lớp. Bài của người khác → 404; lớp không phải của mình → 400. */
	@Transactional(readOnly = true)
	public ExportFile export(Long teacherId, Long assignmentId, Long classId) {
		ProgressMatrix matrix = progressService.progress(teacherId, assignmentId, classId, null);
		Assignment a = assignmentAccess.requireOwned(teacherId, assignmentId);
		String classCode = classId == null ? null : classroomService.detail(teacherId, classId).code();

		Map<Key, BigDecimal> bands = new HashMap<>(); // (sinh viên, phần) → band phần chưa làm tròn
		Map<Key, BigDecimal> scores = new HashMap<>(); // (sinh viên, tiêu chí) → điểm
		jdbc.query(SCORES_SQL, StatsSql.params(assignmentId, classId), rs -> {
			long studentId = rs.getLong("student_id");
			bands.put(new Key(studentId, rs.getLong("part_id")), rs.getBigDecimal("overall_score"));
			long criterionId = rs.getLong("criterion_id");
			if (!rs.wasNull()) {
				scores.put(new Key(studentId, criterionId), rs.getBigDecimal("score"));
			}
		});

		boolean paper = a.isPaper();
		List<AssignmentPart> parts = a.getParts();
		List<Column> columns = parts.stream().map(p -> new Column(p.getTitle(),
				p.getCriteria().stream().map(GradebookExportService::criterionLabel).toList())).toList();
		List<Line> lines = new ArrayList<>();
		for (ProgressRow row : matrix.rows()) {
			long studentId = row.student().id();
			List<PartValues> values = new ArrayList<>();
			List<Weighted> weighted = new ArrayList<>();
			int late = 0;
			for (int i = 0; i < parts.size(); i++) {
				AssignmentPart p = parts.get(i);
				ProgressCell cell = row.cells().get(i);
				BigDecimal raw = bands.get(new Key(studentId, p.getId()));
				List<BigDecimal> criterionScores = p.getCriteria().stream()
						.map(c -> scores.get(new Key(studentId, c.getId()))).toList();
				values.add(new PartValues(criterionScores, PaperScoreRules.display(raw, paper), cell.status(), cell.late()));
				weighted.add(new Weighted(raw, p.getWeight()));
				if (cell.late()) {
					late++;
				}
			}
			lines.add(new Line(row.student().studentCode(), row.student().fullName(), values,
					PaperScoreRules.display(BandCalculator.assignmentBand(weighted), paper), late));
		}

		byte[] content = GradebookWorkbook.build(a.getTitle(), info(a, classCode, lines.size()), columns, lines,
				paper);
		log.info("Giáo viên {} xuất bảng điểm bài {} ({} sinh viên)", teacherId, assignmentId, lines.size());
		return new ExportFile("bang-diem-bai-" + assignmentId + (classCode == null ? "" : "-" + classCode) + ".xlsx",
				content);
	}

	private List<String> info(Assignment a, String classCode, int students) {
		List<String> info = new ArrayList<>();
		info.add("Thời điểm xuất: " + TIME.format(clock.instant().atZone(props.zone())) + " (giờ Việt Nam)");
		if (a.getDueAt() != null) {
			info.add("Hạn nộp: " + TIME.format(a.getDueAt().atZone(props.zone())));
		}
		info.add("Phạm vi: " + (classCode == null ? "mọi người nhận bài" : "lớp " + classCode) + " — " + students
				+ " sinh viên");
		info.add("Điểm lấy từ lần nộp gần nhất đã được trả bài; bài đang chấm chưa có điểm.");
		if (a.isPaper()) {
			info.add("Bài viết paper theo nhóm, thang 10, làm tròn 2 chữ số. Điểm của mỗi người = điểm của nhóm + điểm "
					+ "cộng / trừ riêng (nếu có). Điểm cả bài chỉ có khi mọi phần đã trả.");
		} else {
			info.add("Band phần và band cả bài đã làm tròn theo quy ước IELTS (.25 / .75). Band cả bài chỉ có khi mọi "
					+ "phần đã trả; Task 2 có trọng số gấp đôi Task 1 (ước lượng, IELTS không công bố công thức chính xác).");
		}
		info.add("Ô điểm để trống = chưa có điểm (khác với điểm 0).");
		return info;
	}

	private static String criterionLabel(RubricCriterion c) {
		return c.getCode() != null ? c.getCode().name() : c.getName();
	}

	private record Key(long studentId, long id) {
	}
}
