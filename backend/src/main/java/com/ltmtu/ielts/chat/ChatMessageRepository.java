package com.ltmtu.ielts.chat;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	/** Tin đã lưu theo mã client của người gửi (gửi lại sau khi mất mạng → không lưu trùng). */
	Optional<ChatMessage> findBySenderIdAndClientMsgId(Long senderId, String clientMsgId);
}
