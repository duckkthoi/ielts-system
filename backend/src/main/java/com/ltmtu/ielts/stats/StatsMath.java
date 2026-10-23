package com.ltmtu.ielts.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ltmtu.ielts.grading.BandCalculator;
import com.ltmtu.ielts.stats.dto.AssignmentStatsDtos.BandCount;

/**
 * Phép tính cho số liệu thống kê. Class thuần, không phụ thuộc Spring. Trung bình giữ 2 chữ số thập phân, <b>không</b>
 * làm tròn kiểu IELTS (đây là số liệu tổng hợp, không phải band của một bài); không có dữ liệu → {@code null}.
 */
final class StatsMath {

	private StatsMath() {
	}

	static BigDecimal average(Collection<BigDecimal> values) {
		if (values.isEmpty()) {
			return null;
		}
		BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
	}

	static BigDecimal round(BigDecimal value) {
		return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
	}

	/** Phân bố theo band đã làm tròn hiển thị (BR-12, bước 0.5), chỉ band có bài, tăng dần. */
	static List<BandCount> distribution(Collection<BigDecimal> rawBands) {
		Map<BigDecimal, Integer> counts = new TreeMap<>();
		rawBands.forEach(b -> counts.merge(BandCalculator.display(b), 1, Integer::sum));
		return counts.entrySet().stream().map(e -> new BandCount(e.getKey(), e.getValue())).toList();
	}
}
