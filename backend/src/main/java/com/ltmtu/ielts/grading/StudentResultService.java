package com.ltmtu.ielts.grading;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.assignment.RubricCriterion;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.grading.BandCalculator.Weighted;
import com.ltmtu.ielts.grading.GradingEnums.GradeStatus;
import com.ltmtu.ielts.grading.GradingRules.ScoreCap;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.AssignmentResult;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.ChecklistMark;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.CriterionResult;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.ErrorCount;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.PartResult;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.Result;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.StudentInlineComment;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.VersionDetail;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.VersionSummary;
import com.ltmtu.ielts.submission.StudentSubmissionAccess;
import com.ltmtu.ielts.submission.StudentSubmissionAccess.MyAssignment;
import com.ltmtu.ielts.submission.Submission;
import com.ltmtu.ielts.submission.SubmissionStatus;
import com.ltmtu.ielts.submission.SubmissionVersion;

import lombok.RequiredArgsConstructor;

/**
 * Sinh viên xem lịch sử nộp và kết quả chấm (UC-26, docs/08 mục 4.5). Chỉ kết quả đã trả mới hiển thị (BR-13); gợi ý
 * sửa bị ẩn khi bài đang "Cần sửa", trừ khi bài tập bật hiển thị (BR-21).
 */
@Service
@RequiredArgsConstructor
public class StudentResultService {

	static final String ESTIMATE_NOTE = "Band tổng được ước lượng theo quy ước Task 2 gấp đôi Task 1; "
			+ "IELTS không công bố công thức chính xác.";
	static final String INCOMPLETE_NOTE = "Chưa đủ phần: band tổng chỉ có khi mọi phần đã được trả bài.";

	private final StudentSubmissionAccess submissions;
	private final GradeRepository gradeRepository;
	private final ErrorCodeRepository errorCodeRepository;

	/** Lịch sử nộp, mới nhất trước; có band ở các version đã được trả bài. */
	@Transactional(readOnly = true)
	public List<VersionSummary> versions(Long studentId, Long partId) {
		Submission s = submissions.mine(studentId, partId).orElse(null);
		if (s == null) {
			return List.of();
		}
		Map<Long, Grade> returned = returnedByVersionId(List.of(s.getId()));
		return submissions.versions(s).stream().map(v -> {
			Grade g = returned.get(v.getId());
			return new VersionSummary(v.getVersionNo(), v.getSubmittedAt(), v.getWordCount(), v.isLate(), g != null,
					g == null ? null : BandCalculator.display(g.getOverallScore()), g == null ? null : g.getReturnedAt());
		}).toList();
	}

	/** Nội dung một version + kết quả chấm nếu đã được trả (BR-13). */
	@Transactional(readOnly = true)
	public VersionDetail version(Long studentId, Long partId, int versionNo) {
		Submission s = submissions.mine(studentId, partId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy lần nộp này"));
		SubmissionVersion v = submissions.requireVersion(s, versionNo);
		Grade g = gradeRepository.findByVersionId(v.getId()).filter(Grade::isReturned).orElse(null);
		return new VersionDetail(v.getVersionNo(), v.getSubmittedAt(), v.getWordCount(), v.isLate(), v.getContent(),
				g == null ? null : toResult(g, s, v));
	}

	/**
	 * Band từng phần (lần nộp gần nhất đã được trả) và band cả bài theo trọng số các phần (BR-12). Band cả bài chỉ có
	 * khi mọi phần đã có kết quả.
	 */
	@Transactional(readOnly = true)
	public AssignmentResult assignmentResult(Long studentId, Long assignmentId) {
		MyAssignment mine = submissions.mineInAssignment(studentId, assignmentId);
		Assignment a = mine.assignment();
		Map<Long, Grade> latestByPart = new LinkedHashMap<>();
		if (!mine.submissions().isEmpty()) {
			Map<Long, Long> partOfSubmission = mine.submissions().stream()
					.collect(Collectors.toMap(Submission::getId, s -> s.getPart().getId()));
			for (Grade g : gradeRepository.findBySubmissionIds(partOfSubmission.keySet(), GradeStatus.RETURNED)) {
				latestByPart.merge(partOfSubmission.get(g.getVersion().getSubmission().getId()), g,
						(x, y) -> x.getVersion().getVersionNo() >= y.getVersion().getVersionNo() ? x : y);
			}
		}
		List<PartResult> parts = new ArrayList<>();
		List<Weighted> weighted = new ArrayList<>();
		for (AssignmentPart p : a.getParts()) {
			Grade g = latestByPart.get(p.getId());
			parts.add(new PartResult(p.getId(), p.getOrderNo(), p.getTitle(), p.getTaskType(), p.getWeight(),
					g == null ? null : g.getVersion().getVersionNo(),
					g == null ? null : BandCalculator.display(g.getOverallScore())));
			weighted.add(new Weighted(g == null ? null : g.getOverallScore(), p.getWeight()));
		}
		BigDecimal raw = BandCalculator.assignmentBand(weighted);
		return new AssignmentResult(a.getId(), a.getTitle(), parts, raw != null, BandCalculator.display(raw),
				raw != null ? ESTIMATE_NOTE : INCOMPLETE_NOTE);
	}

	// ─── Nội bộ ──────────────────────────────────────────────────────────────

	private Result toResult(Grade g, Submission s, SubmissionVersion v) {
		AssignmentPart part = s.getPart();
		List<RubricCriterion> criteria = part.getCriteria();
		Map<Long, GradeCriterionScore> scores = g.getScores().stream()
				.collect(Collectors.toMap(x -> x.getCriterion().getId(), Function.identity()));
		Map<Long, Boolean> checked = g.getMarks().stream()
				.collect(Collectors.toMap(m -> m.getItem().getId(), GradeChecklistMark::isChecked));
		List<CriterionResult> criterionResults = criteria.stream().map(c -> {
			GradeCriterionScore sc = scores.get(c.getId());
			ScoreCap cap = GradeUpdater.capOf(c, checked, v.getWordCount());
			return new CriterionResult(c.getId(), c.getCode(), c.getName(), c.getMaxScore(),
					sc == null ? null : sc.getScore(), sc == null ? null : sc.getComment(),
					cap.limited() ? cap.max() : null, cap.reason(),
					c.getChecklist().stream().map(i -> new ChecklistMark(i.getContent(), i.getKind(), i.getMaxBand(),
							checked.getOrDefault(i.getId(), false))).toList());
		}).toList();

		boolean hideSuggestions = s.getStatus() == SubmissionStatus.REVISION_REQUESTED
				&& v.getVersionNo() == s.getCurrentVersion()
				&& !part.getAssignment().isShowSuggestionsOnRevision();
		// Tên mã lỗi theo bộ mã của người chấm (mã hệ thống + mã riêng của giáo viên đó).
		Map<String, ErrorCode> codes = errorCodeRepository.findUsableBy(g.getGrader().getId()).stream()
				.collect(Collectors.toMap(e -> e.getCode().toUpperCase(Locale.ROOT), Function.identity(), (x, y) -> x));
		List<ResolvedComment> inline = g.getInlineComments().stream()
				.sorted(Comparator.comparingInt(InlineComment::getStartOffset))
				.map(ic -> new ResolvedComment(ic, ic.getErrorCode() == null ? null : codes.get(ic.getErrorCode())))
				.toList();
		return new Result(BandCalculator.display(g.getOverallScore()), g.getZeroReason(), g.getGeneralComment(),
				g.getGrader().getFullName(), g.getReturnedAt(), hideSuggestions, criterionResults,
				inline.stream().map(x -> x.toDto(hideSuggestions)).toList(), errorSummary(inline));
	}

	/** Nhận xét inline kèm mã lỗi tra được (null nếu không có mã, hoặc mã riêng đã bị xoá). */
	private record ResolvedComment(InlineComment ic, ErrorCode code) {

		StudentInlineComment toDto(boolean hideSuggestion) {
			RubricCriterion c = ic.getCriterion();
			return new StudentInlineComment(ic.getStartOffset(), ic.getEndOffset(), ic.getQuote(), ic.getComment(),
					ic.getErrorCode(), code == null ? null : code.getName(), c == null ? null : c.getCode(),
					c == null ? null : c.getName(), hideSuggestion ? null : ic.getSuggestion());
		}

		boolean countsAsError() {
			return ic.getErrorCode() != null && (code == null || !code.isPositive());
		}
	}

	/** Một ô đếm, kèm thứ tự tiêu chí trong rubric để sắp xếp (lỗi không gắn tiêu chí xếp cuối). */
	private record Tally(int criterionOrder, ErrorCount count) {
	}

	/** Bảng "Lỗi trong bài" (docs/08 mục 4.5): đếm theo tiêu chí rồi theo mã; bỏ mã khen; nhiều lỗi nhất lên trước. */
	private static List<ErrorCount> errorSummary(List<ResolvedComment> inline) {
		Map<String, Tally> tallies = new LinkedHashMap<>();
		for (ResolvedComment x : inline) {
			if (!x.countsAsError()) {
				continue;
			}
			RubricCriterion c = x.ic().getCriterion();
			ErrorCount one = new ErrorCount(c == null ? null : c.getCode(), c == null ? null : c.getName(),
					x.ic().getErrorCode(), x.code() == null ? null : x.code().getName(), 1);
			tallies.merge((c == null ? "-" : c.getId()) + "|" + x.ic().getErrorCode(),
					new Tally(c == null ? Integer.MAX_VALUE : c.getOrderNo(), one),
					(old, add) -> new Tally(old.criterionOrder(), new ErrorCount(old.count().criterionCode(),
							old.count().criterionName(), old.count().errorCode(), old.count().errorName(),
							old.count().count() + 1)));
		}
		return tallies.values().stream()
				.sorted(Comparator.comparingInt(Tally::criterionOrder)
						.thenComparing((Tally t) -> t.count().count(), Comparator.reverseOrder())
						.thenComparing(t -> t.count().errorCode()))
				.map(Tally::count)
				.toList();
	}

	private Map<Long, Grade> returnedByVersionId(List<Long> submissionIds) {
		return gradeRepository.findBySubmissionIds(submissionIds, GradeStatus.RETURNED).stream()
				.collect(Collectors.toMap(g -> g.getVersion().getId(), Function.identity()));
	}
}
