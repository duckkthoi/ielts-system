package com.ltmtu.ielts.submission;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.assignment.StudentAssignmentAccess;
import com.ltmtu.ielts.common.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * Cửa để module khác (kết quả chấm) đọc bài làm <b>của chính sinh viên</b>. Mọi truy cập đều qua điều kiện "được giao
 * và được thấy" (BR-04, BR-05); không được giao → 404.
 */
@Service
@RequiredArgsConstructor
public class StudentSubmissionAccess {

	private final StudentAssignmentAccess access;
	private final SubmissionRepository submissionRepository;
	private final SubmissionVersionRepository versionRepository;
	private final Clock clock;

	/** Bài làm của tôi (bài PAPER: của nhóm tôi — BR-28) cho một phần; chưa lưu nháp / nộp lần nào → rỗng. */
	@Transactional(readOnly = true)
	public Optional<Submission> mine(Long studentId, Long partId) {
		access.requireVisiblePart(partId, studentId, clock.instant());
		return submissionRepository.findMine(studentId, partId);
	}

	/** Bài tập được thấy (kèm các phần) và mọi bài làm của tôi trong bài đó. */
	@Transactional(readOnly = true)
	public MyAssignment mineInAssignment(Long studentId, Long assignmentId) {
		Assignment a = access.requireVisibleAssignment(assignmentId, studentId, clock.instant());
		List<Long> partIds = a.getParts().stream().map(AssignmentPart::getId).toList();
		return new MyAssignment(a, partIds.isEmpty() ? List.of()
				: submissionRepository.findMineInParts(studentId, partIds));
	}

	public record MyAssignment(Assignment assignment, List<Submission> submissions) {
	}

	/** Mọi version, mới nhất trước. */
	@Transactional(readOnly = true)
	public List<SubmissionVersion> versions(Submission s) {
		return versionRepository.findBySubmissionIdOrderByVersionNoDesc(s.getId());
	}

	@Transactional(readOnly = true)
	public SubmissionVersion requireVersion(Submission s, int versionNo) {
		return versionRepository.findBySubmissionIdAndVersionNo(s.getId(), versionNo)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy lần nộp này"));
	}
}
