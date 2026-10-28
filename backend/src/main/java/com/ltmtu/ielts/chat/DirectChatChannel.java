package com.ltmtu.ielts.chat;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Kênh riêng ổn định giữa một giáo viên và một sinh viên. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "direct_chat_channels", uniqueConstraints = @UniqueConstraint(name = "uq_direct_chat_pair", columnNames = {
		"teacher_id", "student_id" }))
public class DirectChatChannel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "teacher_id", nullable = false)
	private Long teacherId;

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	static DirectChatChannel of(Long teacherId, Long studentId, Instant now) {
		DirectChatChannel c = new DirectChatChannel();
		c.teacherId = teacherId;
		c.studentId = studentId;
		c.createdAt = now;
		return c;
	}
}
