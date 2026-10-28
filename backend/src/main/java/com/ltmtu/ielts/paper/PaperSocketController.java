package com.ltmtu.ielts.paper;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.AwarenessMessage;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.SocketError;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.UpdateMessage;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/** Lệnh STOMP của tài liệu paper (docs/05 B.2). Quyền gửi kiểm ở {@link PaperChannelRule}; lỗi trả riêng người gửi. */
@Controller
@RequiredArgsConstructor
public class PaperSocketController {

	private final PaperDocumentService documentService;

	@MessageMapping("/paper/{documentId}/update")
	public void update(@DestinationVariable Long documentId, @Payload UpdateMessage message, Principal user) {
		documentService.applyUpdate(principal(user), documentId, message);
	}

	@MessageMapping("/paper/{documentId}/awareness")
	public void awareness(@DestinationVariable Long documentId, @Payload AwarenessMessage message, Principal user) {
		documentService.awareness(principal(user), documentId, message == null ? null : message.state());
	}

	@MessageExceptionHandler(ApiException.class)
	@SendToUser(destinations = "/queue/errors", broadcast = false)
	public SocketError onError(ApiException e) {
		return new SocketError(e.getCode(), e.getMessage());
	}

	private static AuthPrincipal principal(Principal user) {
		if (user instanceof Authentication auth && auth.getPrincipal() instanceof AuthPrincipal p) {
			return p;
		}
		throw ApiException.unauthorized("Chưa đăng nhập");
	}
}
