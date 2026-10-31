package com.ltmtu.ielts.paper;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.SlidingWindowLimiter;
import com.ltmtu.ielts.paper.PaperLiveComment.Kind;
import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.CommentDto;
import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.CreateRequest;
import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.UpdateRequest;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.UserQueryService;

/**
 * Phản hồi hướng dẫn của giáo viên trên tài liệu Paper đang viết (H4) — không phải điểm chấm của bản nộp, không đổi
 * nội dung tài liệu. Giáo viên chủ bài tạo; chỉ tác giả sửa / xoá; thành viên nhóm + giáo viên chủ bài đọc và đánh
 * dấu "đã xử lý". Mọi thay đổi phát {@code CREATED | UPDATED | DELETED | RESOLVED} trên
 * {@code /topic/paper/{id}/comments} sau commit. Ghi tối đa {@value #MAX_WRITES_PER_MINUTE} lần / phút / người.
 */
@Service
public class PaperLiveCommentService {

	static final int MAX_WRITES_PER_MINUTE = 30;

	private final PaperDocAccess documentAccess;
	private final PaperLiveCommentRepository commentRepository;
	private final UserQueryService userQueryService;
	private final ApplicationEventPublisher events;
	private final Clock clock;
	private final SlidingWindowLimiter limiter;

	public PaperLiveCommentService(PaperDocAccess documentAccess, PaperLiveCommentRepository commentRepository,
			UserQueryService userQueryService, ApplicationEventPublisher events, Clock clock) {
		this.documentAccess = documentAccess;
		this.commentRepository = commentRepository;
		this.userQueryService = userQueryService;
		this.events = events;
		this.clock = clock;
		this.limiter = new SlidingWindowLimiter(MAX_WRITES_PER_MINUTE, Duration.ofMinutes(1), clock);
	}

	@Transactional(readOnly = true)
	public List<CommentDto> list(AuthPrincipal user, Long documentId) {
		documentAccess.requireReadable(user, documentId);
		return commentRepository.findByDocument(documentId).stream().map(this::toDto).toList();
	}

	@Transactional
	public CommentDto add(AuthPrincipal teacher, Long documentId, CreateRequest req) {
		requireTeacher(teacher);
		PaperDocument document = documentAccess.requireReadable(teacher, documentId);
		if (req.endOffset() <= req.startOffset()) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Vị trí đoạn chọn không hợp lệ");
		}
		throttle(teacher);
		PaperLiveComment c = new PaperLiveComment(document, userQueryService.requireUser(teacher.id()),
				req.startOffset(), req.endOffset(), req.quote().trim(), req.comment().trim());
		c.setKind(req.kind() == null ? Kind.COMMENT : req.kind());
		c.setSuggestion(blankToNull(req.suggestion()));
		c.setSectionKey(blankToNull(req.sectionKey()));
		PaperLiveComment saved = commentRepository.saveAndFlush(c);
		return publish(documentId, "CREATED", saved);
	}

	@Transactional
	public CommentDto update(AuthPrincipal teacher, Long documentId, Long id, UpdateRequest req) {
		PaperLiveComment comment = requireOwn(teacher, documentId, id);
		throttle(teacher);
		comment.setComment(req.comment().trim());
		if (req.kind() != null) {
			comment.setKind(req.kind());
		}
		comment.setSuggestion(blankToNull(req.suggestion()));
		commentRepository.flush();
		return publish(documentId, "UPDATED", comment);
	}

	@Transactional
	public void delete(AuthPrincipal teacher, Long documentId, Long id) {
		PaperLiveComment comment = requireOwn(teacher, documentId, id);
		throttle(teacher);
		commentRepository.delete(comment);
		events.publishEvent(new PaperLiveCommentEvents.Changed(documentId, "DELETED", null, id));
	}

	/** Thành viên nhóm hoặc giáo viên chủ bài đánh dấu đã xử lý / mở lại. */
	@Transactional
	public CommentDto resolve(AuthPrincipal user, Long documentId, Long id, boolean resolved) {
		documentAccess.requireReadable(user, documentId);
		PaperLiveComment comment = commentRepository.findInDocument(id, documentId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy nhận xét"));
		throttle(user);
		if (resolved) {
			comment.setResolvedAt(clock.instant());
			comment.setResolvedBy(userQueryService.requireUser(user.id()));
		} else {
			comment.setResolvedAt(null);
			comment.setResolvedBy(null);
		}
		commentRepository.flush();
		return publish(documentId, "RESOLVED", comment);
	}

	/** Xoá bộ đếm giới hạn (dùng trong test). */
	public void clear() {
		limiter.clear();
	}

	// ─── Nội bộ ──────────────────────────────────────────────────────────────

	private PaperLiveComment requireOwn(AuthPrincipal teacher, Long documentId, Long id) {
		requireTeacher(teacher);
		documentAccess.requireReadable(teacher, documentId);
		PaperLiveComment comment = commentRepository.findInDocument(id, documentId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy nhận xét"));
		if (!comment.getAuthor().getId().equals(teacher.id())) {
			throw ApiException.forbidden("Chỉ sửa / xoá được nhận xét của chính bạn");
		}
		return comment;
	}

	private void throttle(AuthPrincipal user) {
		if (!limiter.tryAcquire("live-comment:" + user.id())) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Bạn thao tác quá nhanh, vui lòng thử lại sau ít phút");
		}
	}

	private CommentDto publish(Long documentId, String type, PaperLiveComment c) {
		CommentDto dto = toDto(c);
		events.publishEvent(new PaperLiveCommentEvents.Changed(documentId, type, dto, c.getId()));
		return dto;
	}

	private static void requireTeacher(AuthPrincipal user) {
		if (user.role() != Role.TEACHER) {
			throw ApiException.forbidden("Chỉ giáo viên mới được nhận xét trực tiếp");
		}
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private CommentDto toDto(PaperLiveComment c) {
		return new CommentDto(c.getId(), c.getDocument().getId(), c.getAuthor().getId(), c.getAuthor().getFullName(),
				c.getStartOffset(), c.getEndOffset(), c.getQuote(), c.getComment(), c.getKind(), c.getSuggestion(),
				c.getSectionKey(), c.getResolvedAt(), c.getResolvedBy() == null ? null : c.getResolvedBy().getFullName(),
				c.getCreatedAt(), c.getUpdatedAt());
	}
}
