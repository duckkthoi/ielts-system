package com.ltmtu.ielts.grading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.grading.GradingRules.LimitMark;
import com.ltmtu.ielts.grading.GradingRules.ScoreCap;

class GradingRulesTest {

	private static final BigDecimal NINE = BigDecimal.valueOf(9);
	private static final BigDecimal ONE = BigDecimal.ONE;
	private static final BigDecimal HALF = new BigDecimal("0.5");

	private static BigDecimal bd(String s) {
		return new BigDecimal(s);
	}

	@Test
	@DisplayName("Không tick mục giới hạn nào → tối đa bằng điểm tối đa của tiêu chí, không có lý do")
	void noLimit() {
		ScoreCap cap = GradingRules.cap(NINE, List.of(), 260);
		assertThat(cap.max()).isEqualByComparingTo("9");
		assertThat(cap.limited()).isFalse();
	}

	@Test
	@DisplayName("BR-17: nhiều mục giới hạn được tick → lấy max_band nhỏ nhất, kèm lý do của mục đó")
	void smallestLimitWins() {
		ScoreCap cap = GradingRules.cap(NINE, List.of(new LimitMark("Không chia đoạn", bd("5")),
				new LimitMark("Toàn bộ bài lạc đề", bd("2")), new LimitMark("Khác", bd("4"))), 260);
		assertThat(cap.max()).isEqualByComparingTo("2");
		assertThat(cap.reason()).isEqualTo("Toàn bộ bài lạc đề");
	}

	@Test
	@DisplayName("BR-18: bài ≤ 20 từ → tối đa 1; 21 từ thì không bị giới hạn")
	void shortEssay() {
		ScoreCap cap = GradingRules.cap(NINE, List.of(new LimitMark("Không chia đoạn", bd("5"))), 20);
		assertThat(cap.max()).isEqualByComparingTo("1");
		assertThat(cap.reason()).contains("20 từ");
		assertThat(GradingRules.cap(NINE, List.of(), 21).limited()).isFalse();
	}

	@Test
	@DisplayName("BR-17: điểm vượt giới hạn → 422 SCORE_LIMITED, message nêu lý do; bằng giới hạn thì hợp lệ")
	void scoreAboveCap() {
		ScoreCap cap = GradingRules.cap(NINE, List.of(new LimitMark("Không chia đoạn", bd("5"))), 260);
		assertThatThrownBy(() -> GradingRules.requireValidScore("Coherence and Cohesion", bd("6"), NINE, ONE, cap))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> {
					assertThat(((ApiException) e).getCode()).isEqualTo("SCORE_LIMITED");
					assertThat(((ApiException) e).getStatus().value()).isEqualTo(422);
				})
				.hasMessageContaining("tối đa 5").hasMessageContaining("Không chia đoạn");
		assertThatCode(() -> GradingRules.requireValidScore("CC", bd("5"), NINE, ONE, cap)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("BR-11: điểm ngoài [0, max] hoặc không phải bội số của bước → 400")
	void rangeAndStep() {
		ScoreCap cap = GradingRules.cap(NINE, List.of(), 260);
		assertThatThrownBy(() -> GradingRules.requireValidScore("TR", bd("9.5"), NINE, HALF, cap))
				.hasMessageContaining("từ 0 tới 9");
		assertThatThrownBy(() -> GradingRules.requireValidScore("TR", bd("-1"), NINE, ONE, cap))
				.hasMessageContaining("từ 0 tới 9");
		assertThatThrownBy(() -> GradingRules.requireValidScore("TR", bd("6.5"), NINE, ONE, cap))
				.hasMessageContaining("bội số của 1");
		assertThatCode(() -> GradingRules.requireValidScore("TR", bd("6.5"), NINE, HALF, cap)).doesNotThrowAnyException();
		assertThatCode(() -> GradingRules.requireValidScore("TR", bd("6.0"), NINE, ONE, cap)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Điểm null (chưa chấm) hợp lệ khi lưu nháp")
	void nullScoreAllowed() {
		assertThatCode(() -> GradingRules.requireValidScore("TR", null, NINE, ONE, GradingRules.cap(NINE, List.of(), 5)))
				.doesNotThrowAnyException();
	}
}
