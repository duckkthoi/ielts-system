package com.ltmtu.ielts.paper.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * DTO của thư viện trích dẫn nhóm — docs/05-api.md mục A.10. Tài liệu tham khảo là bản ghi <b>CSL-JSON</b>; server lọc
 * trường (CitationRules) và trả kèm {@code missing} = các trường bắt buộc còn thiếu (BR-30) để client cảnh báo.
 */
public final class PaperReferenceDtos {

	private PaperReferenceDtos() {
	}

	/**
	 * Một tài liệu tham khảo. {@code missing} gồm {@code AUTHOR}, {@code YEAR}, {@code SOURCE} (tạp chí / hội nghị / nhà
	 * xuất bản / URL).
	 */
	public record ReferenceDto(Long id, JsonNode csl, String doi, List<String> missing, Long createdById,
			String createdByName, Instant createdAt, Instant updatedAt) {
	}

	/** POST / PUT một mục. */
	public record ReferenceRequest(@NotNull(message = "Thiếu bản ghi CSL") JsonNode csl) {
	}

	/** Nhập nhiều mục một lần (vd từ file BibTeX đã đổi sang CSL ở client). */
	public record BatchRequest(
			@NotEmpty(message = "Chưa có tài liệu nào để nhập")
			@Size(max = 100, message = "Mỗi lần nhập tối đa 100 tài liệu") List<JsonNode> items) {
	}

	/** {@code skipped}: số mục bỏ qua vì DOI đã có trong thư viện (hoặc lặp lại trong chính lần nhập). */
	public record BatchResult(List<ReferenceDto> added, int skipped) {
	}

	/** Kết quả tra DOI — chưa lưu; client cho xem / sửa rồi mới POST. */
	public record DoiLookup(String doi, JsonNode csl, List<String> missing) {
	}
}
