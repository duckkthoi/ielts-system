package com.ltmtu.ielts.grading;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ltmtu.ielts.assignment.RubricCriterion;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.LevelDto;
import com.ltmtu.ielts.grading.GradingEnums.CriterionGroup;
import com.ltmtu.ielts.grading.GradingRules.ScoreCap;
import com.ltmtu.ielts.grading.dto.GradingDtos.AdjustmentDto;
import com.ltmtu.ielts.grading.dto.GradingDtos.ChecklistInfo;
import com.ltmtu.ielts.grading.dto.GradingDtos.CriterionInfo;
import com.ltmtu.ielts.grading.dto.GradingDtos.GradeDto;
import com.ltmtu.ielts.grading.dto.GradingDtos.InlineCommentDto;
import com.ltmtu.ielts.grading.dto.GradingDtos.MarkDto;
import com.ltmtu.ielts.grading.dto.GradingDtos.ScoreDto;

/** Ánh xạ kết quả chấm sang DTO. */
final class GradeMapper {

	private GradeMapper() {
	}

	static List<CriterionInfo> criteria(List<RubricCriterion> criteria) {
		return criteria.stream()
				.map(c -> new CriterionInfo(c.getId(), c.getCode(), CriterionGroup.of(c.getCode()), c.getName(),
						c.getDescription(), c.getMaxScore(), c.getStep(), c.getWeight(),
						c.getChecklist().stream()
								.map(i -> new ChecklistInfo(i.getId(), i.getContent(), i.getKind(), i.getMaxBand()))
								.toList(),
						c.getLevels().stream().map(l -> new LevelDto(l.getId(), l.getLabel(), l.getMinScore(),
								l.getMaxScore(), l.getDescriptor(), l.getOrderNo())).toList()))
				.toList();
	}

	/**
	 * @param criteria  tiêu chí của phần, theo thứ tự hiển thị
	 * @param wordCount số từ của version (BR-18), để tính giới hạn điểm hiện tại của từng tiêu chí
	 * @param paper     bài PAPER: điểm thang 10 (không làm tròn kiểu IELTS, không BR-18), kèm cộng / trừ thành viên
	 */
	static GradeDto toDto(Grade g, List<RubricCriterion> criteria, int wordCount, boolean paper) {
		Map<Long, GradeCriterionScore> scores = g.getScores().stream()
				.collect(Collectors.toMap(s -> s.getCriterion().getId(), Function.identity()));
		Map<Long, Boolean> checked = g.getMarks().stream()
				.collect(Collectors.toMap(m -> m.getItem().getId(), GradeChecklistMark::isChecked));
		List<ScoreDto> scoreDtos = criteria.stream().map(c -> {
			GradeCriterionScore s = scores.get(c.getId());
			ScoreCap cap = GradeUpdater.capOf(c, checked, wordCount, paper);
			return new ScoreDto(c.getId(), s == null ? null : s.getScore(), s == null ? null : s.getComment(),
					cap.max(), cap.reason(), s == null || s.getLevel() == null ? null : s.getLevel().getId());
		}).toList();
		List<MarkDto> marks = criteria.stream().flatMap(c -> c.getChecklist().stream())
				.map(i -> new MarkDto(i.getId(), checked.getOrDefault(i.getId(), false)))
				.toList();
		List<InlineCommentDto> inline = g.getInlineComments().stream()
				.sorted(Comparator.comparingInt(InlineComment::getStartOffset).thenComparing(InlineComment::getId,
						Comparator.nullsLast(Comparator.naturalOrder())))
				.map(GradeMapper::toInlineDto)
				.toList();
		return new GradeDto(g.getId(), g.getVersion().getVersionNo(), g.getStatus(), g.getZeroReason(),
				g.getGeneralComment(), g.getOverallScore(), PaperScoreRules.display(g.getOverallScore(), paper),
				g.getOverallScore() != null, g.getGrader().getFullName(), g.getUpdatedAt(), g.getReturnedAt(),
				scoreDtos, marks, inline, adjustments(g));
	}

	/** Cộng / trừ theo tên sinh viên (ổn định khi hiển thị). */
	static List<AdjustmentDto> adjustments(Grade g) {
		return g.getAdjustments().stream()
				.map(a -> new AdjustmentDto(a.getStudent().getId(), a.getStudent().getFullName(), a.getDelta(),
						a.getReason(), PaperScoreRules.memberScore(g.getOverallScore(), a.getDelta())))
				.sorted(Comparator.comparing(AdjustmentDto::studentName))
				.toList();
	}

	static InlineCommentDto toInlineDto(InlineComment ic) {
		return new InlineCommentDto(ic.getId(), ic.getStartOffset(), ic.getEndOffset(), ic.getQuote(), ic.getComment(),
				ic.getErrorCode(), ic.getCriterion() == null ? null : ic.getCriterion().getId(), ic.getSuggestion(),
				ic.getKind(), ic.getEdit(), ic.getResolution());
	}
}
