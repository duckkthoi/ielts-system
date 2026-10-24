package com.ltmtu.ielts.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.security.StompAccessRule;

import lombok.RequiredArgsConstructor;

/**
 * Quyền kênh chat (UC-29 E1, docs/05 B.2): chỉ thành viên kênh mới nghe {@code /topic/chat/{channelId}} và gửi
 * {@code /app/chat/{channelId}/send|typing|read}.
 */
@Component
@RequiredArgsConstructor
public class ChatChannelRule implements StompAccessRule {

	private static final Pattern TOPIC = Pattern.compile("^/topic/chat/([a-z]+-\\d{1,18})$");
	private static final Pattern SEND = Pattern.compile("^/app/chat/([a-z]+-\\d{1,18})/(send|typing|read)$");

	/** Chỉ phụ thuộc truy vấn (không phụ thuộc {@code ChatService}): luật được tạo cùng lúc với cấu hình WebSocket,
	 * mà {@code ChatService} lại cần {@code SimpMessagingTemplate} của chính cấu hình đó → vòng phụ thuộc bean. */
	private final ChatQueries queries;

	@Override
	public Boolean canSubscribe(AuthPrincipal user, String destination) {
		Matcher m = TOPIC.matcher(destination);
		return m.matches() ? isMember(user.id(), m.group(1)) : null;
	}

	@Override
	public Boolean canSend(AuthPrincipal user, String destination) {
		Matcher m = SEND.matcher(destination);
		return m.matches() ? isMember(user.id(), m.group(1)) : null;
	}

	private boolean isMember(Long userId, String channelId) {
		ChatChannel channel = ChatChannel.parse(channelId);
		return channel != null && queries.isMember(userId, channel);
	}
}
