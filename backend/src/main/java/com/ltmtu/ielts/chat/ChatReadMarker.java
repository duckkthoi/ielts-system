package com.ltmtu.ielts.chat;

import java.io.Serializable;
import java.time.Instant;

import com.ltmtu.ielts.chat.ChatChannel.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Tin cuối cùng một người đã đọc trong một kênh ("đã xem"). Ghi bằng upsert trong {@link ChatReadMarkerRepository}. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "chat_read_markers")
public class ChatReadMarker {

	@EmbeddedId
	private Id id;

	@Column(name = "last_read_message_id", nullable = false)
	private Long lastReadMessageId;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Getter
	@NoArgsConstructor
	@EqualsAndHashCode
	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "user_id")
		private Long userId;

		@Enumerated(EnumType.STRING)
		@Column(name = "channel_type", length = 20)
		private Type channelType;

		@Column(name = "channel_ref_id")
		private Long channelRefId;
	}
}
