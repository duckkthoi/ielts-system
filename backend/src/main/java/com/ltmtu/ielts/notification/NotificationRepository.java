package com.ltmtu.ielts.notification;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	/** Mới nhất trước. {@code after}: chỉ lấy thông báo tạo sau thời điểm này (lấy lại thông báo bị lỡ, SD-09). */
	@Query(value = """
			SELECT n FROM Notification n WHERE n.user.id = :userId
			  AND (:unreadOnly = false OR n.readAt IS NULL)
			  AND (CAST(:after AS Instant) IS NULL OR n.createdAt > :after)
			ORDER BY n.createdAt DESC, n.id DESC""",
			countQuery = """
					SELECT count(n) FROM Notification n WHERE n.user.id = :userId
					  AND (:unreadOnly = false OR n.readAt IS NULL)
					  AND (CAST(:after AS Instant) IS NULL OR n.createdAt > :after)""")
	Page<Notification> search(@Param("userId") Long userId, @Param("unreadOnly") boolean unreadOnly,
			@Param("after") Instant after, Pageable pageable);

	long countByUserIdAndReadAtIsNull(Long userId);

	Optional<Notification> findByIdAndUserId(Long id, Long userId);

	@Modifying
	@Query("UPDATE Notification n SET n.readAt = :now WHERE n.user.id = :userId AND n.readAt IS NULL")
	int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);
}
