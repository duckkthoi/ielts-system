package com.ltmtu.ielts.assignment.dto;

import java.time.Instant;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.CriterionDto;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.CriterionRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO của ngân hàng đề — UC-20 / FR-25. */
public final class QuestionBankDtos {

	private QuestionBankDtos() {
	}

	public record QuestionBankRequest(
			@NotBlank(message = "Vui lòng nhập tên đề") @Size(max = 150) String title,
			@NotNull(message = "Vui lòng chọn loại đề") TaskType taskType,
			@NotBlank(message = "Vui lòng nhập nội dung đề") @Size(max = 20000) String prompt,
			@Size(max = 500) String imageUrl,
			@Min(0) @Max(2000) Integer minWords,
			@Min(1) @Max(300) Integer suggestedMinutes,
			@Size(max = 20, message = "Tối đa 20 tag mỗi đề") List<@NotBlank @Size(max = 50) String> tags,
			@Size(max = 10, message = "Tối đa 10 tiêu chí mỗi đề") List<@Valid CriterionRequest> criteria) {
	}

	public record QuestionBankSummary(Long id, String title, TaskType taskType, String imageUrl, Integer minWords,
			Integer suggestedMinutes, List<String> tags, Instant createdAt, Instant updatedAt) {
	}

	public record QuestionBankDetail(Long id, String title, TaskType taskType, String prompt, String imageUrl,
			Integer minWords, Integer suggestedMinutes, List<String> tags, List<CriterionDto> criteria,
			Instant createdAt, Instant updatedAt) {
	}
}
