package com.ltmtu.ielts.assignment;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.assignment.dto.QuestionBankDtos.QuestionBankDetail;
import com.ltmtu.ielts.assignment.dto.QuestionBankDtos.QuestionBankRequest;
import com.ltmtu.ielts.assignment.dto.QuestionBankDtos.QuestionBankSummary;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.Pageables;
import com.ltmtu.ielts.user.User;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Quản lý ngân hàng đề riêng của từng giáo viên (UC-20 / FR-25). */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankService {

	private static final Map<String, String> SORT = Map.of("title", "title", "taskType", "taskType", "createdAt",
			"createdAt", "updatedAt", "updatedAt");

	private final QuestionBankItemRepository repository;
	private final RubricTemplateRepository templateRepository;
	private final UserQueryService userQueryService;

	/** UC-20: tìm đề của chính giáo viên theo từ khoá, loại Task và tag. */
	@Transactional(readOnly = true)
	public Page<QuestionBankSummary> search(Long teacherId, TaskType taskType, String keyword, String tag,
			Pageable pageable) {
		Pageable safePage = Pageables.sanitize(pageable, SORT, Sort.by(Sort.Direction.DESC, "updatedAt"));
		Page<Long> ids = repository.searchIds(teacherId, taskType, Pageables.likePattern(keyword), normalizeTag(tag), safePage);
		if (ids.isEmpty()) {
			return new PageImpl<>(List.of(), safePage, ids.getTotalElements());
		}
		Map<Long, QuestionBankItem> items = repository.findSummaries(ids.getContent()).stream()
				.collect(Collectors.toMap(QuestionBankItem::getId, Function.identity()));
		List<QuestionBankSummary> content = ids.getContent().stream().map(items::get)
				.map(QuestionBankMapper::toSummary).toList();
		return new PageImpl<>(content, safePage, ids.getTotalElements());
	}

	/** UC-20: xem đầy đủ một đề và rubric thuộc giáo viên. */
	@Transactional(readOnly = true)
	public QuestionBankDetail detail(Long teacherId, Long id) {
		return QuestionBankMapper.toDetail(requireOwned(teacherId, id));
	}

	/** UC-20 / FR-25: lưu đề, tag và một bản rubric riêng để dùng lại. */
	@Transactional
	public QuestionBankDetail create(Long teacherId, QuestionBankRequest request) {
		RubricRules.validate(request);
		User teacher = userQueryService.requireUser(teacherId);
		QuestionBankItem item = new QuestionBankItem();
		item.setTeacher(teacher);
		QuestionBankMapper.apply(item, request, teacher);
		requirePrompt(item);
		repository.save(item);
		log.info("Giáo viên {} lưu đề {} vào ngân hàng", teacherId, item.getId());
		return QuestionBankMapper.toDetail(item);
	}

	/** UC-20: thay toàn bộ nội dung đề, tag và rubric; không ảnh hưởng bài tập đã nạp trước đó. */
	@Transactional
	public QuestionBankDetail replace(Long teacherId, Long id, QuestionBankRequest request) {
		RubricRules.validate(request);
		QuestionBankItem item = requireOwned(teacherId, id);
		// Xoá trước các collection có khoá duy nhất theo thứ tự để tránh INSERT mới chạy trước DELETE cũ.
		item.getTags().clear();
		item.getRubricTemplate().getCriteria().clear();
		repository.saveAndFlush(item);
		templateRepository.flush();
		QuestionBankMapper.apply(item, request, item.getTeacher());
		requirePrompt(item);
		repository.saveAndFlush(item);
		return QuestionBankMapper.toDetail(item);
	}

	/** UC-20: xoá đề của chính giáo viên cùng rubric riêng đi kèm. */
	@Transactional
	public void delete(Long teacherId, Long id) {
		QuestionBankItem item = requireOwned(teacherId, id);
		repository.delete(item);
		log.info("Giáo viên {} xoá đề {} khỏi ngân hàng", teacherId, id);
	}

	private QuestionBankItem requireOwned(Long teacherId, Long id) {
		return repository.findByIdAndTeacherId(id, teacherId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy đề trong ngân hàng"));
	}

	private static void requirePrompt(QuestionBankItem item) {
		if (item.getPrompt() == null || item.getPrompt().isBlank()) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Nội dung đề trống sau khi lọc định dạng");
		}
	}

	private static String normalizeTag(String tag) {
		return tag == null || tag.isBlank() ? null : tag.trim().toLowerCase(Locale.ROOT);
	}
}
