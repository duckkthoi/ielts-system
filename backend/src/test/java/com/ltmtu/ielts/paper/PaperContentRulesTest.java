package com.ltmtu.ielts.paper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ltmtu.ielts.paper.PaperContentRules.RequiredSection;
import com.ltmtu.ielts.paper.PaperContentRules.SectionStat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Đọc JSON trình soạn của bài PAPER khi nộp (B25a): nội dung từng mục (BR-29), số từ, tài liệu được trích (BR-30). */
class PaperContentRulesTest {

	private static final ObjectMapper JSON = JsonMapper.builder().build();

	private static final JsonNode DOC = JSON.readTree("""
			{"type": "doc", "content": [
			  {"type": "paperSection", "attrs": {"sectionKey": "abstract", "title": "Abstract Title Words"}, "content": [
			    {"type": "paragraph", "content": [{"type": "text", "text": "We propose"},
			      {"type": "citation", "attrs": {"refIds": "7,3"}}, {"type": "text", "text": " a system."}]},
			    {"type": "paragraph", "content": [{"type": "inlineMath", "attrs": {"latex": "E = mc^2"}}]}]},
			  {"type": "paperSection", "attrs": {"sectionKey": "figures", "title": "Figures"}, "content": [
			    {"type": "image", "attrs": {"src": "/uploads/a.png"}}]},
			  {"type": "paperSection", "attrs": {"sectionKey": "method", "title": "Method"}, "content": [
			    {"type": "paragraph"}]},
			  {"type": "paperSection", "attrs": {"sectionKey": "related", "title": "Related"}, "content": [
			    {"type": "bulletList", "content": [{"type": "listItem", "content": [{"type": "paragraph", "content": [
			      {"type": "text", "text": "prior work"}, {"type": "citation", "attrs": {"refIds": "3, 9,x"}}]}]}]}]}
			]}""");

	@Test
	@DisplayName("BR-29: mục có chữ / hình là có nội dung, mục chỉ có đoạn trống là trống; số từ không tính tiêu đề, công thức")
	void sectionStats() {
		Map<String, SectionStat> stats = PaperContentRules.sections(DOC);
		assertThat(stats.keySet()).containsExactly("abstract", "figures", "method", "related");
		assertThat(stats.get("abstract")).isEqualTo(new SectionStat(true, 4));
		assertThat(stats.get("figures")).isEqualTo(new SectionStat(true, 0));
		assertThat(stats.get("method")).isEqualTo(new SectionStat(false, 0));
		assertThat(stats.get("related")).isEqualTo(new SectionStat(true, 2));
	}

	@Test
	@DisplayName("BR-29: liệt kê mục bắt buộc còn trống theo thứ tự khung; mục không có trong tài liệu cũng tính là trống")
	void emptyRequired() {
		Map<String, SectionStat> stats = PaperContentRules.sections(DOC);
		assertThat(PaperContentRules.emptyRequired(stats, List.of(new RequiredSection("abstract", "Abstract"),
				new RequiredSection("method", "Method"), new RequiredSection("conclusion", "Conclusion"))))
				.containsExactly("Method", "Conclusion");
	}

	@Test
	@DisplayName("BR-30: tài liệu được trích theo thứ tự xuất hiện lần đầu, bỏ trùng và phần không phải số")
	void citedReferenceIds() {
		assertThat(PaperContentRules.citedReferenceIds(DOC)).containsExactly(7L, 3L, 9L);
		assertThat(PaperContentRules.citedReferenceIds(JSON.readTree("{\"type\": \"doc\"}"))).isEmpty();
	}
}
