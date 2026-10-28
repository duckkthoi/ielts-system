package com.ltmtu.ielts.assignment;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.assignment.AssignmentRepository.ProgressCounts;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentDetail;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentRow;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentSettingsRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentSummary;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.RubricTemplateDto;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.Pageables;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Soạn bài tập (UC-13), sửa bài đã phát hành (BR-06), xoá bài nháp, danh sách kèm số liệu nộp bài. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {

	private static final Map<String, String> SORT = Map.of("title", "title", "dueAt", "dueAt", "createdAt",
			"createdAt", "updatedAt", "updatedAt");

	private final AssignmentRepository assignmentRepository;
	private final RubricTemplateRepository templateRepository;
	private final UserQueryService userQueryService;

	@Transactional(readOnly = true)
	public Page<AssignmentSummary> search(Long teacherId, AssignmentStatus status, String keyword, Pageable pageable) {
		Page<AssignmentRow> page = assignmentRepository.searchOfTeacher(teacherId, status, Pageables.likePattern(keyword),
				Pageables.sanitize(pageable, SORT, Sort.by(Sort.Direction.DESC, "updatedAt")));
		List<Long> ids = page.map(AssignmentRow::id).getContent();
		Map<Long, ProgressCounts> counts = ids.isEmpty() ? Map.of()
				: assignmentRepository.progressCounts(ids).stream()
						.collect(Collectors.toMap(ProgressCounts::getAssignmentId, Function.identity()));
		return page.map(r -> {
			ProgressCounts c = counts.get(r.id());
			return new AssignmentSummary(r.id(), r.title(), r.status(), r.openAt(), r.dueAt(), r.partCount(),
					r.publishedAt(), r.createdAt(), r.updatedAt(), c == null ? 0 : c.getRecipients(),
					c == null ? 0 : c.getCompleted(), c == null ? 0 : c.getPending());
		});
	}

	@Transactional(readOnly = true)
	public AssignmentDetail detail(Long teacherId, Long id) {
		return AssignmentMapper.toDetail(requireOwned(teacherId, id));
	}

	/** UC-13: tạo bài ở trạng thái nháp. Bài nháp chưa cần đủ phần / tiêu chí (kiểm tra khi phát hành). */
	@Transactional
	public AssignmentDetail create(Long teacherId, AssignmentRequest req) {
		RubricRules.validate(req);
		Assignment a = new Assignment();
		a.setTeacher(userQueryService.requireUser(teacherId));
		AssignmentMapper.apply(a, req);
		requireNonEmptyPrompts(a);
		assignmentRepository.save(a);
		log.info("Giáo viên {} tạo bài tập {} ({} phần)", teacherId, a.getId(), a.getParts().size());
		return AssignmentMapper.toDetail(a);
	}

	/** Thay toàn bộ bài nháp (thông tin + các phần + rubric). Bài đã phát hành → dùng {@link #updateSettings}. */
	@Transactional
	public AssignmentDetail replaceDraft(Long teacherId, Long id, AssignmentRequest req) {
		Assignment a = requireOwned(teacherId, id);
		if (!a.isDraft()) {
			throw ApiException.conflict("INVALID_STATE",
					"Bài đã phát hành chỉ sửa được thông tin chung (tiêu đề, mô tả, hạn nộp, nộp muộn, số lần sửa)");
		}
		RubricRules.validate(req);
		// Xoá các phần cũ và flush trước khi thêm phần mới: Hibernate chạy INSERT trước DELETE khi flush,
		// sẽ vi phạm UNIQUE (assignment_id, order_no) nếu làm trong một lần.
		a.getParts().clear();
		assignmentRepository.saveAndFlush(a);
		AssignmentMapper.apply(a, req);
		requireNonEmptyPrompts(a);
		assignmentRepository.saveAndFlush(a);
		return AssignmentMapper.toDetail(a);
	}

	/**
	 * Sửa thông tin không làm sai lệch bài đã nộp (BR-06) — dùng được ở mọi trạng thái.
	 * Đổi hạn nộp không làm thay đổi cờ "nộp muộn" của các version đã nộp.
	 */
	@Transactional
	public AssignmentDetail updateSettings(Long teacherId, Long id, AssignmentSettingsRequest req) {
		Assignment a = requireOwned(teacherId, id);
		if (req.title() != null) {
			if (req.title().isBlank()) {
				throw ApiException.badRequest("VALIDATION_ERROR", "Tên bài tập không được trống");
			}
			a.setTitle(req.title().trim());
		}
		if (req.description() != null) {
			a.setDescription(req.description().isBlank() ? null : req.description().trim());
		}
		if (req.dueAt() != null) {
			RubricRules.requireOrder(a.getOpenAt(), req.dueAt());
			a.setDueAt(req.dueAt());
		}
		if (req.allowLate() != null) {
			a.setAllowLate(req.allowLate());
		}
		if (req.maxRevisions() != null) {
			a.setMaxRevisions(req.maxRevisions());
		}
		if (req.showSuggestionsOnRevision() != null) {
			a.setShowSuggestionsOnRevision(req.showSuggestionsOnRevision());
		}
		if (req.partDueAts() != null) {
			req.partDueAts().forEach((partId, dueAt) -> a.getParts().stream()
					.filter(p -> Objects.equals(p.getId(), partId))
					.findFirst()
					.orElseThrow(() -> ApiException.badRequest("VALIDATION_ERROR", "Phần " + partId + " không thuộc bài tập này"))
					.setPartDueAt(dueAt));
		}
		if (req.groupLockAt() != null || req.maxGroupSize() != null) {
			applyGroupSettings(a, req);
		}
		assignmentRepository.flush();
		return AssignmentMapper.toDetail(a);
	}

	/** Bài PAPER: hạn chốt nhóm, cỡ nhóm (BR-27). Không giảm cỡ nhóm xuống dưới nhóm đông nhất đang có. */
	private void applyGroupSettings(Assignment a, AssignmentSettingsRequest req) {
		if (!a.isPaper()) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Chỉ bài paper mới có nhóm");
		}
		if (req.groupLockAt() != null) {
			PaperRules.requireLockBeforeDue(req.groupLockAt(), a.getDueAt());
			a.setGroupLockAt(req.groupLockAt());
		}
		if (req.maxGroupSize() != null) {
			int largest = assignmentRepository.largestGroupSize(a.getId());
			if (req.maxGroupSize() < largest) {
				throw ApiException.badRequest("VALIDATION_ERROR",
						"Đang có nhóm " + largest + " người — cỡ nhóm không được nhỏ hơn " + largest);
			}
			a.setMaxGroupSize(req.maxGroupSize());
		}
	}

	/** Chỉ xoá được bài nháp; bài đã phát hành thì đóng bài (B4b). */
	@Transactional
	public void delete(Long teacherId, Long id) {
		Assignment a = requireOwned(teacherId, id);
		if (!a.isDraft()) {
			throw ApiException.conflict("INVALID_STATE", "Chỉ xoá được bài tập ở trạng thái nháp; bài đã phát hành hãy đóng bài");
		}
		assignmentRepository.delete(a);
		log.info("Giáo viên {} xoá bài tập nháp {}", teacherId, id);
	}

	@Transactional(readOnly = true)
	public List<RubricTemplateDto> templates(Long teacherId, TaskType taskType) {
		return templateRepository.findAvailable(teacherId, taskType).stream().map(AssignmentMapper::toTemplateDto)
				.toList();
	}

	/** Bài tập của giáo viên khác → 404 (BR-15, docs/05 A.1). */
	Assignment requireOwned(Long teacherId, Long id) {
		return assignmentRepository.findByIdAndTeacherId(id, teacherId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy bài tập"));
	}

	/** Đề bài chỉ gồm thẻ bị lọc (vd chỉ có &lt;script&gt;) sẽ thành rỗng sau khi lọc HTML. */
	private static void requireNonEmptyPrompts(Assignment a) {
		for (AssignmentPart p : a.getParts()) {
			if (p.getPrompt() == null || p.getPrompt().isBlank()) {
				throw ApiException.badRequest("VALIDATION_ERROR", "Phần " + p.getOrderNo() + ": đề bài trống sau khi lọc định dạng");
			}
		}
	}
}
