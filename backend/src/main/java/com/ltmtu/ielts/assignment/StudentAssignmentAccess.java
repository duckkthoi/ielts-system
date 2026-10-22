package com.ltmtu.ielts.assignment;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.common.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * Cửa duy nhất để module khác lấy bài tập / phần bài <b>phía sinh viên</b>: mọi truy cập đều qua điều kiện
 * "được giao và được thấy" (BR-04, BR-05). Bài không được giao → 404, không lộ việc bài tồn tại.
 */
@Service
@RequiredArgsConstructor
public class StudentAssignmentAccess {

	private final AssignmentRepository assignmentRepository;
	private final AssignmentPartRepository partRepository;

	@Transactional(readOnly = true)
	public List<Assignment> visibleAssignments(Long studentId, Instant now) {
		return assignmentRepository.findVisibleForStudent(studentId, now);
	}

	@Transactional(readOnly = true)
	public Assignment requireVisibleAssignment(Long assignmentId, Long studentId, Instant now) {
		return assignmentRepository.findVisibleForStudent(assignmentId, studentId, now)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy bài tập"));
	}

	/** Phần bài (kèm bài tập) mà sinh viên được thấy. */
	@Transactional(readOnly = true)
	public AssignmentPart requireVisiblePart(Long partId, Long studentId, Instant now) {
		AssignmentPart part = partRepository.findWithAssignment(partId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy bài tập"));
		requireVisibleAssignment(part.getAssignment().getId(), studentId, now);
		return part;
	}
}
