package com.ltmtu.ielts.submission;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.Pageables;
import com.ltmtu.ielts.submission.dto.TeacherSubmissionDtos.QueueItem;

import lombok.RequiredArgsConstructor;

/**
 * Cửa duy nhất để module khác (chấm bài) đọc và chuyển trạng thái bài nộp <b>phía giáo viên</b>. Bài nộp thuộc bài
 * tập của giáo viên khác → 404 (BR-15), không lộ việc bài nộp tồn tại.
 */
@Service
@RequiredArgsConstructor
public class TeacherSubmissionAccess {

	private final SubmissionRepository submissionRepository;
	private final SubmissionVersionRepository versionRepository;

	/** Hàng đợi chấm. Chỉ các trạng thái đã có bài nộp; sắp theo thời điểm nộp nên bỏ qua {@code sort} của client. */
	@Transactional(readOnly = true)
	public Page<QueueItem> queue(Long teacherId, SubmissionStatus status, Long assignmentId, Long partId, Long classId,
			String keyword, Pageable pageable) {
		if (status == SubmissionStatus.NOT_STARTED || status == SubmissionStatus.DRAFT) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Hàng đợi chấm chỉ gồm các bài đã nộp");
		}
		return submissionRepository.gradingQueue(teacherId, status, assignmentId, partId, classId,
				Pageables.likePattern(keyword), PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
	}

	/** Bài nộp (kèm phần, bài tập, sinh viên) của giáo viên. */
	@Transactional(readOnly = true)
	public Submission requireOwned(Long submissionId, Long teacherId) {
		Submission s = submissionRepository.findWithPart(submissionId)
				.filter(x -> x.getPart().getAssignment().getTeacher().getId().equals(teacherId))
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy bài nộp"));
		if (s.getCurrentVersion() == 0) {
			throw ApiException.notFound("Bài này chưa được nộp");
		}
		return s;
	}

	/**
	 * Như {@link #requireOwned} và khoá dòng ({@code FOR UPDATE}) tới hết transaction của người gọi. Khoá
	 * <b>trước</b> rồi mới đọc, để trạng thái đọc được là trạng thái mới nhất sau khi giành được khoá.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public Submission requireOwnedForUpdate(Long submissionId, Long teacherId) {
		submissionRepository.findByIdForUpdate(submissionId);
		return requireOwned(submissionId, teacherId);
	}

	/** Một version của bài nộp; {@code versionNo = null} → version mới nhất. */
	@Transactional(readOnly = true)
	public SubmissionVersion requireVersion(Submission s, Integer versionNo) {
		int no = versionNo == null ? s.getCurrentVersion() : versionNo;
		return versionRepository.findBySubmissionIdAndVersionNo(s.getId(), no)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy lần nộp " + no));
	}

	/** Mọi version, mới nhất trước. */
	@Transactional(readOnly = true)
	public List<SubmissionVersion> versions(Submission s) {
		return versionRepository.findBySubmissionIdOrderByVersionNoDesc(s.getId());
	}

	// ─── Chuyển trạng thái (docs/06-trang-thai.md mục 2) ─────────────────────

	/** Lưu chấm: SUBMITTED → GRADED; đang GRADED thì giữ nguyên. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void markGraded(Submission s) {
		requireStatus(s, SubmissionStatus.SUBMITTED, SubmissionStatus.GRADED);
		s.setStatus(SubmissionStatus.GRADED);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void markReturned(Submission s) {
		requireStatus(s, SubmissionStatus.GRADED);
		s.setStatus(SubmissionStatus.RETURNED);
	}

	/** Yêu cầu sửa: hạn sửa được ưu tiên hơn hạn phần / hạn bài khi sinh viên nộp lại (BR-07). */
	@Transactional(propagation = Propagation.MANDATORY)
	public void markRevisionRequested(Submission s, String note, Instant dueAt) {
		requireStatus(s, SubmissionStatus.GRADED);
		s.setStatus(SubmissionStatus.REVISION_REQUESTED);
		s.setRevisionNote(note);
		s.setRevisionDueAt(dueAt);
	}

	private static void requireStatus(Submission s, SubmissionStatus... allowed) {
		for (SubmissionStatus a : allowed) {
			if (s.getStatus() == a) {
				return;
			}
		}
		throw ApiException.conflict("INVALID_STATE", "Thao tác không hợp lệ với bài nộp đang ở trạng thái " + s.getStatus());
	}
}
