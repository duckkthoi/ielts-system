package com.ltmtu.ielts.grading;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;

import com.ltmtu.ielts.assignment.RubricLevel;
import com.ltmtu.ielts.common.ApiException;

/**
 * Chấm bài PAPER thang 10 (BR-31, docs/12 mục 5.4). Class thuần để unit test.
 * <ul>
 * <li>Điểm cả bài = trung bình có trọng số các tiêu chí (cùng công thức {@link BandCalculator#partBand}), hiển thị 2 chữ
 * số thập phân — <b>không</b> làm tròn .25 / .75 của IELTS (BR-12) và không áp giới hạn bài ≤ 20 từ (BR-18).</li>
 * <li>Chọn mức đạt thì điểm phải nằm trong khoảng của mức.</li>
 * <li>Cộng / trừ riêng từng thành viên: tối đa ±2, bội số 0,25, bắt buộc lý do; điểm thành viên kẹp trong [0, 10].</li>
 * </ul>
 */
public final class PaperScoreRules {

	public static final BigDecimal MAX_SCORE = BigDecimal.TEN;
	public static final BigDecimal MAX_ADJUSTMENT = new BigDecimal("2");
	public static final BigDecimal ADJUSTMENT_STEP = new BigDecimal("0.25");

	private PaperScoreRules() {
	}

	/** Điểm hiển thị: bài PAPER làm tròn 2 chữ số thập phân; bài luận theo quy ước band IELTS. */
	public static BigDecimal display(BigDecimal raw, boolean paper) {
		if (!paper) {
			return BandCalculator.display(raw);
		}
		return raw == null ? null : raw.setScale(2, RoundingMode.HALF_UP);
	}

	/** Điểm của một thành viên = điểm cả bài (đã làm tròn hiển thị) + delta, kẹp trong [0, 10]. */
	public static BigDecimal memberScore(BigDecimal raw, BigDecimal delta) {
		BigDecimal base = display(raw, true);
		if (base == null) {
			return null;
		}
		BigDecimal v = delta == null ? base : base.add(delta);
		return v.max(BigDecimal.ZERO).min(MAX_SCORE).setScale(2, RoundingMode.UNNECESSARY);
	}

	/** Điểm đã chọn phải nằm trong khoảng của mức đạt đã chọn → nếu không 422 {@code LEVEL_MISMATCH}. */
	public static void requireWithinLevel(String criterionName, RubricLevel level, BigDecimal score) {
		if (level != null && score != null && !level.contains(score)) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "LEVEL_MISMATCH",
					criterionName + ": điểm " + plain(score) + " nằm ngoài mức \"" + level.getLabel() + "\" ("
							+ plain(level.getMinScore()) + "–" + plain(level.getMaxScore()) + ")");
		}
	}

	/** Một mục cộng / trừ: khác 0, |delta| ≤ 2, bội số 0,25, có lý do → nếu không 400. */
	public static void requireValidAdjustment(String who, BigDecimal delta, String reason) {
		if (delta == null || delta.signum() == 0) {
			throw invalid(who + ": điểm cộng / trừ phải khác 0 (bỏ dòng nếu không cộng / trừ)");
		}
		if (delta.abs().compareTo(MAX_ADJUSTMENT) > 0 || delta.remainder(ADJUSTMENT_STEP).signum() != 0) {
			throw invalid(who + ": điểm cộng / trừ từ -2 tới +2, bước 0,25");
		}
		if (reason == null || reason.isBlank()) {
			throw invalid(who + ": cần ghi lý do cộng / trừ điểm");
		}
	}

	private static ApiException invalid(String message) {
		return ApiException.badRequest("VALIDATION_ERROR", message);
	}

	private static String plain(BigDecimal value) {
		return value.stripTrailingZeros().toPlainString();
	}
}
