package com.ltmtu.ielts.chat;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.chat.dto.ChatDtos.ChannelSummary;
import com.ltmtu.ielts.chat.dto.ChatDtos.ChatHistory;
import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/** REST của chat (dùng chung mọi vai trò) — docs/05-api.md mục A.9. Gửi tin đi qua STOMP ({@link ChatSocketController}). */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@GetMapping("/channels")
	public ApiResponse<List<ChannelSummary>> channels(@AuthenticationPrincipal AuthPrincipal me) {
		return ApiResponse.ok(chatService.channels(me.id()));
	}

	@GetMapping("/{channelId}/messages")
	public ApiResponse<ChatHistory> messages(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String channelId,
			@RequestParam(required = false) Long beforeId, @RequestParam(required = false) Integer size) {
		return ApiResponse.ok(chatService.history(me.id(), channelId, beforeId, size));
	}
}
