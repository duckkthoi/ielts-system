package com.ltmtu.ielts.grading;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.Pageables;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.grading.GradingEnums.CriterionGroup;
import com.ltmtu.ielts.grading.dto.FeedbackDtos.CommentBankDto;
import com.ltmtu.ielts.grading.dto.FeedbackDtos.CommentBankRequest;
import com.ltmtu.ielts.grading.dto.FeedbackDtos.ErrorCodeDto;
import com.ltmtu.ielts.grading.dto.FeedbackDtos.ErrorCodeRequest;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;

/**
 * Thư viện phản hồi của giáo viên: mã lỗi (BR-21) và ngân hàng nhận xét (BR-22). Mục của hệ thống ai cũng dùng được
 * nhưng không sửa / xoá được; mục riêng của giáo viên khác thì không thấy (404).
 */
@Service
@RequiredArgsConstructor
public class FeedbackLibraryService {

	private final ErrorCodeRepository errorCodeRepository;
	private final CommentBankRepository commentBankRepository;
	private final UserQueryService userQueryService;
	private final AppProperties props;
	private final Clock clock;

	// ─── Mã lỗi ──────────────────────────────────────────────────────────────

	@Transactional(readOnly = true)
	public List<ErrorCodeDto> errorCodes(Long teacherId) {
		return errorCodeRepository.findUsableBy(teacherId).stream().map(ErrorCodeDto::from).toList();
	}

	/** BR-22: thêm mã riêng; không được trùng mã của hệ thống hay mã khác của chính mình. */
	@Transactional
	public ErrorCodeDto createErrorCode(Long teacherId, ErrorCodeRequest req) {
		if (errorCodeRepository.countByOwnerId(teacherId) >= props.grading().maxErrorCodesPerTeacher()) {
			throw ApiException.badRequest("VALIDATION_ERROR",
					"Mỗi giáo viên có tối đa " + props.grading().maxErrorCodesPerTeacher() + " mã lỗi riêng");
		}
		ErrorCode e = new ErrorCode();
		e.setOwner(userQueryService.requireUser(teacherId));
		apply(e, req, teacherId);
		return ErrorCodeDto.from(errorCodeRepository.save(e));
	}

	@Transactional
	public ErrorCodeDto updateErrorCode(Long teacherId, Long id, ErrorCodeRequest req) {
		ErrorCode e = requireOwnErrorCode(teacherId, id);
		apply(e, req, teacherId);
		return ErrorCodeDto.from(errorCodeRepository.saveAndFlush(e));
	}

	/** Xoá mã riêng. Nhận xét inline đã dùng mã này vẫn giữ mã dạng chữ (bản chụp). */
	@Transactional
	public void deleteErrorCode(Long teacherId, Long id) {
		errorCodeRepository.delete(requireOwnErrorCode(teacherId, id));
	}

	private void apply(ErrorCode e, ErrorCodeRequest req, Long teacherId) {
		String code = req.code().trim().toUpperCase(Locale.ROOT);
		if (errorCodeRepository.existsConflicting(code, teacherId, e.getId())) {
			throw ApiException.conflict("DUPLICATE", "Mã lỗi '" + code + "' đã tồn tại");
		}
		e.setCode(code);
		e.setName(req.name().trim());
		e.setCriterion(req.criterion());
		e.setDescription(blankToNull(req.description()));
		e.setPositive(Boolean.TRUE.equals(req.positive()));
	}

	private ErrorCode requireOwnErrorCode(Long teacherId, Long id) {
		ErrorCode e = errorCodeRepository.findUsable(id, teacherId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy mã lỗi"));
		if (e.isSystem()) {
			throw ApiException.forbidden("Không sửa hoặc xoá được mã lỗi của hệ thống");
		}
		return e;
	}

	// ─── Ngân hàng nhận xét ──────────────────────────────────────────────────

	/** Nhận xét của tôi (hay dùng nhất lên đầu) rồi tới nhận xét mẫu; lọc theo tiêu chí vẫn kèm nhận xét dùng chung. */
	@Transactional(readOnly = true)
	public List<CommentBankDto> comments(Long teacherId, CriterionGroup criterion, String keyword) {
		return commentBankRepository.findUsableBy(teacherId, criterion, Pageables.likePattern(keyword)).stream()
				.map(CommentBankDto::from).toList();
	}

	@Transactional
	public CommentBankDto createComment(Long teacherId, CommentBankRequest req) {
		if (commentBankRepository.countByOwnerId(teacherId) >= props.grading().maxCommentBankItemsPerTeacher()) {
			throw ApiException.badRequest("VALIDATION_ERROR",
					"Ngân hàng nhận xét có tối đa " + props.grading().maxCommentBankItemsPerTeacher() + " mục");
		}
		CommentBankItem c = new CommentBankItem();
		c.setOwner(userQueryService.requireUser(teacherId));
		c.setCriterion(req.criterion());
		c.setContent(req.content().trim());
		return CommentBankDto.from(commentBankRepository.save(c));
	}

	@Transactional
	public CommentBankDto updateComment(Long teacherId, Long id, CommentBankRequest req) {
		CommentBankItem c = requireOwnComment(teacherId, id);
		c.setCriterion(req.criterion());
		c.setContent(req.content().trim());
		return CommentBankDto.from(commentBankRepository.saveAndFlush(c));
	}

	@Transactional
	public void deleteComment(Long teacherId, Long id) {
		commentBankRepository.delete(requireOwnComment(teacherId, id));
	}

	/**
	 * Ghi nhận một lần chèn nhận xét (BR-22: hay dùng lên đầu). Nhận xét mẫu của hệ thống dùng chung cho mọi giáo
	 * viên nên không đếm — đếm ở đó sẽ làm thứ tự của người này phụ thuộc vào người khác.
	 */
	@Transactional
	public CommentBankDto markUsed(Long teacherId, Long id) {
		CommentBankItem c = commentBankRepository.findUsable(id, teacherId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy nhận xét"));
		if (!c.isSystem()) {
			c.setUseCount(c.getUseCount() + 1);
			c.setLastUsedAt(clock.instant());
			commentBankRepository.flush();
		}
		return CommentBankDto.from(c);
	}

	private CommentBankItem requireOwnComment(Long teacherId, Long id) {
		CommentBankItem c = commentBankRepository.findUsable(id, teacherId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy nhận xét"));
		if (c.isSystem()) {
			throw ApiException.forbidden("Không sửa hoặc xoá được nhận xét mẫu của hệ thống");
		}
		return c;
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
