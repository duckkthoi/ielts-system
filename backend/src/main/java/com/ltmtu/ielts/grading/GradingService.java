package com.ltmtu.ielts.grading;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.assignment.RubricCriterion;
import com.ltmtu.ielts.audit.AuditAction;
import com.ltmtu.ielts.audit.AuditService;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.grading.GradingEnums.GradeStatus;
import com.ltmtu.ielts.grading.GradingEnums.InlineKind;
import com.ltmtu.ielts.grading.GradingEnums.Resolution;
import com.ltmtu.ielts.grading.dto.GradingDtos.AssignmentInfo;
import com.ltmtu.ielts.grading.dto.GradingDtos.GradeDto;
import com.ltmtu.ielts.grading.dto.GradingDtos.GradingScreen;
import com.ltmtu.ielts.grading.dto.GradingDtos.InlineCommentDto;
import com.ltmtu.ielts.grading.dto.GradingDtos.PartInfo;
import com.ltmtu.ielts.grading.dto.GradingDtos.QueueRow;
import com.ltmtu.ielts.grading.dto.GradingDtos.RevisionRequest;
import com.ltmtu.ielts.grading.dto.GradingDtos.SaveGradeRequest;
import com.ltmtu.ielts.grading.dto.GradingDtos.StudentInfo;
import com.ltmtu.ielts.grading.dto.GradingDtos.SubmissionInfo;
import com.ltmtu.ielts.grading.dto.GradingDtos.VersionInfo;
import com.ltmtu.ielts.grading.dto.GradingDtos.VersionSummary;
import com.ltmtu.ielts.grading.dto.GradingDtos.Warning;
import com.ltmtu.ielts.submission.Submission;
import com.ltmtu.ielts.submission.SubmissionStatus;
import com.ltmtu.ielts.submission.SubmissionVersion;
import com.ltmtu.ielts.submission.TeacherSubmissionAccess;
import com.ltmtu.ielts.submission.dto.TeacherSubmissionDtos.QueueItem;
import com.ltmtu.ielts.user.User;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Chấm bài (UC-17), trả bài / yêu cầu sửa (UC-18) — SD-08. Chỉ giáo viên tạo bài tập mới chấm được (BR-15). */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradingService {

	private final TeacherSubmissionAccess submissions;
	private final GradeRepository gradeRepository;
	private final InlineCommentRepository inlineCommentRepository;
	private final ErrorCodeRepository errorCodeRepository;
	private final ClassroomService classroomService;
	private final UserQueryService userQueryService;
	private final ApplicationEventPublisher events;
	private final Clock clock;
	private final AuditService audit;

	/** Hàng đợi chấm, mặc định các bài "Đã nộp – chờ chấm", bài nộp trước xếp trước. */
	@Transactional(readOnly = true)
	public Page<QueueRow> queue(Long teacherId, SubmissionStatus status, Long assignmentId, Long partId, Long classId,
			String keyword, Pageable pageable) {
		if (classId != null) {
			classroomService.requireOwnedClassrooms(teacherId, List.of(classId));
		}
		Page<QueueItem> page = submissions.queue(teacherId, status == null ? SubmissionStatus.SUBMITTED : status,
				assignmentId, partId, classId, keyword, pageable);
		Map<Long, Grade> grades = gradesByVersionId(page.map(QueueItem::versionId).getContent());
		return page.map(i -> {
			Grade g = grades.get(i.versionId());
			return new QueueRow(i.submissionId(), i.status(), i.currentVersion(),
					new StudentInfo(i.studentId(), i.studentCode(), i.studentName()), i.assignmentId(),
					i.assignmentTitle(), i.partId(), i.partTitle(), i.partOrderNo(), i.taskType(), i.submittedAt(),
					i.late(), i.wordCount(), g == null ? null : g.getStatus(),
					g == null ? null : BandCalculator.display(g.getOverallScore()));
		});
	}

	/** Màn chấm của một version (mặc định version mới nhất). Version cũ chỉ xem, không sửa (UC-17 A3). */
	@Transactional(readOnly = true)
	public GradingScreen screen(Long teacherId, Long submissionId, Integer versionNo) {
		Submission s = submissions.requireOwned(submissionId, teacherId);
		return buildScreen(s, submissions.requireVersion(s, versionNo));
	}

	@Transactional(readOnly = true)
	public List<VersionSummary> versions(Long teacherId, Long submissionId) {
		return versionSummaries(submissions.requireOwned(submissionId, teacherId));
	}

	/**
	 * Lưu nháp chấm (SD-08): thay toàn bộ kết quả chấm của version mới nhất, kiểm tra điểm (BR-11, BR-17, BR-18),
	 * "Chấm 0" (BR-19), nhận xét inline (BR-21); band do server tính (BR-12). SUBMITTED → GRADED. Chưa chấm đủ tiêu
	 * chí vẫn lưu được (UC-17 E1).
	 */
	@Transactional
	public GradeDto saveGrade(Long teacherId, Long submissionId, SaveGradeRequest req) {
		Submission s = submissions.requireOwnedForUpdate(submissionId, teacherId);
		if (req.versionNo() != s.getCurrentVersion()) {
			throw ApiException.conflict("INVALID_STATE",
					"Chỉ chấm được lần nộp mới nhất (version " + s.getCurrentVersion() + "). Hãy tải lại trang");
		}
		requireGradable(s);
		SubmissionVersion v = submissions.requireVersion(s, req.versionNo());
		Grade g = gradeRepository.findByVersionId(v.getId()).orElse(null);
		if (g == null) {
			// Lưu trước để có id cho khoá kép của các dòng con (điểm, tick checklist).
			g = gradeRepository.saveAndFlush(Grade.start(v, userQueryService.requireUser(teacherId)));
		}
		List<RubricCriterion> criteria = s.getPart().getCriteria();
		Map<String, ErrorCode> codes = errorCodeRepository.findUsableBy(teacherId).stream()
				.collect(Collectors.toMap(e -> e.getCode().toUpperCase(Locale.ROOT), Function.identity(), (a, b) -> a));
		boolean paper = isPaper(s);
		GradeUpdater.apply(g, criteria, v.getContent(), v.getWordCount(), req, codes, clock.instant(), paper,
				paper ? members(s) : Map.of());
		submissions.markGraded(s);
		gradeRepository.saveAndFlush(g);
		log.info("Giáo viên {} lưu chấm bài nộp {} version {}", teacherId, s.getId(), v.getVersionNo());
		return GradeMapper.toDto(g, criteria, v.getWordCount(), paper);
	}

	/**
	 * So sánh 2 lần nộp (F5b): giáo viên đánh giá lại một lỗi của lần nộp CŨ — đã sửa / chưa sửa / đổi khác gợi ý,
	 * hoặc {@code null} để giao diện tự đoán. Chỉ lỗi (ERROR) của lần nộp đã có lần nộp sau. Không đổi kết quả chấm
	 * nên làm được cả khi đã trả bài.
	 */
	@Transactional
	public InlineCommentDto setResolution(Long teacherId, Long submissionId, Long commentId, Resolution resolution) {
		Submission s = submissions.requireOwned(submissionId, teacherId);
		InlineComment ic = inlineCommentRepository.findInSubmission(commentId, s.getId())
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy nhận xét"));
		if (ic.getKind() != InlineKind.ERROR) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Chỉ đánh giá được đánh dấu lỗi");
		}
		if (ic.getGrade().getVersion().getVersionNo() >= s.getCurrentVersion()) {
			throw ApiException.conflict("INVALID_STATE", "Lần nộp này chưa có bản nộp sau để so sánh");
		}
		ic.setResolution(resolution);
		return GradeMapper.toInlineDto(ic);
	}

	/** UC-18: trả bài. Cần đã chấm đủ mọi tiêu chí. Từ lúc này sinh viên mới thấy điểm (BR-13). */
	@Transactional
	public GradingScreen returnGrade(Long teacherId, Long submissionId) {
		Submission s = submissions.requireOwnedForUpdate(submissionId, teacherId);
		Grade g = requireCompleteGrade(s);
		submissions.markReturned(s);
		publish(g);
		events.publishEvent(new GradingEvents.Returned(s.getId(), s.getPart().getAssignment().getId(),
				s.getPart().getId(), s.getStudent().getId(), s.getCurrentVersion(),
				PaperScoreRules.display(g.getOverallScore(), isPaper(s)), s.getPart().getAssignment().getTitle(),
				s.getPart().getTitle(), submissions.memberIds(s), isPaper(s)));
		audit.record(teacherId, AuditAction.RETURN_GRADE, "SUBMISSION", s.getId(), Map.of("versionNo", s.getCurrentVersion()));
		log.info("Giáo viên {} trả bài nộp {} version {}", teacherId, s.getId(), s.getCurrentVersion());
		return buildScreen(s, g.getVersion());
	}

	/**
	 * UC-18 A1–A3: yêu cầu sửa. Trả kết quả hiện tại kèm lời nhắn và hạn sửa. Điều kiện: đã chấm đủ, còn lượt sửa
	 * (BR-10: số lần đã yêu cầu = currentVersion − 1), hạn sửa ở tương lai, bài tập chưa đóng.
	 */
	@Transactional
	public GradingScreen requestRevision(Long teacherId, Long submissionId, RevisionRequest req) {
		Submission s = submissions.requireOwnedForUpdate(submissionId, teacherId);
		Grade g = requireCompleteGrade(s);
		Assignment a = s.getPart().getAssignment();
		if (a.getStatus() == AssignmentStatus.CLOSED) {
			throw ApiException.conflict("INVALID_STATE", "Bài tập đã đóng. Mở lại bài tập trước khi yêu cầu sửa");
		}
		int used = s.getCurrentVersion() - 1;
		if (used >= a.getMaxRevisions()) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "REVISION_LIMIT",
					"Sinh viên đã dùng hết " + a.getMaxRevisions() + " lần sửa. Chỉ trả bài được");
		}
		Instant now = clock.instant();
		if (!req.dueAt().isAfter(now)) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Hạn sửa phải ở tương lai");
		}
		String note = req.note() == null || req.note().isBlank() ? null : req.note().trim();
		submissions.markRevisionRequested(s, note, req.dueAt());
		publish(g);
		events.publishEvent(new GradingEvents.RevisionRequested(s.getId(), a.getId(), s.getPart().getId(),
				s.getStudent().getId(), s.getCurrentVersion(), PaperScoreRules.display(g.getOverallScore(), isPaper(s)), note,
				req.dueAt(), a.getTitle(), s.getPart().getTitle(), submissions.memberIds(s)));
		audit.record(teacherId, AuditAction.REQUEST_REVISION, "SUBMISSION", s.getId(),
				Map.of("versionNo", s.getCurrentVersion(), "revision", used + 1));
		log.info("Giáo viên {} yêu cầu sửa bài nộp {} (lần {}/{})", teacherId, s.getId(), used + 1, a.getMaxRevisions());
		return buildScreen(s, g.getVersion());
	}

	// ─── Nội bộ ──────────────────────────────────────────────────────────────

	private static boolean isPaper(Submission s) {
		return s.getPart().getAssignment().isPaper();
	}

	/** Thành viên hiện tại của nhóm làm bài PAPER, theo id (để cộng / trừ riêng). */
	private Map<Long, User> members(Submission s) {
		Map<Long, User> map = new LinkedHashMap<>();
		submissions.memberIds(s).forEach(id -> map.put(id, userQueryService.requireUser(id)));
		return map;
	}

	private static void requireGradable(Submission s) {
		if (s.getStatus() != SubmissionStatus.SUBMITTED && s.getStatus() != SubmissionStatus.GRADED) {
			throw ApiException.conflict("INVALID_STATE", "Bài đã được trả, không sửa kết quả chấm được nữa");
		}
	}

	/** Bài đang "Đang chấm" và kết quả của version mới nhất đã đủ điểm mọi tiêu chí (UC-17 E1). */
	private Grade requireCompleteGrade(Submission s) {
		if (s.getStatus() == SubmissionStatus.SUBMITTED) {
			throw ApiException.conflict("INVALID_STATE", "Bài này chưa được chấm");
		}
		requireGradable(s);
		Grade g = gradeRepository.findByVersionId(submissions.requireVersion(s, null).getId())
				.orElseThrow(() -> ApiException.conflict("INVALID_STATE", "Bài này chưa được chấm"));
		List<String> missing = new ArrayList<>();
		g.getScores().stream().filter(x -> x.getScore() == null).forEach(x -> missing.add(x.getCriterion().getName()));
		if (g.getOverallScore() == null || !missing.isEmpty()) {
			throw ApiException.conflict("INVALID_STATE",
					"Chưa chấm đủ mọi tiêu chí" + (missing.isEmpty() ? "" : ": " + String.join(", ", missing)));
		}
		return g;
	}

	private void publish(Grade g) {
		g.setStatus(GradeStatus.RETURNED);
		g.setReturnedAt(clock.instant());
		gradeRepository.saveAndFlush(g);
	}

	private GradingScreen buildScreen(Submission s, SubmissionVersion v) {
		AssignmentPart part = s.getPart();
		Assignment a = part.getAssignment();
		List<RubricCriterion> criteria = part.getCriteria();
		User student = s.getStudent();
		Grade grade = gradeRepository.findByVersionId(v.getId()).orElse(null);
		boolean editable = v.getVersionNo() == s.getCurrentVersion()
				&& (s.getStatus() == SubmissionStatus.SUBMITTED || s.getStatus() == SubmissionStatus.GRADED);
		boolean complete = grade != null && grade.getOverallScore() != null;
		boolean canReturn = editable && complete && s.getStatus() == SubmissionStatus.GRADED;
		int revisionsUsed = s.getCurrentVersion() - 1;
		boolean canRevise = canReturn && revisionsUsed < a.getMaxRevisions() && a.getStatus() != AssignmentStatus.CLOSED;
		boolean paper = a.isPaper();
		List<StudentInfo> members = paper ? members(s).values().stream()
				.map(u -> new StudentInfo(u.getId(), u.getStudentCode(), u.getFullName())).toList() : List.of();
		return new GradingScreen(
				new SubmissionInfo(s.getId(), s.getStatus(), s.getCurrentVersion(), revisionsUsed,
						new StudentInfo(student.getId(), student.getStudentCode(), student.getFullName()),
						s.getRevisionNote(), s.getRevisionDueAt()),
				new AssignmentInfo(a.getId(), a.getTitle(), a.getStatus(), a.getMaxRevisions(),
						a.isShowSuggestionsOnRevision(), a.getKind()),
				new PartInfo(part.getId(), part.getOrderNo(), part.getTitle(), part.getTaskType(), part.getPrompt(),
						part.getImageUrl(), part.getMinWords(), part.getWeight()),
				new VersionInfo(v.getVersionNo(), v.getContent(), v.getWordCount(), v.getSubmittedAt(), v.isLate()),
				warnings(v.getWordCount(), part.getMinWords(), paper),
				GradeMapper.criteria(criteria),
				grade == null ? null : GradeMapper.toDto(grade, criteria, v.getWordCount(), paper),
				versionSummaries(s), editable, canReturn, canRevise, members);
	}

	/**
	 * docs/08 mục 3.3: ≤ 20 từ thì hệ thống tự giới hạn; thiếu số từ tối thiểu chỉ cảnh báo, không tự trừ điểm. Bài
	 * PAPER không áp giới hạn ≤ 20 từ (BR-31).
	 */
	private static List<Warning> warnings(int wordCount, Integer minWords, boolean paper) {
		List<Warning> list = new ArrayList<>();
		if (!paper && wordCount <= GradingRules.SHORT_ESSAY_WORDS) {
			list.add(new Warning("SHORT_ESSAY", "Bài chỉ có " + wordCount + " từ (≤ " + GradingRules.SHORT_ESSAY_WORDS
					+ " từ): mọi tiêu chí tối đa Band " + GradingRules.SHORT_ESSAY_MAX_BAND + " (BR-18)"));
		} else if (minWords != null && wordCount < minWords) {
			list.add(new Warning("UNDER_MIN_WORDS", "Bài có " + wordCount + " từ, ít hơn yêu cầu " + minWords
					+ " từ. Cân nhắc tick mục checklist liên quan; hệ thống không tự trừ điểm"));
		}
		return list;
	}

	private List<VersionSummary> versionSummaries(Submission s) {
		List<SubmissionVersion> versions = submissions.versions(s);
		Map<Long, Grade> grades = gradesByVersionId(versions.stream().map(SubmissionVersion::getId).toList());
		return versions.stream().map(v -> {
			Grade g = grades.get(v.getId());
			return new VersionSummary(v.getVersionNo(), v.getSubmittedAt(), v.getWordCount(), v.isLate(),
					g == null ? null : g.getStatus(), g == null ? null : PaperScoreRules.display(g.getOverallScore(), isPaper(s)));
		}).toList();
	}

	private Map<Long, Grade> gradesByVersionId(List<Long> versionIds) {
		if (versionIds.isEmpty()) {
			return Map.of();
		}
		return gradeRepository.findByVersionIdIn(versionIds).stream()
				.collect(Collectors.toMap(g -> g.getVersion().getId(), Function.identity()));
	}
}
