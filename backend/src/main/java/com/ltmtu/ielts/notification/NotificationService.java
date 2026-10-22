package com.ltmtu.ielts.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.notification.MailService.Recipient;
import com.ltmtu.ielts.user.User;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cửa duy nhất để tạo thông báo (AI-CODING-RULES mục 11; SD-05, SD-09). Thứ tự:
 * <ol>
 * <li>lưu DB <b>trong transaction của nghiệp vụ</b> — nghiệp vụ rollback thì không có thông báo "ma";</li>
 * <li>sau khi commit: đẩy WebSocket tới {@code /user/queue/notifications} của từng người;</li>
 * <li>rồi gửi email nền ({@code @Async}) nếu loại thông báo có email (FR-51).</li>
 * </ol>
 * Người đang offline không nhận được WebSocket nhưng vẫn có thông báo trong DB, client lấy lại bằng {@code ?after=}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	static final String USER_QUEUE = "/queue/notifications";

	private final NotificationRepository repository;
	private final UserQueryService userQueryService;
	private final SimpMessageSendingOperations messaging;
	private final MailService mailService;
	private final Clock clock;

	/** Nội dung một thông báo, giống nhau cho mọi người nhận. */
	public record NewNotification(NotificationType type, String title, String body, String link) {
	}

	private record Delivery(String username, NotificationDto dto) {
	}

	/** Tạo thông báo cho nhiều người nhận. Id không tồn tại thì bỏ qua. Trả về số thông báo đã tạo. */
	@Transactional
	public int notify(Collection<Long> userIds, NewNotification n) {
		List<User> recipients = userQueryService.findAllByIds(userIds);
		if (recipients.isEmpty()) {
			return 0;
		}
		Instant now = clock.instant();
		List<Notification> saved = repository.saveAll(recipients.stream().map(u -> {
			Notification x = new Notification();
			x.setUser(u);
			x.setType(n.type());
			x.setTitle(n.title());
			x.setBody(n.body());
			x.setLink(n.link());
			x.setCreatedAt(now);
			return x;
		}).toList());
		// Lấy sẵn dữ liệu cần dùng sau commit (lúc đó không còn đọc entity lazy được nữa).
		List<Delivery> deliveries = saved.stream()
				.map(x -> new Delivery(x.getUser().getUsername(), NotificationDto.from(x))).toList();
		List<Recipient> mailTo = n.type().sendsEmail()
				? recipients.stream().map(u -> new Recipient(u.getEmail(), u.getFullName())).toList()
				: List.of();
		afterCommit(() -> {
			deliveries.forEach(this::push);
			if (!mailTo.isEmpty()) {
				mailService.sendNotification(mailTo, n.title(), n.body(), n.link());
			}
		});
		log.info("Tạo {} thông báo {}", saved.size(), n.type());
		return saved.size();
	}

	// ─── API của người nhận (docs/05-api.md A.9) ─────────────────────────────

	@Transactional(readOnly = true)
	public Page<NotificationDto> list(Long userId, boolean unreadOnly, Instant after, Pageable pageable) {
		return repository.search(userId, unreadOnly, after, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
				.map(NotificationDto::from);
	}

	@Transactional(readOnly = true)
	public long unreadCount(Long userId) {
		return repository.countByUserIdAndReadAtIsNull(userId);
	}

	/** Đánh dấu đã đọc. Đọc lại lần nữa thì giữ thời điểm đọc đầu tiên. Thông báo của người khác → 404. */
	@Transactional
	public NotificationDto markRead(Long userId, Long id) {
		Notification n = repository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy thông báo"));
		if (n.getReadAt() == null) {
			n.setReadAt(clock.instant());
		}
		return NotificationDto.from(n);
	}

	@Transactional
	public int markAllRead(Long userId) {
		return repository.markAllRead(userId, clock.instant());
	}

	// ─── Nội bộ ──────────────────────────────────────────────────────────────

	private void push(Delivery d) {
		try {
			messaging.convertAndSendToUser(d.username(), USER_QUEUE, d.dto());
		} catch (MessagingException e) {
			// Thông báo đã nằm trong DB; client sẽ lấy lại khi kết nối lại.
			log.warn("Không đẩy được thông báo {} qua WebSocket: {}", d.dto().id(), e.getMessage());
		}
	}

	/** Chạy sau khi transaction hiện tại commit; không có transaction (vd gọi từ job) thì chạy ngay. */
	private static void afterCommit(Runnable task) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					task.run();
				}
			});
		} else {
			task.run();
		}
	}
}
