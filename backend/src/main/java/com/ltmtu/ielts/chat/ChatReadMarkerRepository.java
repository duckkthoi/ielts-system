package com.ltmtu.ielts.chat;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatReadMarkerRepository extends JpaRepository<ChatReadMarker, ChatReadMarker.Id> {

	/**
	 * Ghi "đã đọc tới tin X". Chỉ tăng, không lùi ({@code GREATEST}) — tin "đã xem" tới muộn từ tab cũ không làm mất
	 * trạng thái mới hơn. Upsert một câu, chạy đồng thời vẫn đúng.
	 */
	@Modifying
	@Query(nativeQuery = true, value = """
			INSERT INTO chat_read_markers (user_id, channel_type, channel_ref_id, last_read_message_id, updated_at)
			VALUES (:userId, :type, :refId, :messageId, :now)
			ON CONFLICT (user_id, channel_type, channel_ref_id) DO UPDATE
			SET last_read_message_id = GREATEST(chat_read_markers.last_read_message_id, EXCLUDED.last_read_message_id),
			    updated_at = EXCLUDED.updated_at""")
	int markRead(@Param("userId") Long userId, @Param("type") String channelType, @Param("refId") Long refId,
			@Param("messageId") Long messageId, @Param("now") Instant now);
}
