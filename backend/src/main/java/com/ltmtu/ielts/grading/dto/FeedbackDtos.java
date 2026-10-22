package com.ltmtu.ielts.grading.dto;

import java.time.Instant;

import com.ltmtu.ielts.grading.CommentBankItem;
import com.ltmtu.ielts.grading.ErrorCode;
import com.ltmtu.ielts.grading.GradingEnums.CriterionGroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Mã lỗi và ngân hàng nhận xét của giáo viên — docs/05-api.md mục A.7 (BR-21, BR-22). */
public final class FeedbackDtos {

	private FeedbackDtos() {
	}

	/** {@code system = true}: mã của hệ thống, không sửa / xoá được. */
	public record ErrorCodeDto(Long id, String code, String name, CriterionGroup criterion, String description,
			boolean positive, boolean system) {

		public static ErrorCodeDto from(ErrorCode e) {
			return new ErrorCodeDto(e.getId(), e.getCode(), e.getName(), e.getCriterion(), e.getDescription(),
					e.isPositive(), e.isSystem());
		}
	}

	/** Mã được lưu dạng chữ hoa; không chứa khoảng trắng. */
	public record ErrorCodeRequest(
			@NotBlank(message = "Vui lòng nhập mã lỗi")
			@Pattern(regexp = "^\\s*[\\p{L}\\p{N}/^?!+_.\\-]{1,20}\\s*$",
					message = "Mã lỗi dài tối đa 20 ký tự, không có khoảng trắng") String code,
			@NotBlank(message = "Vui lòng nhập tên mã lỗi") @Size(max = 100, message = "Tên mã lỗi tối đa 100 ký tự") String name,
			CriterionGroup criterion,
			@Size(max = 500, message = "Mô tả tối đa 500 ký tự") String description,
			Boolean positive) {
	}

	/** {@code system = true}: nhận xét mẫu của hệ thống, chỉ đọc. */
	public record CommentBankDto(Long id, CriterionGroup criterion, String content, int useCount, Instant lastUsedAt,
			boolean system) {

		public static CommentBankDto from(CommentBankItem c) {
			return new CommentBankDto(c.getId(), c.getCriterion(), c.getContent(), c.getUseCount(), c.getLastUsedAt(),
					c.isSystem());
		}
	}

	public record CommentBankRequest(
			CriterionGroup criterion,
			@NotBlank(message = "Vui lòng nhập nội dung nhận xét")
			@Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự") String content) {
	}
}
