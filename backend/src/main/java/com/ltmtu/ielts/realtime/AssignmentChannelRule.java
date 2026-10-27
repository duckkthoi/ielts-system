package com.ltmtu.ielts.realtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.ltmtu.ielts.assignment.TeacherAssignmentAccess;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.security.StompAccessRule;
import com.ltmtu.ielts.user.Role;

import lombok.RequiredArgsConstructor;

/**
 * Quyền các kênh theo bài tập (docs/05 B.2): chỉ <b>giáo viên chủ bài</b> nghe được tiến độ, online và hoạt động của bài;
 * chỉ <b>sinh viên</b> gửi được trạng thái đang mở phần nào (bài có được thấy hay không kiểm ở
 * {@link PresenceService}).
 */
@Component
@RequiredArgsConstructor
public class AssignmentChannelRule implements StompAccessRule {

	private static final Pattern TOPIC = Pattern.compile("^/topic/assignments/(\\d{1,18})/(progress|presence|activity)$");
	private static final String VIEWING = "/app/presence/viewing";

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
		return VIEWING.equals(destination) ? user.role() == Role.STUDENT : null;
	}
}
