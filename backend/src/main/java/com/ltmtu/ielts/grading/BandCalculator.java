package com.ltmtu.ielts.grading;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Cách tính band (BR-12, docs/08 mục 3.2). Class thuần, không phụ thuộc Spring.
 * <ul>
 * <li>Band phần = trung bình có trọng số điểm các tiêu chí.</li>
 * <li>Band bài = trung bình có trọng số band các phần (mặc định Task 2 trọng số 2, Task 1 trọng số 1).</li>
 * <li>Giữ số thực khi tính; <b>chỉ làm tròn khi hiển thị</b> theo quy ước .25 / .75 của IELTS.</li>
 * </ul>
 */
public final class BandCalculator {

	/**
	 * Số chữ số thập phân khi lưu / tính tiếp. Với trọng số ≤ 2 chữ số thập phân và điểm bội số của 0.5, khoảng cách
	 * thật tới ngưỡng làm tròn (.25 / .75) hoặc bằng 0 hoặc lớn hơn nhiều so với sai số 10⁻⁶, nên không lệch ngưỡng.
	 */
	public static final int SCALE = 6;

	private static final BigDecimal QUARTER = new BigDecimal("0.25");
	private static final BigDecimal THREE_QUARTERS = new BigDecimal("0.75");
	private static final BigDecimal HALF = new BigDecimal("0.5");

	private BandCalculator() {
	}

	/** Một giá trị kèm trọng số: điểm tiêu chí, hoặc band của một phần. */
	public record Weighted(BigDecimal value, BigDecimal weight) {
	}

	/** Band của một phần. {@code null} khi chưa chấm đủ mọi tiêu chí. */
	public static BigDecimal partBand(Collection<Weighted> criterionScores) {
		return weightedAverage(criterionScores);
	}

	/** Band của cả bài. {@code null} khi còn phần chưa có band (docs/08 mục 3.2 ý 4: "Chưa đủ phần"). */
	public static BigDecimal assignmentBand(Collection<Weighted> partBands) {
		return weightedAverage(partBands);
	}

	/**
	 * Làm tròn để hiển thị: phần lẻ &lt; .25 → xuống số nguyên; .25 tới &lt; .75 → .5; ≥ .75 → lên số nguyên kế tiếp.
	 * Ví dụ 6.583 → 6.5; 6.75 → 7.0; 6.125 → 6.0.
	 */
	public static BigDecimal display(BigDecimal raw) {
		if (raw == null) {
			return null;
		}
		BigDecimal whole = raw.setScale(0, RoundingMode.FLOOR);
		BigDecimal fraction = raw.subtract(whole);
		BigDecimal rounded;
		if (fraction.compareTo(QUARTER) < 0) {
			rounded = whole;
		} else if (fraction.compareTo(THREE_QUARTERS) < 0) {
			rounded = whole.add(HALF);
		} else {
			rounded = whole.add(BigDecimal.ONE);
		}
		return rounded.setScale(1, RoundingMode.UNNECESSARY);
	}

	private static BigDecimal weightedAverage(Collection<Weighted> items) {
		if (items == null || items.isEmpty()) {
			return null;
		}
		BigDecimal sum = BigDecimal.ZERO;
		BigDecimal totalWeight = BigDecimal.ZERO;
		for (Weighted item : items) {
			if (item.value() == null) {
				return null;
			}
			sum = sum.add(item.value().multiply(item.weight()));
			totalWeight = totalWeight.add(item.weight());
		}
		if (totalWeight.signum() <= 0) {
			return null;
		}
		return sum.divide(totalWeight, SCALE, RoundingMode.HALF_UP);
	}
}
