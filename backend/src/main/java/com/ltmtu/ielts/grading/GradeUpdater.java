package com.ltmtu.ielts.grading;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;
import com.ltmtu.ielts.assignment.ChecklistItem;
import com.ltmtu.ielts.assignment.RubricCriterion;
import com.ltmtu.ielts.assignment.RubricLevel;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.grading.BandCalculator.Weighted;
import com.ltmtu.ielts.grading.GradingEnums.CriterionGroup;
import com.ltmtu.ielts.grading.GradingEnums.EditOp;
import com.ltmtu.ielts.grading.GradingEnums.InlineKind;
import com.ltmtu.ielts.grading.GradingRules.LimitMark;
import com.ltmtu.ielts.grading.GradingRules.ScoreCap;
import com.ltmtu.ielts.grading.dto.GradingDtos.AdjustmentRequest;
import com.ltmtu.ielts.grading.dto.GradingDtos.InlineCommentRequest;
import com.ltmtu.ielts.grading.dto.GradingDtos.MarkRequest;
import com.ltmtu.ielts.grading.dto.GradingDtos.SaveGradeRequest;
import com.ltmtu.ielts.grading.dto.GradingDtos.ScoreRequest;
import com.ltmtu.ielts.user.User;

/**
 * Áp body "lưu chấm" vào một {@link Grade} (thay toàn bộ), kiểm tra mọi quy tắc trước khi đổi dữ liệu:
 * BR-11 / BR-17 / BR-18 (điểm), BR-19 ("Chấm 0"), BR-21 (nhận xét inline); bài PAPER thêm mức đạt + cộng / trừ thành
 * viên, bỏ BR-18 (BR-31). Không phụ thuộc Spring.
 */
final class GradeUpdater {

	private GradeUpdater() {
	}

	/**
	 * @param criteria tiêu chí của phần (kèm checklist)
	 * @param content  nội dung version đang chấm — offset nhận xét inline tính trên chuỗi này
	 * @param codes    mã lỗi dùng được (của hệ thống + của giáo viên), khoá là mã viết hoa
	 * @param paper    bài PAPER: chọn mức đạt, không áp BR-18, có cộng / trừ thành viên (BR-31)
	 * @param members  bài PAPER: thành viên hiện tại của nhóm theo id (được cộng / trừ); bài luận → rỗng
	 */
	static void apply(Grade grade, List<RubricCriterion> criteria, String content, int wordCount,
			SaveGradeRequest req, Map<String, ErrorCode> codes, Instant now, boolean paper, Map<Long, User> members) {
		Map<Long, RubricCriterion> criteriaById = criteria.stream()
				.collect(Collectors.toMap(RubricCriterion::getId, Function.identity()));
		Map<Long, ScoreRequest> scores = indexScores(req.scores(), criteriaById);
		Map<Long, Boolean> checked = indexMarks(req.checklist(), criteria);
		boolean zero = req.zeroReason() != null;
		if (zero && scores.values().stream().anyMatch(s -> s.score() != null && s.score().signum() != 0)) {
			throw invalid("\"Chấm 0\": mọi tiêu chí phải là 0 (BR-19)");
		}

		// Kiểm tra và tính hết trước, rồi mới ghi vào entity: lỗi ở tiêu chí sau không để lại thay đổi dở dang.
		Map<Long, BigDecimal> finalScores = new HashMap<>();
		Map<Long, RubricLevel> finalLevels = new HashMap<>();
		for (RubricCriterion c : criteria) {
			ScoreRequest s = scores.get(c.getId());
			BigDecimal score = zero ? BigDecimal.ZERO : (s == null ? null : s.score());
			RubricLevel level = levelOf(c, s, paper);
			if (!zero) {
				GradingRules.requireValidScore(c.getName(), score, c.getMaxScore(), c.getStep(),
						capOf(c, checked, wordCount, paper));
				PaperScoreRules.requireWithinLevel(c.getName(), level, score);
			}
			// Cột điểm 2 chữ số thập phân (bước 0,25 của bài PAPER); điểm đã được kiểm là bội số của bước.
			finalScores.put(c.getId(), score == null ? null : score.setScale(2, RoundingMode.UNNECESSARY));
			finalLevels.put(c.getId(), zero ? null : level);
		}
		List<InlineComment> inline = buildInline(req.inlineComments(), criteria, criteriaById, content, codes, now);
		Map<Long, AdjustmentRequest> adjustments = indexAdjustments(req.memberAdjustments(), paper, members);

		Map<Long, GradeCriterionScore> existingScores = grade.getScores().stream()
				.collect(Collectors.toMap(s -> s.getCriterion().getId(), Function.identity()));
		Map<Long, GradeChecklistMark> existingMarks = grade.getMarks().stream()
				.collect(Collectors.toMap(m -> m.getItem().getId(), Function.identity()));
		List<Weighted> weighted = new ArrayList<>();
		for (RubricCriterion c : criteria) {
			GradeCriterionScore row = existingScores.get(c.getId());
			if (row == null) {
				row = new GradeCriterionScore(grade, c);
				grade.getScores().add(row);
			}
			ScoreRequest s = scores.get(c.getId());
			row.setScore(finalScores.get(c.getId()));
			row.setLevel(finalLevels.get(c.getId()));
			row.setComment(s == null ? null : blankToNull(s.comment()));
			weighted.add(new Weighted(row.getScore(), c.getWeight()));
			for (ChecklistItem item : c.getChecklist()) {
				GradeChecklistMark mark = existingMarks.get(item.getId());
				if (mark == null) {
					mark = new GradeChecklistMark(grade, item);
					grade.getMarks().add(mark);
				}
				mark.setChecked(checked.getOrDefault(item.getId(), false));
			}
		}
		grade.getInlineComments().clear();
		inline.forEach(ic -> {
			ic.setGrade(grade);
			grade.getInlineComments().add(ic);
		});
		grade.setGeneralComment(blankToNull(req.generalComment()));
		grade.setZeroReason(req.zeroReason());
		grade.setOverallScore(BandCalculator.partBand(weighted));
		applyAdjustments(grade, adjustments, members);
	}

	/** Giới hạn điểm của một tiêu chí theo các mục LIMIT đang tick và số từ (bài PAPER không áp BR-18). */
	static ScoreCap capOf(RubricCriterion c, Map<Long, Boolean> checked, int wordCount, boolean paper) {
		List<LimitMark> limits = c.getChecklist().stream()
				.filter(i -> i.getKind() == ChecklistKind.LIMIT && checked.getOrDefault(i.getId(), false))
				.map(i -> new LimitMark(i.getContent(), i.getMaxBand()))
				.toList();
		return GradingRules.cap(c.getMaxScore(), limits, wordCount, !paper);
	}

	/** Mức đạt đã chọn cho tiêu chí: phải thuộc tiêu chí; chỉ bài PAPER mới chọn mức (BR-31). */
	private static RubricLevel levelOf(RubricCriterion c, ScoreRequest s, boolean paper) {
		if (s == null || s.levelId() == null) {
			return null;
		}
		if (!paper) {
			throw invalid(c.getName() + ": chỉ bài Paper mới chọn mức đạt");
		}
		return c.getLevels().stream().filter(l -> l.getId().equals(s.levelId())).findFirst()
				.orElseThrow(() -> invalid(c.getName() + ": mức đạt " + s.levelId() + " không thuộc tiêu chí này"));
	}

	/** Cộng / trừ thành viên: chỉ bài PAPER, người phải là thành viên hiện tại của nhóm, mỗi người một dòng. */
	private static Map<Long, AdjustmentRequest> indexAdjustments(List<AdjustmentRequest> list, boolean paper,
			Map<Long, User> members) {
		Map<Long, AdjustmentRequest> map = new HashMap<>();
		List<AdjustmentRequest> items = Objects.requireNonNullElse(list, List.of());
		if (!items.isEmpty() && !paper) {
			throw invalid("Chỉ bài Paper mới cộng / trừ điểm riêng từng thành viên");
		}
		for (AdjustmentRequest a : items) {
			User member = members.get(a.studentId());
			if (member == null) {
				throw invalid("Sinh viên " + a.studentId() + " không thuộc nhóm của bài này");
			}
			PaperScoreRules.requireValidAdjustment(member.getFullName(), a.delta(), a.reason());
			if (map.put(a.studentId(), a) != null) {
				throw invalid(member.getFullName() + ": bị gửi hai lần");
			}
		}
		return map;
	}

	/** Cập nhật tại chỗ theo khoá (grade, sinh viên) — xoá hết rồi thêm lại sẽ vướng INSERT trước DELETE của Hibernate. */
	private static void applyAdjustments(Grade grade, Map<Long, AdjustmentRequest> wanted, Map<Long, User> members) {
		grade.getAdjustments().removeIf(a -> !wanted.containsKey(a.getStudent().getId()));
		Map<Long, GradeMemberAdjustment> existing = grade.getAdjustments().stream()
				.collect(Collectors.toMap(a -> a.getStudent().getId(), Function.identity()));
		wanted.forEach((studentId, req) -> {
			BigDecimal delta = req.delta().setScale(2, RoundingMode.UNNECESSARY);
			GradeMemberAdjustment row = existing.get(studentId);
			if (row == null) {
				grade.getAdjustments().add(new GradeMemberAdjustment(grade, members.get(studentId), delta,
						req.reason().trim()));
			} else {
				row.setDelta(delta);
				row.setReason(req.reason().trim());
			}
		});
	}

	private static Map<Long, ScoreRequest> indexScores(List<ScoreRequest> scores, Map<Long, RubricCriterion> criteria) {
		Map<Long, ScoreRequest> map = new HashMap<>();
		for (ScoreRequest s : Objects.requireNonNullElse(scores, List.<ScoreRequest>of())) {
			if (!criteria.containsKey(s.criterionId())) {
				throw invalid("Tiêu chí " + s.criterionId() + " không thuộc phần này");
			}
			if (map.put(s.criterionId(), s) != null) {
				throw invalid("Tiêu chí " + criteria.get(s.criterionId()).getName() + " bị gửi hai lần");
			}
		}
		return map;
	}

	private static Map<Long, Boolean> indexMarks(List<MarkRequest> marks, List<RubricCriterion> criteria) {
		Map<Long, ChecklistItem> items = criteria.stream().flatMap(c -> c.getChecklist().stream())
				.collect(Collectors.toMap(ChecklistItem::getId, Function.identity()));
		Map<Long, Boolean> map = new HashMap<>();
		for (MarkRequest m : Objects.requireNonNullElse(marks, List.<MarkRequest>of())) {
			if (!items.containsKey(m.checklistItemId())) {
				throw invalid("Mục checklist " + m.checklistItemId() + " không thuộc phần này");
			}
			if (map.put(m.checklistItemId(), Boolean.TRUE.equals(m.checked())) != null) {
				throw invalid("Mục checklist " + m.checklistItemId() + " bị gửi hai lần");
			}
		}
		return map;
	}

	/**
	 * BR-21: vị trí hợp lệ, đoạn trích khớp nội dung, mã lỗi tồn tại. Lỗi (ERROR) cần ít nhất mã lỗi / nhận xét / gợi ý
	 * sửa; đánh dấu vàng / xanh được để trống nhưng không mang mã lỗi (để thống kê lỗi không lẫn). Không gửi
	 * {@code kind} = ERROR, trừ khi mã là mã khen → GOOD (tương thích client cũ).
	 */
	private static List<InlineComment> buildInline(List<InlineCommentRequest> requests, List<RubricCriterion> criteria,
			Map<Long, RubricCriterion> criteriaById, String content, Map<String, ErrorCode> codes, Instant now) {
		List<InlineComment> result = new ArrayList<>();
		List<InlineCommentRequest> list = Objects.requireNonNullElse(requests, List.of());
		for (int i = 0; i < list.size(); i++) {
			InlineCommentRequest r = list.get(i);
			String at = "Nhận xét inline thứ " + (i + 1);
			if (r.startOffset() < 0 || r.endOffset() <= r.startOffset() || r.endOffset() > content.length()) {
				throw invalid(at + ": vị trí đoạn văn không hợp lệ");
			}
			String quote = content.substring(r.startOffset(), r.endOffset());
			if (r.quote() != null && !r.quote().equals(quote)) {
				throw invalid(at + ": đoạn trích không khớp với nội dung bài nộp");
			}
			String comment = blankToNull(r.comment());
			String suggestion = blankToNull(r.suggestion());
			ErrorCode code = null;
			if (blankToNull(r.errorCode()) != null) {
				String key = r.errorCode().trim().toUpperCase(Locale.ROOT);
				code = codes.get(key);
				if (code == null) {
					throw invalid(at + ": mã lỗi '" + key + "' không tồn tại");
				}
			}
			InlineKind kind = r.kind() != null ? r.kind()
					: code != null && code.isPositive() ? InlineKind.GOOD : InlineKind.ERROR;
			// Có gợi ý sửa mà không nói kiểu sửa = thay thế (client cũ).
			EditOp edit = r.edit() != null ? r.edit() : suggestion != null && kind == InlineKind.ERROR ? EditOp.REPLACE : null;
			if (kind == InlineKind.ERROR && code == null && comment == null && suggestion == null && edit == null) {
				throw invalid(at + ": cần có mã lỗi, lời nhận xét, gợi ý sửa hoặc thao tác sửa");
			}
			if (kind != InlineKind.ERROR && code != null && !code.isPositive()) {
				throw invalid(at + ": chỉ đánh dấu lỗi mới gắn mã lỗi '" + code.getCode() + "'");
			}
			if (kind == InlineKind.COMMENT && comment == null) {
				throw invalid(at + ": nhận xét không được để trống");
			}
			if (edit != null && kind != InlineKind.ERROR) {
				throw invalid(at + ": chỉ đánh dấu lỗi mới kèm thao tác sửa");
			}
			if ((edit == EditOp.REPLACE || edit == EditOp.INSERT) && suggestion == null) {
				throw invalid(at + ": cần nhập chữ mới");
			}
			if (edit == EditOp.DELETE && suggestion != null) {
				throw invalid(at + ": xoá chữ thì không kèm chữ mới");
			}
			RubricCriterion criterion = null;
			if (r.criterionId() != null) {
				criterion = criteriaById.get(r.criterionId());
				if (criterion == null) {
					throw invalid(at + ": tiêu chí " + r.criterionId() + " không thuộc phần này");
				}
			} else if (code != null && code.getCriterion() != null) {
				criterion = matchCriterion(criteria, code.getCriterion());
			}
			InlineComment ic = new InlineComment();
			ic.setStartOffset(r.startOffset());
			ic.setEndOffset(r.endOffset());
			ic.setQuote(quote);
			ic.setComment(comment);
			ic.setErrorCode(code == null ? null : code.getCode());
			ic.setCriterion(criterion);
			ic.setSuggestion(suggestion);
			ic.setKind(kind);
			ic.setEdit(edit);
			ic.setCreatedAt(now);
			result.add(ic);
		}
		return result;
	}

	/** Mã lỗi nhóm TASK khớp tiêu chí TA hoặc TR của phần; không có tiêu chí cùng nhóm → để trống. */
	private static RubricCriterion matchCriterion(List<RubricCriterion> criteria, CriterionGroup group) {
		return criteria.stream().filter(c -> CriterionGroup.of(c.getCode()) == group).findFirst().orElse(null);
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	private static ApiException invalid(String message) {
		return ApiException.badRequest("VALIDATION_ERROR", message);
	}
}
