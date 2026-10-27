package com.ltmtu.ielts.chat;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectChatChannelRepository extends JpaRepository<DirectChatChannel, Long> {

	Optional<DirectChatChannel> findByTeacherIdAndStudentId(Long teacherId, Long studentId);
}
