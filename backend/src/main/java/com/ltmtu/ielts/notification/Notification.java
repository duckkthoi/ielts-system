package com.ltmtu.ielts.notification;

import java.time.Instant;

import com.ltmtu.ielts.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một thông báo trong ứng dụng (FR-50). Luôn lưu DB trước khi đẩy WebSocket, nên người đang offline vẫn thấy khi vào
 * lại, và client mất kết nối lấy lại bằng {@code ?after=} (SD-09). Bảng chỉ có {@code created_at}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private NotificationType type;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "text")
	private String body;

	/** Đường dẫn trong frontend để mở khi bấm vào thông báo. */
	@Column(length = 500)
	private String link;

	@Column(name = "read_at")
	private Instant readAt;

	/** Giờ server (bean Clock); dùng cho {@code ?after=} khi kết nối lại. */
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
