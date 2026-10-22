package com.ltmtu.ielts.assignment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.ChecklistItemRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.CriterionRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.PartRequest;
import com.ltmtu.ielts.common.ApiException;

/**
 * Quy tắc nghiệp vụ của bài tập và rubric mà annotation validate không diễn đạt được
 * (BR-11, BR-16, docs/08-nghiep-vu-cham-ielts.md mục 3.1, 4.2).
 */
final class RubricRules {

	static final BigDecimal WHOLE = BigDecimal.ONE;
	static final BigDecimal HALF = new BigDecimal("0.5");
	static final BigDecimal DEFAULT_MAX = BigDecimal.valueOf(9);

	private RubricRules() {
	}

	static void validate(AssignmentRequest req) {
		requireOrder(req.openAt(), req.dueAt());
		List<PartRequest> parts = req.parts() == null ? List.of() : req.parts();
		for (int p = 0; p < parts.size(); p++) {
			validatePart(parts.get(p), "Phần " + (p + 1));
		}
	}

	static void requireOrder(Instant openAt, Instant dueAt) {
		if (openAt != null && dueAt != null && !openAt.isBefore(dueAt)) {
			throw invalid("Thời điểm mở phải trước hạn nộp");
		}
	}

	private static void validatePart(PartRequest part, String where) {
		if (part.imageUrl() != null && !part.imageUrl().isBlank() && !isAllowedImageUrl(part.imageUrl())) {
			throw invalid(where + ": đường dẫn ảnh không hợp lệ");
		}
		Set<CriterionCode> codes = EnumSet.noneOf(CriterionCode.class);
		List<CriterionRequest> criteria = part.criteria() == null ? List.of() : part.criteria();
		for (int c = 0; c < criteria.size(); c++) {
			CriterionRequest cr = criteria.get(c);
			String at = where + " – tiêu chí " + (c + 1);
			if (cr.code() != null && !codes.add(cr.code())) {
				throw invalid(at + ": mã tiêu chí " + cr.code() + " bị lặp trong cùng một phần");
			}
			BigDecimal step = stepOf(cr);
			BigDecimal max = maxOf(cr);
			if (step.compareTo(WHOLE) != 0 && step.compareTo(HALF) != 0) {
				throw invalid(at + ": bước điểm chỉ được là 1 (nguyên band) hoặc 0.5");
			}
			if (!isMultiple(max, step)) {
				throw invalid(at + ": điểm tối đa phải là bội số của bước điểm");
			}
			List<ChecklistItemRequest> items = cr.checklist() == null ? List.of() : cr.checklist();
			for (ChecklistItemRequest item : items) {
				validateItem(item, max, step, at);
			}
		}
	}

	private static void validateItem(ChecklistItemRequest item, BigDecimal max, BigDecimal step, String at) {
		if (kindOf(item) == ChecklistKind.LIMIT) {
			BigDecimal band = item.maxBand();
			if (band == null) {
				throw invalid(at + ": mục \"Giới hạn điểm\" phải có band tối đa");
			}
			if (band.signum() < 0 || band.compareTo(max) > 0 || !isMultiple(band, step)) {
				throw invalid(at + ": band tối đa của mục \"" + item.content() + "\" phải từ 0 tới " + max.stripTrailingZeros().toPlainString()
						+ " và là bội số của bước điểm");
			}
		}
	}

	/** Ảnh đề bài: chỉ ảnh upload lên hệ thống hoặc link http(s). */
	static boolean isAllowedImageUrl(String url) {
		return url.startsWith("/uploads/") || url.startsWith("https://") || url.startsWith("http://");
	}

	static BigDecimal stepOf(CriterionRequest cr) {
		return cr.step() == null ? WHOLE : cr.step();
	}

	static BigDecimal maxOf(CriterionRequest cr) {
		return cr.maxScore() == null ? DEFAULT_MAX : cr.maxScore();
	}

	static ChecklistKind kindOf(ChecklistItemRequest item) {
		return item.kind() == null ? ChecklistKind.CHECK : item.kind();
	}

	static boolean isMultiple(BigDecimal value, BigDecimal step) {
		return value.remainder(step).signum() == 0;
	}

	private static ApiException invalid(String message) {
		return ApiException.badRequest("VALIDATION_ERROR", message);
	}
}
