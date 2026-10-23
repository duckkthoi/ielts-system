package com.ltmtu.ielts.chat;

import java.time.Instant;

import com.ltmtu.ielts.chat.ChatChannel.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Một tin nhắn. Chỉ thêm, không sửa / xoá. Giữ id dạng số (bài nộp, lớp, người gửi) thay vì quan hệ entity, để module
 * chat không gắn chặt vào entity của module khác.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "channel_type", nullable = false, length = 20)
	private Type channelType;

	@Column(name = "submission_id")
	private Long submissionId;

	@Column(name = "classroom_id")
	private Long classroomId;

	@Column(name = "sender_id", nullable = false)
	private Long senderId;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "client_msg_id", length = 40)
	private String clientMsgId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	static ChatMessage of(ChatChannel channel, Long senderId, String content, String clientMsgId, Instant now) {
		ChatMessage m = new ChatMessage();
		m.channelType = channel.type();
		if (channel.type() == Type.SUBMISSION) {
			m.submissionId = channel.refId();
		} else {
			m.classroomId = channel.refId();
		}
		m.senderId = senderId;
		m.content = content;
		m.clientMsgId = clientMsgId;
		m.createdAt = now;
		return m;
	}
}
