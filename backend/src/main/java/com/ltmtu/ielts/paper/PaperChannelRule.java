package com.ltmtu.ielts.paper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.security.StompAccessRule;
import com.ltmtu.ielts.user.Role;

import lombok.RequiredArgsConstructor;

/**
 * Quyền kênh tài liệu paper (docs/05 B.2): thành viên nhóm và giáo viên chủ bài nghe được
 * {@code /topic/paper/{id}[/awareness]}; chỉ sinh viên gửi được {@code /app/paper/{id}/update|awareness} (là thành viên
 * hay không kiểm ở {@link PaperDocumentService}, lỗi trả về {@code /user/queue/errors}).
 */
@Component
@RequiredArgsConstructor
public class PaperChannelRule implements StompAccessRule {

	private static final Pattern TOPIC = Pattern.compile("^/topic/paper/(\\d{1,18})(/awareness)?$");
	private static final Pattern SEND = Pattern.compile("^/app/paper/\\d{1,18}/(update|awareness)$");

	private final PaperDocAccess access;

	@Override
	public Boolean canSubscribe(AuthPrincipal user, String destination) {
		Matcher m = TOPIC.matcher(destination);
		return m.matches() ? access.canRead(user, Long.valueOf(m.group(1))) : null;
	}

	@Override
	public Boolean canSend(AuthPrincipal user, String destination) {
		return SEND.matcher(destination).matches() ? user.role() == Role.STUDENT : null;
	}
}
