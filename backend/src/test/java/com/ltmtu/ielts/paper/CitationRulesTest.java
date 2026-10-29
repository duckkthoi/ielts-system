package com.ltmtu.ielts.paper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.ltmtu.ielts.common.ApiException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Tài liệu tham khảo CSL-JSON: chuẩn hoá DOI, lọc trường, trường còn thiếu (docs/12 mục 5.3, BR-30). */
class CitationRulesTest {

	private static final ObjectMapper JSON = JsonMapper.builder().build();
	private static final int MAX = 16 * 1024;

	private static JsonNode csl(String s) {
		return JSON.readTree(s);
	}

	@ParameterizedTest
	@CsvSource({
			"10.1145/3368089.3409731, 10.1145/3368089.3409731",
			"https://doi.org/10.1109/TSE.2020.123, 10.1109/tse.2020.123",
			"http://dx.doi.org/10.1000/ABC, 10.1000/abc",
			"'doi: 10.1000/xyz ', 10.1000/xyz" })
	@DisplayName("DOI: bỏ tiền tố doi.org / doi:, khoảng trắng; chuyển chữ thường")
	void normalizeDoi(String raw, String expected) {
		assertThat(CitationRules.normalizeDoi(raw)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "abc", "10.12/x", "11.1000/abc", "10.1000/", "10.1000/có khoảng" })
	@DisplayName("DOI sai dạng → 400")
	void invalidDoi(String raw) {
		assertThatThrownBy(() -> CitationRules.normalizeDoi(raw)).isInstanceOf(ApiException.class)
				.hasMessageContaining("DOI không hợp lệ");
	}

	@Test
	@DisplayName("Lọc CSL: chỉ giữ trường cho phép, tên tác giả chỉ giữ phần tên, DOI chuẩn hoá, kiểu mặc định document")
	void sanitizeKeepsAllowedFields() {
		JsonNode input = csl("""
				{"title": "  Deep Learning ", "author": [{"given": "Y.", "family": "LeCun", "affiliation": [{"name": "NYU"}],
				 "ORCID": "x"}], "issued": {"date-parts": [[2015, 5]]}, "container-title": "Nature", "volume": "521",
				 "DOI": "https://doi.org/10.1038/NATURE14539", "reference": [{"key": "r1"}], "score": 1, "id": "abc",
				 "ISSN": ["0028-0836", "1476-4687"], "custom": {"x": 1}}""");
		var s = CitationRules.sanitize(input, MAX);

		assertThat(s.doi()).isEqualTo("10.1038/nature14539");
		ObjectNode out = s.csl();
		assertThat(out.get("type").asString()).isEqualTo("document");
		assertThat(out.get("title").asString()).isEqualTo("Deep Learning");
		assertThat(out.get("DOI").asString()).isEqualTo("10.1038/nature14539");
		assertThat(out.get("author").get(0).propertyNames()).containsExactlyInAnyOrder("given", "family");
		assertThat(out.get("ISSN")).hasSize(2);
		assertThat(out.has("reference")).isFalse();
		assertThat(out.has("score")).isFalse();
		assertThat(out.has("id")).isFalse();
		assertThat(out.has("custom")).isFalse();
		assertThat(CitationRules.missingFields(out)).isEmpty();
	}

	@Test
	@DisplayName("Lọc CSL: tối đa 100 tác giả; quá kích thước sau khi lọc → 400")
	void sanitizeLimits() {
		ObjectNode input = JSON.createObjectNode().put("title", "Big");
		ArrayNode authors = input.putArray("author");
		for (int i = 0; i < 150; i++) {
			authors.addObject().put("family", "Tác giả " + i);
		}
		assertThat(CitationRules.sanitize(input, MAX).csl().get("author")).hasSize(CitationRules.MAX_NAMES);

		input.put("note", "x".repeat(CitationRules.MAX_TEXT + 50));
		assertThat(CitationRules.sanitize(input, MAX).csl().get("note").asString()).hasSize(CitationRules.MAX_TEXT);
		assertThatThrownBy(() -> CitationRules.sanitize(input, 1024)).isInstanceOf(ApiException.class)
				.hasMessageContaining("quá lớn");
	}

	@Test
	@DisplayName("Lọc CSL: thiếu tiêu đề, không phải object, DOI sai → 400")
	void sanitizeRejects() {
		assertThatThrownBy(() -> CitationRules.sanitize(csl("{\"author\": []}"), MAX))
				.hasMessageContaining("cần có tiêu đề");
		assertThatThrownBy(() -> CitationRules.sanitize(csl("[1, 2]"), MAX)).hasMessageContaining("CSL-JSON");
		assertThatThrownBy(() -> CitationRules.sanitize(csl("{\"title\": \"A\", \"DOI\": \"abc\"}"), MAX))
				.hasMessageContaining("DOI không hợp lệ");
	}

	@Test
	@DisplayName("BR-30: thiếu tác giả / năm / nguồn → liệt kê để cảnh báo; biên tập viên, năm dạng chữ, URL đều được tính")
	void missingFields() {
		assertThat(CitationRules.missingFields(csl("{\"title\": \"A\"}")))
				.containsExactly(CitationRules.MISSING_AUTHOR, CitationRules.MISSING_YEAR, CitationRules.MISSING_SOURCE);
		assertThat(CitationRules.missingFields(csl("""
				{"title": "A", "editor": [{"family": "B"}], "issued": {"literal": "2021"}, "URL": "https://x.org"}""")))
				.isEmpty();
		assertThat(CitationRules.missingFields(csl("""
				{"title": "A", "author": [{"literal": "W3C"}], "issued": {"date-parts": [[]]}, "publisher": "MIT"}""")))
				.containsExactly(CitationRules.MISSING_YEAR);
	}
}
