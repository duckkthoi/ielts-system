package com.ltmtu.ielts.exam;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.ltmtu.ielts.assignment.TeacherAssignmentAccess;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.security.StompAccessRule;
import com.ltmtu.ielts.user.Role;

import lombok.RequiredArgsConstructor;

/**
 * Quyền kênh chế độ thi (docs/05 B.2): chỉ <b>giáo viên chủ bài</b> nghe được {@code /topic/assignments/{id}/exam};
 * chỉ <b>sinh viên</b> gửi được {@code /app/exam/{sessionId}/event} (phiên có phải của mình hay không kiểm ở
 * {@link ExamEventService}).
 */
@Component
@RequiredArgsConstructor
public class ExamChannelRule implements StompAccessRule {

	private static final Pattern TOPIC = Pattern.compile("^/topic/assignments/(\\d{1,18})/exam$");
	private static final Pattern EVENT = Pattern.compile("^/app/exam/\\d{1,18}/event$");

	private final TeacherAssignmentAccess assignmentAccess;

	@Override
	public Boolean canSubscribe(AuthPrincipal user, String destination) {
		Matcher m = TOPIC.matcher(destination);
		if (!m.matches()) {
			return null;
		}
		return user.role() == Role.TEACHER && assignmentAccess.isOwned(user.id(), Long.valueOf(m.group(1)));
	}

	@Override
	public Boolean canSend(AuthPrincipal user, String destination) {
		return EVENT.matcher(destination).matches() ? user.role() == Role.STUDENT : null;
	}
}
