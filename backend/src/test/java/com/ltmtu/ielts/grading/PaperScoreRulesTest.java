package com.ltmtu.ielts.grading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.ltmtu.ielts.assignment.RubricLevel;
import com.ltmtu.ielts.common.ApiException;

/** Chấm bài PAPER thang 10 (BR-31): điểm hiển thị, mức đạt, cộng / trừ thành viên. */
class PaperScoreRulesTest {

	private static BigDecimal bd(String s) {
		return new BigDecimal(s);
	}

	@ParameterizedTest
	@CsvSource({ "7.666667, true, 7.67", "6.125000, true, 6.13", "6.125000, false, 6.0", "7.666667, false, 7.5" })
	@DisplayName("BR-31: bài PAPER hiện 2 chữ số thập phân; bài luận vẫn làm tròn band kiểu IELTS (BR-12)")
	void display(String raw, boolean paper, String expected) {
		assertThat(PaperScoreRules.display(bd(raw), paper)).isEqualByComparingTo(expected);
		assertThat(PaperScoreRules.display(null, paper)).isNull();
	}

	@ParameterizedTest
	@CsvSource({ "7.666667, 1, 8.67", "9.5, 1, 10.00", "1.2, -2, 0.00", "8, -0.25, 7.75" })
	@DisplayName("Điểm thành viên = điểm cả bài + cộng / trừ, kẹp trong [0, 10]")
	void memberScore(String raw, String delta, String expected) {
		assertThat(PaperScoreRules.memberScore(bd(raw), bd(delta))).isEqualByComparingTo(expected);
	}

	@Test
	@DisplayName("BR-31: điểm phải nằm trong khoảng của mức đạt đã chọn → nếu không 422 LEVEL_MISMATCH")
	void withinLevel() {
		RubricLevel good = new RubricLevel();
		good.setLabel("Tốt");
		good.setMinScore(bd("7"));
		good.setMaxScore(bd("8.75"));
		assertThatCode(() -> PaperScoreRules.requireWithinLevel("Phương pháp", good, bd("7"))).doesNotThrowAnyException();
		assertThatCode(() -> PaperScoreRules.requireWithinLevel("Phương pháp", good, bd("8.75"))).doesNotThrowAnyException();
		assertThatCode(() -> PaperScoreRules.requireWithinLevel("Phương pháp", null, bd("2"))).doesNotThrowAnyException();
		assertThatThrownBy(() -> PaperScoreRules.requireWithinLevel("Phương pháp", good, bd("9")))
				.isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("LEVEL_MISMATCH"))
				.hasMessage("Phương pháp: điểm 9 nằm ngoài mức \"Tốt\" (7–8.75)");
	}

	@Test
	@DisplayName("Cộng / trừ: khác 0, trong ±2, bước 0,25, bắt buộc lý do")
	void adjustment() {
		assertThatCode(() -> PaperScoreRules.requireValidAdjustment("An", bd("-2"), "Ít tham gia"))
				.doesNotThrowAnyException();
		assertThatCode(() -> PaperScoreRules.requireValidAdjustment("An", bd("0.75"), "Viết phần chính"))
				.doesNotThrowAnyException();
		assertThatThrownBy(() -> PaperScoreRules.requireValidAdjustment("An", bd("0"), "x")).hasMessageContaining("khác 0");
		assertThatThrownBy(() -> PaperScoreRules.requireValidAdjustment("An", bd("2.25"), "x"))
				.hasMessageContaining("từ -2 tới +2");
		assertThatThrownBy(() -> PaperScoreRules.requireValidAdjustment("An", bd("0.1"), "x"))
				.hasMessageContaining("bước 0,25");
		assertThatThrownBy(() -> PaperScoreRules.requireValidAdjustment("An", bd("1"), " ")).hasMessageContaining("lý do");
	}
}
