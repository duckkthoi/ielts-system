package com.ltmtu.ielts.chat;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ltmtu.ielts.chat.dto.ChatDtos.ChannelSummary;
import com.ltmtu.ielts.chat.dto.ChatDtos.ChatHistory;
import com.ltmtu.ielts.chat.dto.ChatDtos.MessageEvent;
import com.ltmtu.ielts.chat.dto.ChatDtos.ReadEvent;
import com.ltmtu.ielts.chat.dto.ChatDtos.TypingEvent;
import com.ltmtu.ielts.common.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Chat theo bài nộp và theo lớp (UC-29, FR-55, SD-12). Tin nhắn lưu DB trong transaction, <b>sau commit</b> mới phát
 * tới {@code /topic/chat/{channelId}} — người nhận tải lịch sử ngay sau đó luôn thấy tin. "Đang gõ" không lưu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

	static final int MAX_CONTENT = 4000;
	static final int MAX_CLIENT_ID = 40;
	static final int MAX_PAGE = 100;
	/** Server bỏ bớt tin "đang gõ" dày hơn mức này (client gửi tối đa 1 lần / 3 giây). */
	private static final long TYPING_GAP_MS = 2000;

	private final ChatMessageRepository messageRepository;
	private final ChatReadMarkerRepository readRepository;
	private final ChatQueries queries;
	private final SimpMessagingTemplate messaging;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	/** Lần "đang gõ" gần nhất của (người, kênh) — trong bộ nhớ. */
	private final Map<String, Instant> lastTyping = new ConcurrentHashMap<>();

	/** Sự kiện nội bộ: phát tin sau commit. */
	record Broadcast(String topic, Object payload) {
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void afterCommit(Broadcast b) {
		messaging.convertAndSend(b.topic(), b.payload());
	}

	/** Kênh của tôi (REST {@code GET /api/chat/channels}). */
	@Transactional(readOnly = true)
	public List<ChannelSummary> channels(Long userId) {
		return queries.channelsOf(userId);
	}

	/** Lịch sử, mới nhất trước. Không thuộc kênh hoặc mã kênh sai → 404 (không lộ kênh tồn tại). */
	@Transactional(readOnly = true)
	public ChatHistory history(Long userId, String channelId, Long beforeId, Integer size) {
		ChatChannel channel = requireMember(userId, channelId);
		int n = size == null ? 50 : Math.max(1, Math.min(size, MAX_PAGE));
		return new ChatHistory(queries.history(channel, beforeId, n), queries.readers(channel, userId));
	}

	/**
	 * Gửi tin (STOMP {@code /app/chat/{id}/send}). Nội dung 1–4000 ký tự sau khi bỏ khoảng trắng hai đầu. Cùng
	 * {@code clientMsgId} gửi lại (mạng chập chờn) → không lưu thêm, phát lại tin đã lưu để client đánh dấu "đã gửi".
	 */
	@Transactional
	public MessageEvent send(Long userId, String channelId, String content, String clientMsgId) {
		ChatChannel channel = requireMember(userId, channelId);
		String text = content == null ? "" : content.strip();
		if (text.isEmpty() || text.length() > MAX_CONTENT) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Tin nhắn phải từ 1 đến " + MAX_CONTENT + " ký tự");
		}
		String clientId = clientMsgId == null || clientMsgId.isBlank() ? null : clientMsgId.strip();
		if (clientId != null && clientId.length() > MAX_CLIENT_ID) {
			throw ApiException.badRequest("VALIDATION_ERROR", "clientMsgId tối đa " + MAX_CLIENT_ID + " ký tự");
		}
		ChatMessage saved = clientId == null ? null
				: messageRepository.findBySenderIdAndClientMsgId(userId, clientId).orElse(null);
		if (saved == null) {
			try {
				saved = messageRepository.saveAndFlush(ChatMessage.of(channel, userId, text, clientId, clock.instant()));
			} catch (DataIntegrityViolationException e) {
				// Hai lần gửi cùng clientMsgId chạy song song: lần kia đã lưu và sẽ phát tin.
				throw ApiException.conflict("DUPLICATE", "Tin nhắn này đã được gửi");
			}
		}
		MessageEvent event = new MessageEvent("MESSAGE", saved.getId(), channel.id(), userId, queries.fullName(userId),
				saved.getContent(), saved.getClientMsgId(), saved.getCreatedAt());
		events.publishEvent(new Broadcast(channel.topic(), event));
		return event;
	}

	/** "Đang gõ…" (STOMP {@code /app/chat/{id}/typing}). Không lưu; gửi dày quá thì bỏ qua. */
	@Transactional(readOnly = true)
	public void typing(Long userId, String channelId) {
		Instant now = clock.instant();
		String key = userId + "@" + channelId;
		Instant last = lastTyping.get(key);
		if (last != null && now.toEpochMilli() - last.toEpochMilli() < TYPING_GAP_MS) {
			return;
		}
		ChatChannel channel = requireMember(userId, channelId);
		lastTyping.put(key, now);
		messaging.convertAndSend(channel.topic(), new TypingEvent("TYPING", userId, queries.fullName(userId)));
	}

	/** "Đã xem" (STOMP {@code /app/chat/{id}/read}): mốc chỉ tăng, tin phải thuộc kênh. */
	@Transactional
	public void read(Long userId, String channelId, Long lastMessageId) {
		ChatChannel channel = requireMember(userId, channelId);
		if (lastMessageId == null || !queries.messageInChannel(lastMessageId, channel)) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Tin nhắn không thuộc kênh này");
		}
		readRepository.markRead(userId, channel.type().name(), channel.refId(), lastMessageId, clock.instant());
		events.publishEvent(new Broadcast(channel.topic(), new ReadEvent("READ", userId, lastMessageId)));
	}

	private ChatChannel requireMember(Long userId, String channelId) {
		ChatChannel channel = ChatChannel.parse(channelId);
		if (channel == null || !queries.isMember(userId, channel)) {
			throw ApiException.notFound("Không tìm thấy kênh chat");
		}
		return channel;
	}
}
