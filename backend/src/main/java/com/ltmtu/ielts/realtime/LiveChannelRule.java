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
 * Quyền kênh xem trực tiếp (docs/05 B.2, BR-26): chỉ <b>giáo viên chủ bài</b> của phần nghe được
 * {@code /topic/live/parts/{partId}/students/{studentId}}; chỉ <b>sinh viên</b> gửi được
 * {@code /app/live/{partId}/content} (phần có được thấy hay không kiểm ở {@link LiveDraftService}).
 */
@Component
@RequiredArgsConstructor
public class LiveChannelRule implements StompAccessRule {

	private static final Pattern TOPIC = Pattern.compile("^/topic/live/parts/(\\d{1,18})/students/\\d{1,18}$");
	private static final Pattern CONTENT = Pattern.compile("^/app/live/\\d{1,18}/content$");

	private final TeacherAssignmentAccess assignmentAccess;

	@Override
	public Boolean canSubscribe(AuthPrincipal user, String destination) {
		Matcher m = TOPIC.matcher(destination);
		if (!m.matches()) {
			return null;
		}
		return user.role() == Role.TEACHER && assignmentAccess.isPartOwned(user.id(), Long.valueOf(m.group(1)));
	}

	@Override
	public Boolean canSend(AuthPrincipal user, String destination) {
		return CONTENT.matcher(destination).matches() ? user.role() == Role.STUDENT : null;
	}
}
