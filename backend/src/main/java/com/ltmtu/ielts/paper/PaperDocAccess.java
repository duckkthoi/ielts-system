package com.ltmtu.ielts.paper;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.Role;

import lombok.RequiredArgsConstructor;

/**
 * Quyền trên tài liệu paper (BR-28, BR-32): thành viên <b>hiện tại</b> của nhóm đọc + sửa; giáo viên chủ bài chỉ đọc.
 * Không phụ thuộc {@code SimpMessagingTemplate} để luật kênh STOMP dùng được (AI-CODING-RULES mục 10).
 */
@Service
@RequiredArgsConstructor
public class PaperDocAccess {

	/** Bài làm còn sửa được — cùng tập với {@code SubmissionStatus.isOpenForStudent}. */
	private static final Set<String> OPEN_STATUSES = Set.of("NOT_STARTED", "DRAFT", "REVISION_REQUESTED");

	private final PaperDocumentRepository documentRepository;
	private final PaperGroupMemberRepository memberRepository;

	/** Tài liệu mà sinh viên là thành viên nhóm; không phải → 404 (không lộ tài liệu tồn tại). */
	@Transactional(readOnly = true)
	public PaperDocument requireMember(Long studentId, Long documentId) {
		PaperDocument d = documentRepository.findFull(documentId).orElse(null);
		if (d == null || !isMember(studentId, d)) {
			throw ApiException.notFound("Không tìm thấy tài liệu");
		}
		return d;
	}

	/** Tài liệu đọc được: thành viên nhóm hoặc giáo viên chủ bài. */
	@Transactional(readOnly = true)
	public PaperDocument requireReadable(AuthPrincipal user, Long documentId) {
		PaperDocument d = documentRepository.findFull(documentId).orElse(null);
		if (d == null || !canRead(user, d)) {
			throw ApiException.notFound("Không tìm thấy tài liệu");
		}
		return d;
	}

	/** Cho luật kênh STOMP: không ném lỗi. */
	@Transactional(readOnly = true)
	public boolean canRead(AuthPrincipal user, Long documentId) {
		return documentRepository.findFull(documentId).map(d -> canRead(user, d)).orElse(false);
	}

	/** Còn sửa được: bài chưa đóng và bài làm của nhóm chưa nộp (hoặc đang "Cần sửa"). Hạn nộp kiểm ở lúc nộp. */
	@Transactional(readOnly = true)
	public boolean editable(PaperDocument d) {
		if (d.getPart().getAssignment().getStatus() == AssignmentStatus.CLOSED) {
			return false;
		}
		return documentRepository.submissionStatus(d.getPart().getId(), d.getGroup().getId())
				.map(OPEN_STATUSES::contains).orElse(true);
	}

	private boolean canRead(AuthPrincipal user, PaperDocument d) {
		if (user.role() == Role.TEACHER) {
			return d.getPart().getAssignment().getTeacher().getId().equals(user.id());
		}
		return user.role() == Role.STUDENT && isMember(user.id(), d);
	}

	private boolean isMember(Long studentId, PaperDocument d) {
		return memberRepository.findByAssignmentIdAndIdStudentId(d.getPart().getAssignment().getId(), studentId)
				.map(m -> m.getId().getGroupId().equals(d.getGroup().getId())).orElse(false);
	}
}
