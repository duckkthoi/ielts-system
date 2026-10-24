package com.ltmtu.ielts.assignment;

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
}
