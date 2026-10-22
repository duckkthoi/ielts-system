package com.ltmtu.ielts.grading;

import java.math.BigDecimal;
import java.util.Collection;

import org.springframework.http.HttpStatus;

import com.ltmtu.ielts.common.ApiException;

/**
 * Quy tắc kiểm tra điểm một tiêu chí (BR-11, BR-17, BR-18; docs/08 mục 3.1, 3.3). Class thuần để unit test;
 * server luôn kiểm tra lại, không tin giới hạn mà giao diện đã khoá.
 */
public final class GradingRules {

	/** BR-18: bài có số từ ≤ ngưỡng này thì mọi tiêu chí tối đa {@link #SHORT_ESSAY_MAX_BAND}. */
	public static final int SHORT_ESSAY_WORDS = 20;
	public static final BigDecimal SHORT_ESSAY_MAX_BAND = BigDecimal.ONE;

	private GradingRules() {
	}

	/** Mục checklist loại LIMIT đang được tick. */
	public record LimitMark(String content, BigDecimal maxBand) {
	}

	/**
	 * Điểm tối đa được phép của một tiêu chí.
	 *
	 * @param max    điểm tối đa của tiêu chí
	 * @param reason lý do bị giới hạn (hiển thị cho giáo viên và sinh viên); {@code null} khi không bị giới hạn
	 */
	public record ScoreCap(BigDecimal max, String reason) {

		public boolean limited() {
			return reason != null;
		}
	}

	/**
	 * BR-17: lấy {@code max_band} nhỏ nhất trong các mục LIMIT đang tick. BR-18: bài ≤ 20 từ thì tối đa 1.
	 * Trùng mức thì giữ lý do xuất hiện trước.
	 */
	public static ScoreCap cap(BigDecimal maxScore, Collection<LimitMark> checkedLimits, int wordCount) {
		BigDecimal max = maxScore;
		String reason = null;
		if (wordCount <= SHORT_ESSAY_WORDS && SHORT_ESSAY_MAX_BAND.compareTo(max) < 0) {
			max = SHORT_ESSAY_MAX_BAND;
			reason = "Bài chỉ có " + wordCount + " từ (≤ " + SHORT_ESSAY_WORDS + " từ)";
		}
		for (LimitMark mark : checkedLimits) {
			if (mark.maxBand() != null && mark.maxBand().compareTo(max) < 0) {
				max = mark.maxBand();
				reason = mark.content();
			}
		}
		return new ScoreCap(max, reason);
	}

	/**
	 * Kiểm tra một điểm: trong [0, max], là bội số của bước (BR-11), không vượt giới hạn (BR-17, BR-18).
	 * {@code null} = chưa chấm, hợp lệ khi lưu nháp.
	 */
	public static void requireValidScore(String criterionName, BigDecimal score, BigDecimal maxScore, BigDecimal step,
			ScoreCap cap) {
		if (score == null) {
			return;
		}
		if (score.signum() < 0 || score.compareTo(maxScore) > 0) {
			throw ApiException.badRequest("VALIDATION_ERROR",
					criterionName + ": điểm phải từ 0 tới " + plain(maxScore));
		}
		if (score.remainder(step).signum() != 0) {
			throw ApiException.badRequest("VALIDATION_ERROR",
					criterionName + ": điểm phải là bội số của " + plain(step));
		}
		if (score.compareTo(cap.max()) > 0) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "SCORE_LIMITED",
					criterionName + " bị giới hạn tối đa " + plain(cap.max()) + " vì: " + cap.reason());
		}
	}

	private static String plain(BigDecimal value) {
		return value.stripTrailingZeros().toPlainString();
	}
}
