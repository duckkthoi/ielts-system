package com.ltmtu.ielts.assignment;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentKind;
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
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.SectionDto;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.SectionRequest;
import com.ltmtu.ielts.common.ApiException;
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
		AssignmentKind kind = PaperRules.kindOf(req);
		a.setKind(kind);
		boolean paper = kind == AssignmentKind.PAPER;
		// Các trường của paper chỉ có ở bài PAPER (CHECK chk_assignment_paper_fields).
		a.setCitationStyle(paper ? (req.citationStyle() == null ? PaperRules.DEFAULT_CITATION : req.citationStyle())
				: null);
		a.setMaxGroupSize(paper ? (req.maxGroupSize() == null ? PaperRules.DEFAULT_GROUP_SIZE : req.maxGroupSize())
				: null);
		a.setGroupLockAt(paper ? req.groupLockAt() : null);
		List<PartRequest> parts = req.parts() == null ? List.of() : req.parts();
		for (int i = 0; i < parts.size(); i++) {
			a.addPart(toPart(parts.get(i), i + 1, kind));
		}
	}

	private static AssignmentPart toPart(PartRequest req, int orderNo, AssignmentKind kind) {
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
		p.setExamMode(Boolean.TRUE.equals(req.examMode()));
		// Không nhập thời lượng thi thì lấy thời gian gợi ý của phần (20 / 40 phút theo IELTS).
		Integer duration = req.examDurationMinutes() != null ? req.examDurationMinutes() : req.suggestedMinutes();
		if (p.isExamMode() && duration == null) {
			throw ApiException.badRequest("VALIDATION_ERROR",
					"Phần \"" + p.getTitle() + "\" bật chế độ thi nhưng chưa nhập thời gian thi");
		}
		p.setExamDurationMinutes(p.isExamMode() ? duration : null);
		List<CriterionRequest> criteria = req.criteria() == null ? List.of() : req.criteria();
		for (int i = 0; i < criteria.size(); i++) {
			p.addCriterion(toCriterion(criteria.get(i), i + 1, kind));
		}
		List<SectionRequest> sections = kind == AssignmentKind.PAPER && req.sections() != null ? req.sections()
				: List.of();
		Set<String> usedKeys = new HashSet<>();
		for (int i = 0; i < sections.size(); i++) {
			SectionRequest sr = sections.get(i);
			PaperSection s = new PaperSection();
			s.setOrderNo(i + 1);
			s.setTitle(sr.title().trim());
			s.setSectionKey(PaperRules.sectionKey(sr.sectionKey(), sr.title(), usedKeys));
			s.setRequired(!Boolean.FALSE.equals(sr.required()));
			s.setMinWords(sr.minWords());
			s.setMaxWords(sr.maxWords());
			p.addSection(s);
		}
		return p;
	}

	private static RubricCriterion toCriterion(CriterionRequest req, int orderNo, AssignmentKind kind) {
		RubricCriterion c = new RubricCriterion();
		c.setOrderNo(orderNo);
		c.setCode(req.code());
		c.setName(req.name().trim());
		c.setDescription(blankToNull(req.description()));
		c.setMaxScore(RubricRules.maxOf(req, kind));
		c.setStep(RubricRules.stepOf(req, kind));
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
				a.getParts().stream().map(AssignmentMapper::toPartDto).toList(), a.getKind(), a.getCitationStyle(),
				a.getMaxGroupSize(), a.getGroupLockAt());
	}

	private static PartDto toPartDto(AssignmentPart p) {
		return new PartDto(p.getId(), p.getOrderNo(), p.getTitle(), p.getTaskType(), p.getPrompt(), p.getImageUrl(),
				p.getMinWords(), p.getSuggestedMinutes(), p.getPartDueAt(), p.getWeight(),
				p.getCriteria().stream().map(AssignmentMapper::toCriterionDto).toList(), p.isExamMode(),
				p.getExamDurationMinutes(), toSectionDtos(p));
	}

	static List<SectionDto> toSectionDtos(AssignmentPart p) {
		return p.getSections().stream().map(s -> new SectionDto(s.getId(), s.getSectionKey(), s.getTitle(),
				s.isRequired(), s.getMinWords(), s.getMaxWords(), s.getOrderNo())).toList();
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
