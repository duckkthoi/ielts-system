package com.ltmtu.ielts.assignment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.ChecklistItemRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.CriterionRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.LevelRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.PartRequest;
import com.ltmtu.ielts.assignment.dto.QuestionBankDtos.QuestionBankRequest;
import com.ltmtu.ielts.common.ApiException;

/**
 * Quy tắc nghiệp vụ của bài tập và rubric mà annotation validate không diễn đạt được
 * (BR-11, BR-16, BR-31, docs/08-nghiep-vu-cham-ielts.md mục 3.1, 4.2).
 */
final class RubricRules {

	static final BigDecimal WHOLE = BigDecimal.ONE;
	static final BigDecimal HALF = new BigDecimal("0.5");
	static final BigDecimal QUARTER = new BigDecimal("0.25");
	/** Band IELTS tối đa (bài ESSAY). */
	static final BigDecimal DEFAULT_MAX = BigDecimal.valueOf(9);
	/** Thang 10 của bài PAPER (BR-31). */
	static final BigDecimal PAPER_MAX = BigDecimal.TEN;

	private RubricRules() {
	}

	static void validate(AssignmentRequest req) {
		requireOrder(req.openAt(), req.dueAt());
		PaperRules.validate(req);
		AssignmentKind kind = PaperRules.kindOf(req);
		List<PartRequest> parts = req.parts() == null ? List.of() : req.parts();
		for (int p = 0; p < parts.size(); p++) {
			validateRubric(parts.get(p).imageUrl(), parts.get(p).criteria(), "Phần " + (p + 1), kind);
		}
	}

	static void validate(QuestionBankRequest req) {
		validateRubric(req.imageUrl(), req.criteria(), "Ngân hàng đề", AssignmentKind.ESSAY);
	}

	static void requireOrder(Instant openAt, Instant dueAt) {
		if (openAt != null && dueAt != null && !openAt.isBefore(dueAt)) {
			throw invalid("Thời điểm mở phải trước hạn nộp");
		}
	}

	private static void validateRubric(String imageUrl, List<CriterionRequest> requestedCriteria, String where,
			AssignmentKind kind) {
		if (imageUrl != null && !imageUrl.isBlank() && !isAllowedImageUrl(imageUrl)) {
			throw invalid(where + ": đường dẫn ảnh không hợp lệ");
		}
		Set<CriterionCode> codes = EnumSet.noneOf(CriterionCode.class);
		List<CriterionRequest> criteria = requestedCriteria == null ? List.of() : requestedCriteria;
		for (int c = 0; c < criteria.size(); c++) {
			CriterionRequest cr = criteria.get(c);
			String at = where + " – tiêu chí " + (c + 1);
			if (cr.code() != null && !codes.add(cr.code())) {
				throw invalid(at + ": mã tiêu chí " + cr.code() + " bị lặp trong cùng một phần");
			}
			BigDecimal step = stepOf(cr, kind);
			BigDecimal max = maxOf(cr, kind);
			if (kind == AssignmentKind.PAPER) {
				// BR-31: thang 10, bước 1 / 0,5 / 0,25.
				if (step.compareTo(WHOLE) != 0 && step.compareTo(HALF) != 0 && step.compareTo(QUARTER) != 0) {
					throw invalid(at + ": bước điểm chỉ được là 1, 0.5 hoặc 0.25");
				}
			} else {
				if (step.compareTo(WHOLE) != 0 && step.compareTo(HALF) != 0) {
					throw invalid(at + ": bước điểm chỉ được là 1 (nguyên band) hoặc 0.5");
				}
				if (max.compareTo(DEFAULT_MAX) > 0) {
					throw invalid(at + ": điểm tối đa của bài IELTS từ 1 tới 9");
				}
			}
			if (!isMultiple(max, step)) {
				throw invalid(at + ": điểm tối đa phải là bội số của bước điểm");
			}
			List<ChecklistItemRequest> items = cr.checklist() == null ? List.of() : cr.checklist();
			for (ChecklistItemRequest item : items) {
				validateItem(item, max, step, at);
			}
			validateLevels(cr.levels(), max, step, at, kind);
		}
	}

	/**
	 * BR-31: mức đạt chỉ có ở bài PAPER; 2–6 mức, tên không trùng; mỗi khoảng [min, max] nằm trong [0, điểm tối đa], là
	 * bội số của bước; các khoảng không chồng nhau (không bắt buộc phủ kín thang điểm).
	 */
	static void validateLevels(List<LevelRequest> levels, BigDecimal max, BigDecimal step, String at,
			AssignmentKind kind) {
		if (levels == null || levels.isEmpty()) {
			return;
		}
		if (kind != AssignmentKind.PAPER) {
			throw invalid(at + ": chỉ bài Paper mới có mức đạt");
		}
		if (levels.size() < 2) {
			throw invalid(at + ": cần ít nhất 2 mức đạt");
		}
		Set<String> labels = new HashSet<>();
		for (LevelRequest l : levels) {
			String name = l.label().trim();
			if (!labels.add(name.toLowerCase(Locale.ROOT))) {
				throw invalid(at + ": mức đạt \"" + name + "\" bị lặp");
			}
			if (l.minScore().signum() < 0 || l.minScore().compareTo(l.maxScore()) > 0 || l.maxScore().compareTo(max) > 0) {
				throw invalid(at + ": mức \"" + name + "\" phải có 0 ≤ điểm thấp nhất ≤ điểm cao nhất ≤ "
						+ max.stripTrailingZeros().toPlainString());
			}
			if (!isMultiple(l.minScore(), step) || !isMultiple(l.maxScore(), step)) {
				throw invalid(at + ": khoảng điểm của mức \"" + name + "\" phải là bội số của bước điểm");
			}
		}
		List<LevelRequest> sorted = levels.stream().sorted(Comparator.comparing(LevelRequest::minScore)).toList();
		for (int i = 1; i < sorted.size(); i++) {
			if (sorted.get(i).minScore().compareTo(sorted.get(i - 1).maxScore()) <= 0) {
				throw invalid(at + ": mức \"" + sorted.get(i - 1).label().trim() + "\" và \"" + sorted.get(i).label().trim()
						+ "\" chồng khoảng điểm");
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
		return stepOf(cr, AssignmentKind.ESSAY);
	}

	static BigDecimal maxOf(CriterionRequest cr) {
		return maxOf(cr, AssignmentKind.ESSAY);
	}

	/** Không nhập bước điểm: band nguyên (ESSAY), 0,25 (PAPER). */
	static BigDecimal stepOf(CriterionRequest cr, AssignmentKind kind) {
		return cr.step() != null ? cr.step() : kind == AssignmentKind.PAPER ? QUARTER : WHOLE;
	}

	/** Không nhập điểm tối đa: 9 (ESSAY), 10 (PAPER). */
	static BigDecimal maxOf(CriterionRequest cr, AssignmentKind kind) {
		return cr.maxScore() != null ? cr.maxScore() : kind == AssignmentKind.PAPER ? PAPER_MAX : DEFAULT_MAX;
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
