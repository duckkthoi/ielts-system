package com.ltmtu.ielts.assignment;

import java.math.BigDecimal;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentDetail;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.ChecklistItemDto;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.ChecklistItemRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.CriterionDto;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.CriterionRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.PartDto;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.PartRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.RubricTemplateDto;
import com.ltmtu.ielts.common.HtmlSanitizer;

/** Chuyển đổi request → entity (kèm giá trị mặc định) và entity → DTO. */
final class AssignmentMapper {

	private AssignmentMapper() {
	}

	/** Gán thông tin chung + toàn bộ các phần từ request (dùng khi tạo / thay bài nháp). */
	static void apply(Assignment a, AssignmentRequest req) {
		a.setTitle(req.title().trim());
		a.setDescription(blankToNull(req.description()));
		a.setOpenAt(req.openAt());
		a.setDueAt(req.dueAt());
		// Trường boolean dùng kiểu Boolean: Jackson 3 báo lỗi nếu thiếu trường kiểu nguyên thuỷ trong record.
		a.setAllowLate(Boolean.TRUE.equals(req.allowLate()));
		a.setMaxRevisions(req.maxRevisions() == null ? 2 : req.maxRevisions());
		a.setShowSuggestionsOnRevision(Boolean.TRUE.equals(req.showSuggestionsOnRevision()));
		List<PartRequest> parts = req.parts() == null ? List.of() : req.parts();
		for (int i = 0; i < parts.size(); i++) {
			a.addPart(toPart(parts.get(i), i + 1));
		}
	}

	private static AssignmentPart toPart(PartRequest req, int orderNo) {
		AssignmentPart p = new AssignmentPart();
		p.setOrderNo(orderNo);
		p.setTitle(req.title().trim());
		p.setTaskType(req.taskType());
		p.setPrompt(HtmlSanitizer.sanitize(req.prompt()));
		p.setImageUrl(blankToNull(req.imageUrl()));
		p.setMinWords(req.minWords());
		p.setSuggestedMinutes(req.suggestedMinutes());
		p.setPartDueAt(req.partDueAt());
		p.setWeight(req.weight() == null ? req.taskType().defaultWeight() : req.weight());
		List<CriterionRequest> criteria = req.criteria() == null ? List.of() : req.criteria();
		for (int i = 0; i < criteria.size(); i++) {
			p.addCriterion(toCriterion(criteria.get(i), i + 1));
		}
		return p;
	}

	private static RubricCriterion toCriterion(CriterionRequest req, int orderNo) {
		RubricCriterion c = new RubricCriterion();
		c.setOrderNo(orderNo);
		c.setCode(req.code());
		c.setName(req.name().trim());
		c.setDescription(blankToNull(req.description()));
		c.setMaxScore(RubricRules.maxOf(req));
		c.setStep(RubricRules.stepOf(req));
		c.setWeight(req.weight() == null ? BigDecimal.ONE : req.weight());
		List<ChecklistItemRequest> items = req.checklist() == null ? List.of() : req.checklist();
		for (int i = 0; i < items.size(); i++) {
			ChecklistItemRequest ir = items.get(i);
			ChecklistItem item = new ChecklistItem();
			item.setOrderNo(i + 1);
			item.setContent(ir.content().trim());
			item.setKind(RubricRules.kindOf(ir));
			// Mục "Đạt" không có band tối đa (ràng buộc chk_checklist_limit).
			item.setMaxBand(item.getKind() == ChecklistKind.LIMIT ? ir.maxBand() : null);
			c.addItem(item);
		}
		return c;
	}

	static AssignmentDetail toDetail(Assignment a) {
		return new AssignmentDetail(a.getId(), a.getTitle(), a.getDescription(), a.getStatus(), a.getOpenAt(),
				a.getDueAt(), a.isAllowLate(), a.getMaxRevisions(), a.isShowSuggestionsOnRevision(),
				a.getPublishedAt(), a.getCreatedAt(), a.getUpdatedAt(),
				a.getParts().stream().map(AssignmentMapper::toPartDto).toList());
	}

	private static PartDto toPartDto(AssignmentPart p) {
		return new PartDto(p.getId(), p.getOrderNo(), p.getTitle(), p.getTaskType(), p.getPrompt(), p.getImageUrl(),
				p.getMinWords(), p.getSuggestedMinutes(), p.getPartDueAt(), p.getWeight(),
				p.getCriteria().stream().map(AssignmentMapper::toCriterionDto).toList());
	}

	private static CriterionDto toCriterionDto(RubricCriterion c) {
		return new CriterionDto(c.getId(), c.getCode(), c.getName(), c.getDescription(), c.getMaxScore(), c.getStep(),
				c.getWeight(), c.getOrderNo(), c.getChecklist().stream()
						.map(i -> new ChecklistItemDto(i.getId(), i.getContent(), i.getKind(), i.getMaxBand(), i.getOrderNo()))
						.toList());
	}

	static RubricTemplateDto toTemplateDto(RubricTemplate t) {
		return new RubricTemplateDto(t.getId(), t.getName(), t.getTaskType(), t.getDescription(), t.getOwner() == null,
				t.getCriteria().stream()
						.map(c -> new CriterionDto(c.getId(), c.getCode(), c.getName(), c.getDescription(),
								c.getMaxScore(), c.getStep(), c.getWeight(), c.getOrderNo(), c.getChecklist().stream()
										.map(i -> new ChecklistItemDto(i.getId(), i.getContent(), i.getKind(),
												i.getMaxBand(), i.getOrderNo()))
										.toList()))
						.toList());
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
