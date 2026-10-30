package com.ltmtu.ielts.assignment;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.common.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * Cửa để module khác (tiến độ, thống kê, xuất Excel) lấy bài tập <b>phía giáo viên</b>. Bài của giáo viên khác → 404
 * (BR-15), không lộ việc bài tồn tại.
 */
@Service
@RequiredArgsConstructor
public class TeacherAssignmentAccess {

	private final AssignmentRepository assignmentRepository;
	private final AssignmentPartRepository partRepository;
	private final AssignmentTargetRepository targetRepository;

	/** Bài tập của giáo viên. Các phần ({@code getParts()}, theo thứ tự) nạp lười trong transaction của người gọi. */
	@Transactional(readOnly = true)
	public Assignment requireOwned(Long teacherId, Long assignmentId) {
		return assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy bài tập"));
	}

	/** Bài có phải của giáo viên này không (kiểm tra quyền kênh WebSocket, không ném lỗi). */
	@Transactional(readOnly = true)
	public boolean isOwned(Long teacherId, Long assignmentId) {
		return assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId).isPresent();
	}

	/** Người nhận bài hiện tại (giao trực tiếp ∪ thành viên lớp được giao) — module paper dùng để lập nhóm. */
	@Transactional(readOnly = true)
	public Set<Long> recipientIds(Long assignmentId) {
		return targetRepository.findRecipientIds(assignmentId);
	}

	/** Phần bài (kèm bài tập) của giáo viên; của người khác → 404. */
	@Transactional(readOnly = true)
	public AssignmentPart requireOwnedPart(Long teacherId, Long partId) {
		return partRepository.findWithAssignment(partId)
				.filter(p -> p.getAssignment().getTeacher().getId().equals(teacherId))
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy bài tập"));
	}

	/** Phần bài có thuộc bài của giáo viên này không (không ném lỗi). */
	@Transactional(readOnly = true)
	public boolean isPartOwned(Long teacherId, Long partId) {
		return partRepository.existsByIdAndAssignmentTeacherId(partId, teacherId);
	}
}
