package com.ltmtu.ielts.paper;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.CommentDto;
import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.CreateRequest;
import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.UpdateRequest;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;

/** Comment của giáo viên trên tài liệu Paper đang viết; không phải điểm chấm của bản nộp. */
@Service
@RequiredArgsConstructor
public class PaperLiveCommentService {

	private final PaperDocAccess documentAccess;
	private final PaperLiveCommentRepository commentRepository;
	private final UserQueryService userQueryService;
	private final ApplicationEventPublisher events;

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
		PaperLiveComment saved = commentRepository.saveAndFlush(new PaperLiveComment(document,
				userQueryService.requireUser(teacher.id()), req.startOffset(), req.endOffset(), req.quote().trim(),
				req.comment().trim()));
		CommentDto dto = toDto(saved);
		events.publishEvent(new PaperLiveCommentEvents.Changed(documentId, "CREATED", dto, saved.getId()));
		return dto;
	}

	@Transactional
	public CommentDto update(AuthPrincipal teacher, Long documentId, Long id, UpdateRequest req) {
		requireTeacher(teacher);
		PaperLiveComment comment = commentRepository.findInDocument(id, documentId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy nhận xét"));
		documentAccess.requireReadable(teacher, documentId);
		comment.setComment(req.comment().trim());
		commentRepository.flush();
		CommentDto dto = toDto(comment);
		events.publishEvent(new PaperLiveCommentEvents.Changed(documentId, "UPDATED", dto, id));
		return dto;
	}

	@Transactional
	public void delete(AuthPrincipal teacher, Long documentId, Long id) {
		requireTeacher(teacher);
		PaperLiveComment comment = commentRepository.findInDocument(id, documentId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy nhận xét"));
		documentAccess.requireReadable(teacher, documentId);
		commentRepository.delete(comment);
		events.publishEvent(new PaperLiveCommentEvents.Changed(documentId, "DELETED", null, id));
	}

	private static void requireTeacher(AuthPrincipal user) {
		if (user.role() != Role.TEACHER) {
			throw ApiException.forbidden("Chỉ giáo viên mới được nhận xét trực tiếp");
		}
	}

	private CommentDto toDto(PaperLiveComment c) {
		return new CommentDto(c.getId(), c.getDocument().getId(), c.getAuthor().getId(), c.getAuthor().getFullName(),
				c.getStartOffset(), c.getEndOffset(), c.getQuote(), c.getComment(), c.getCreatedAt(), c.getUpdatedAt());
	}
}
