package com.ltmtu.ielts.exam;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.ltmtu.ielts.exam.dto.ExamDtos.EventMessage;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/** Lệnh STOMP {@code /app/exam/{sessionId}/event} của sinh viên (docs/05 B.2). Quyền gửi kiểm ở {@link ExamChannelRule}. */
@Controller
@RequiredArgsConstructor
public class ExamSocketController {

	private final ExamEventService eventService;

	@MessageMapping("/exam/{sessionId}/event")
	public void event(@DestinationVariable Long sessionId, @Payload EventMessage message, Principal user) {
		if (user instanceof Authentication auth && auth.getPrincipal() instanceof AuthPrincipal p && message != null) {
			eventService.clientEvent(p, sessionId, message.type(), message.pasteChars(), message.occurredAt());
		}
	}
}
