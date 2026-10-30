package com.ltmtu.ielts.realtime;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.ltmtu.ielts.realtime.RealtimeDtos.LiveContentMessage;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/** Lệnh STOMP {@code /app/live/{partId}/content} của sinh viên (docs/05 B.2). Quyền gửi kiểm ở {@link LiveChannelRule}. */
@Controller
@RequiredArgsConstructor
public class LiveController {

	private final LiveDraftService liveDraftService;

	@MessageMapping("/live/{partId}/content")
	public void content(@DestinationVariable Long partId, @Payload LiveContentMessage message, Principal user) {
		AuthPrincipal p = WsSessionRegistry.principal(user);
		if (p != null && message != null) {
			liveDraftService.update(p, partId, message.content());
		}
	}
}
