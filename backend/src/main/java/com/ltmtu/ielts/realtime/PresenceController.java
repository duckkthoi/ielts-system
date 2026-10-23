package com.ltmtu.ielts.realtime;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.ltmtu.ielts.realtime.RealtimeDtos.ViewingMessage;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/** Lệnh STOMP {@code /app/presence/viewing} của sinh viên (docs/05 B.2). Quyền gửi kiểm ở {@link AssignmentChannelRule}. */
@Controller
@RequiredArgsConstructor
public class PresenceController {

	private final PresenceService presenceService;

	@MessageMapping("/presence/viewing")
	public void viewing(@Payload ViewingMessage message, Principal user, SimpMessageHeaderAccessor headers) {
		AuthPrincipal p = WsSessionRegistry.principal(user);
		if (p != null && message != null) {
			presenceService.updateViewing(p, headers.getSessionId(), message.assignmentId(), message.partId());
		}
	}
}
