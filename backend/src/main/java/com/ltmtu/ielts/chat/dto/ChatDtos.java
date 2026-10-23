package com.ltmtu.ielts.chat.dto;

import java.time.Instant;
import java.util.List;

/** Dữ liệu chat — docs/05-api.md mục A.9 (REST) và B.2 ({@code ChatEvent} qua WebSocket). */
public final class ChatDtos {

	private ChatDtos() {
	}

	/** SEND {@code /app/chat/{channelId}/send}. */
	public record SendMessage(String content, String clientMsgId) {
	}

	/** SEND {@code /app/chat/{channelId}/read}. */
	public record ReadMessage(Long lastMessageId) {
	}

	/** {@code ChatEvent} loại {@code MESSAGE}; cũng là một dòng của lịch sử chat. */
	public record MessageEvent(String type, Long id, String channelId, Long senderId, String senderName, String content,
			String clientMsgId, Instant createdAt) {
	}

	/** {@code ChatEvent} loại {@code TYPING}. */
	public record TypingEvent(String type, Long userId, String userName) {
	}

	/** {@code ChatEvent} loại {@code READ}. */
	public record ReadEvent(String type, Long userId, Long lastMessageId) {
	}

	/** Lịch sử: {@code messages} mới nhất trước; {@code reads} = tin cuối mỗi người khác đã đọc ("đã xem"). */
	public record ChatHistory(List<MessageEvent> messages, List<Reader> reads) {
	}

	public record Reader(Long userId, String userName, Long lastMessageId) {
	}

	/** Một kênh trong danh sách: tin cuối, số tin chưa đọc (tin của người khác sau mốc đã đọc). */
	public record ChannelSummary(String channelId, String type, String title, LastMessage lastMessage, long unreadCount) {
	}

	public record LastMessage(Long id, String senderName, String preview, Instant createdAt) {
	}

	/** Lỗi gửi về {@code /user/queue/errors} khi lệnh STOMP không hợp lệ. */
	public record ChatError(String code, String message) {
	}
}
