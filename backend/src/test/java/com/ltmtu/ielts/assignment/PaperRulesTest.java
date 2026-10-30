package com.ltmtu.ielts.assignment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PaperRulesTest {

	@ParameterizedTest(name = "\"{0}\" → {1}")
	@DisplayName("Mã mục: chữ thường không dấu, nối bằng gạch ngang")
	@CsvSource({ "Related Work, related-work", "Phương pháp đề xuất, phuong-phap-de-xuat", "  Kết quả & Đánh giá ,ket-qua-danh-gia",
			"Introduction, introduction" })
	void slug(String title, String expected) {
		assertThat(PaperRules.slug(title)).isEqualTo(expected);
	}

	@Test
	@DisplayName("Mã mục trùng trong một phần → thêm hậu tố -2, -3; tiêu đề toàn ký hiệu → \"muc\"")
	void uniqueKeys() {
		Set<String> used = new HashSet<>();
		assertThat(PaperRules.sectionKey(null, "Method", used)).isEqualTo("method");
		assertThat(PaperRules.sectionKey(null, "Method", used)).isEqualTo("method-2");
		assertThat(PaperRules.sectionKey("method", "Khác", used)).isEqualTo("method-3");
		assertThat(PaperRules.sectionKey(null, "???", used)).isEqualTo("muc");
	}
}
