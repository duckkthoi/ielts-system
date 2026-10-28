package com.ltmtu.ielts.assignment;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;
import com.ltmtu.ielts.assignment.RubricTemplate.Criterion;
import com.ltmtu.ielts.assignment.RubricTemplate.Item;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.ChecklistItemRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.CriterionRequest;
import com.ltmtu.ielts.assignment.dto.QuestionBankDtos.QuestionBankDetail;
import com.ltmtu.ielts.assignment.dto.QuestionBankDtos.QuestionBankRequest;
import com.ltmtu.ielts.assignment.dto.QuestionBankDtos.QuestionBankSummary;
import com.ltmtu.ielts.common.HtmlSanitizer;
import com.ltmtu.ielts.user.User;

/** Chuyển đổi đề trong ngân hàng và rubric riêng đi kèm. */
final class QuestionBankMapper {

	private QuestionBankMapper() {
	}

	static void apply(QuestionBankItem bankItem, QuestionBankRequest req, User teacher) {
		bankItem.setTitle(req.title().trim());
		bankItem.setTaskType(req.taskType());
		bankItem.setPrompt(HtmlSanitizer.sanitize(req.prompt()));
		bankItem.setImageUrl(blankToNull(req.imageUrl()));
		bankItem.setMinWords(req.minWords());
		bankItem.setSuggestedMinutes(req.suggestedMinutes());
		bankItem.getTags().clear();
		List<String> tags = req.tags() == null ? List.of() : req.tags();
		tags.stream().map(String::trim).filter(tag -> !tag.isEmpty()).map(tag -> tag.toLowerCase(Locale.ROOT))
				.forEach(bankItem.getTags()::add);

		RubricTemplate template = bankItem.getRubricTemplate();
		if (template == null) {
			template = new RubricTemplate();
			template.setOwner(teacher);
			bankItem.setRubricTemplate(template);
		}
		template.setName(bankItem.getTitle());
		template.setTaskType(req.taskType());
		template.setDescription("Rubric lưu cùng đề trong ngân hàng");
		template.getCriteria().clear();
		List<CriterionRequest> criteria = req.criteria() == null ? List.of() : req.criteria();
		for (int i = 0; i < criteria.size(); i++) {
			template.addCriterion(toCriterion(criteria.get(i), i + 1));
		}
	}

	private static Criterion toCriterion(CriterionRequest req, int orderNo) {
		Criterion criterion = new Criterion();
		criterion.setOrderNo(orderNo);
		criterion.setCode(req.code());
		criterion.setName(req.name().trim());
		criterion.setDescription(blankToNull(req.description()));
		criterion.setMaxScore(RubricRules.maxOf(req));
		criterion.setStep(RubricRules.stepOf(req));
		criterion.setWeight(req.weight() == null ? BigDecimal.ONE : req.weight());
		List<ChecklistItemRequest> checklist = req.checklist() == null ? List.of() : req.checklist();
		for (int i = 0; i < checklist.size(); i++) {
			ChecklistItemRequest request = checklist.get(i);
			Item item = new Item();
			item.setOrderNo(i + 1);
			item.setContent(request.content().trim());
			item.setKind(RubricRules.kindOf(request));
			item.setMaxBand(item.getKind() == ChecklistKind.LIMIT ? request.maxBand() : null);
			criterion.addItem(item);
		}
		return criterion;
	}

	static QuestionBankSummary toSummary(QuestionBankItem item) {
		return new QuestionBankSummary(item.getId(), item.getTitle(), item.getTaskType(), item.getImageUrl(),
				item.getMinWords(), item.getSuggestedMinutes(), sortedTags(item), item.getCreatedAt(), item.getUpdatedAt());
	}

	static QuestionBankDetail toDetail(QuestionBankItem item) {
		return new QuestionBankDetail(item.getId(), item.getTitle(), item.getTaskType(), item.getPrompt(),
				item.getImageUrl(), item.getMinWords(), item.getSuggestedMinutes(), sortedTags(item),
				AssignmentMapper.toTemplateDto(item.getRubricTemplate()).criteria(), item.getCreatedAt(), item.getUpdatedAt());
	}

	private static List<String> sortedTags(QuestionBankItem item) {
		return new LinkedHashSet<>(item.getTags()).stream().sorted().toList();
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
