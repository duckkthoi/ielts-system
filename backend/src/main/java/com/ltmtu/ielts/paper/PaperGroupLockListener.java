package com.ltmtu.ielts.paper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ltmtu.ielts.assignment.AssignmentEvents;
import com.ltmtu.ielts.notification.NotificationService;
import com.ltmtu.ielts.notification.NotificationService.NewNotification;
import com.ltmtu.ielts.notification.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * BR-27: tới hạn chốt nhóm mà còn sinh viên chưa có nhóm → báo giáo viên chủ bài để chia nốt. Nghe {@code BEFORE_COMMIT}
 * trong transaction của job, nên thông báo lưu cùng lúc với cờ "đã báo" (rollback thì không có gì).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class PaperGroupLockListener {

	private final PaperGroupMemberRepository memberRepository;
	private final NotificationService notifications;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	void onGroupLockReached(AssignmentEvents.GroupLockReached e) {
		Set<Long> ungrouped = new HashSet<>(e.recipientIds());
		memberRepository.findByAssignment(e.assignmentId()).forEach(m -> ungrouped.remove(m.getStudent().getId()));
		if (ungrouped.isEmpty()) {
			return;
		}
		notifications.notify(List.of(e.teacherId()), new NewNotification(NotificationType.PAPER_GROUP_LOCK,
				"Còn " + ungrouped.size() + " sinh viên chưa có nhóm: " + e.title(),
				"Đã tới hạn chốt nhóm. Hãy xếp nhóm cho những sinh viên này.",
				"/giao-vien/bai-tap/" + e.assignmentId() + "/nhom"));
		log.info("Bài {} tới hạn chốt nhóm, còn {} sinh viên chưa có nhóm", e.assignmentId(), ungrouped.size());
	}
}
