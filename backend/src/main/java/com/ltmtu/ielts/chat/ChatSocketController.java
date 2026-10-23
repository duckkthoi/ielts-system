package com.ltmtu.ielts.chat;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.ltmtu.ielts.chat.dto.ChatDtos.ChatError;
import com.ltmtu.ielts.chat.dto.ChatDtos.ReadMessage;
import com.ltmtu.ielts.chat.dto.ChatDtos.SendMessage;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * Lệnh STOMP của chat (docs/05 B.2). Quyền gửi vào kênh đã kiểm ở {@link ChatChannelRule}; lỗi dữ liệu (tin rỗng,
 * quá dài…) trả về riêng người gửi trên {@code /user/queue/errors}.
 */
@Controller
@RequiredArgsConstructor
public class ChatSocketController {

	private final ChatService chatService;

	@MessageMapping("/chat/{channelId}/send")
	public void send(@DestinationVariable String channelId, @Payload SendMessage message, Principal user) {
		chatService.send(id(user), channelId, message == null ? null : message.content(),
				message == null ? null : message.clientMsgId());
	}

	@MessageMapping("/chat/{channelId}/typing")
	public void typing(@DestinationVariable String channelId, Principal user) {
		chatService.typing(id(user), channelId);
	}

	@MessageMapping("/chat/{channelId}/read")
	public void read(@DestinationVariable String channelId, @Payload ReadMessage message, Principal user) {
		chatService.read(id(user), channelId, message == null ? null : message.lastMessageId());
	}

	@MessageExceptionHandler(ApiException.class)
	@SendToUser(destinations = "/queue/errors", broadcast = false)
	public ChatError onError(ApiException e) {
		return new ChatError(e.getCode(), e.getMessage());
	}

	private static Long id(Principal user) {
		if (user instanceof Authentication auth && auth.getPrincipal() instanceof AuthPrincipal p) {
			return p.id();
		}
		throw ApiException.unauthorized("Chưa đăng nhập");
	}
}
