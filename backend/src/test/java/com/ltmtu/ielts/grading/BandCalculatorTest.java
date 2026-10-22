package com.ltmtu.ielts.grading;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.ltmtu.ielts.grading.BandCalculator.Weighted;

class BandCalculatorTest {

	private static List<Weighted> equalWeights(String... scores) {
		return Arrays.stream(scores).map(s -> new Weighted(new BigDecimal(s), BigDecimal.ONE)).toList();
	}

	private static BigDecimal bd(String s) {
		return new BigDecimal(s);
	}

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({
			"6.0, 6.0", "6.125, 6.0", "6.249999, 6.0",
			"6.25, 6.5", "6.5, 6.5", "6.583333, 6.5", "6.749999, 6.5",
			"6.75, 7.0", "6.875, 7.0", "0, 0.0", "8.75, 9.0", "9, 9.0"
	})
	@DisplayName("BR-12: làm tròn khi hiển thị theo quy ước .25 / .75")
	void displayRounding(String raw, String expected) {
		assertThat(BandCalculator.display(bd(raw))).isEqualByComparingTo(expected);
		assertThat(BandCalculator.display(bd(raw)).scale()).isEqualTo(1);
	}

	@Test
	@DisplayName("BR-12: band phần = trung bình 4 tiêu chí, giữ số thực (6, 6, 7, 6 → 6.25, hiển thị 6.5)")
	void partBandKeepsFraction() {
		BigDecimal raw = BandCalculator.partBand(equalWeights("6", "6", "7", "6"));
		assertThat(raw).isEqualByComparingTo("6.25");
		assertThat(BandCalculator.display(raw)).isEqualByComparingTo("6.5");
	}

	@Test
	@DisplayName("BR-12: ví dụ docs/08 — Task 1 = 6.25, Task 2 = 6.75 (trọng số 2) → bài 6.583 → hiển thị 6.5")
	void assignmentBandTask2Weighted() {
		BigDecimal task1 = BandCalculator.partBand(equalWeights("6", "6", "7", "6"));
		BigDecimal task2 = BandCalculator.partBand(equalWeights("7", "7", "6", "7"));
		BigDecimal overall = BandCalculator.assignmentBand(
				List.of(new Weighted(task1, BigDecimal.ONE), new Weighted(task2, BigDecimal.valueOf(2))));
		assertThat(overall).isEqualByComparingTo("6.583333");
		assertThat(BandCalculator.display(overall)).isEqualByComparingTo("6.5");
		assertThat(BandCalculator.display(task1)).isEqualByComparingTo("6.5");
		assertThat(BandCalculator.display(task2)).isEqualByComparingTo("7.0");
	}

	@Test
	@DisplayName("Nếu làm tròn từng phần trước rồi mới gộp sẽ ra kết quả khác — hệ thống không làm vậy")
	void noIntermediateRounding() {
		// Task 1 = 5.75 (hiển thị 6.0), Task 2 = 6.25 (hiển thị 6.5).
		BigDecimal task1 = BandCalculator.partBand(equalWeights("6", "6", "5", "6"));
		BigDecimal task2 = BandCalculator.partBand(equalWeights("6", "6", "7", "6"));
		BigDecimal overall = BandCalculator.assignmentBand(
				List.of(new Weighted(task1, BigDecimal.ONE), new Weighted(task2, BigDecimal.valueOf(2))));
		// (5.75 + 2 × 6.25) / 3 = 6.083 → 6.0; làm tròn trước: (6.0 + 2 × 6.5) / 3 = 6.33 → 6.5.
		assertThat(BandCalculator.display(overall)).isEqualByComparingTo("6.0");
	}

	@Test
	@DisplayName("Trọng số tiêu chí khác nhau và điểm bước 0.5")
	void customWeights() {
		BigDecimal raw = BandCalculator.partBand(List.of(new Weighted(bd("6.5"), bd("2")), new Weighted(bd("5"), bd("1"))));
		assertThat(raw).isEqualByComparingTo("6");
	}

	@Test
	@DisplayName("Chưa chấm đủ tiêu chí / còn phần chưa có band → null (\"Chưa đủ phần\")")
	void incomplete() {
		assertThat(BandCalculator.partBand(List.of(new Weighted(bd("6"), BigDecimal.ONE), new Weighted(null, BigDecimal.ONE))))
				.isNull();
		assertThat(BandCalculator.assignmentBand(List.of())).isNull();
		assertThat(BandCalculator.display(null)).isNull();
	}
}
