package com.ltmtu.ielts.notification;

import java.time.Instant;

/** Dùng chung cho REST và WebSocket ({@code /user/queue/notifications}) — docs/05-api.md mục A.9. */
public record NotificationDto(Long id, NotificationType type, String title, String body, String link, Instant readAt,
		Instant createdAt) {

	public static NotificationDto from(Notification n) {
		return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(), n.getReadAt(),
				n.getCreatedAt());
	}
}
