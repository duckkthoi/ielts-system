package com.ltmtu.ielts.submission;

import java.util.regex.Pattern;

/**
 * Đếm từ theo quy ước IELTS (BR-20, docs/08-nghiep-vu-cham-ielts.md mục 3.4):
 * tách theo khoảng trắng (kể cả khoảng trắng không ngắt); mỗi cụm có ít nhất một chữ cái hoặc chữ số là một từ.
 * Vì vậy từ nối gạch ngang, từ viết tắt, số kèm ký hiệu (20%) đều là 1 từ; dấu câu đứng riêng không tính;
 * từ viết dính / tách sai được đếm đúng như sinh viên viết.
 * <p>
 * Frontend dùng cùng thuật toán ({@code frontend/lib/utils/wordCount.ts}); cả hai chạy bộ ca kiểm thử chung
 * {@code shared/word-count-cases.json}.
 */
public final class WordCounter {

	private static final Pattern SEPARATOR = Pattern.compile("[\\s\\u00A0\\u2007\\u202F]+");
	private static final Pattern HAS_WORD_CHAR = Pattern.compile("[\\p{L}\\p{N}]");

	private WordCounter() {
	}

	public static int count(String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}
		int n = 0;
		for (String token : SEPARATOR.split(text.strip())) {
			if (HAS_WORD_CHAR.matcher(token).find()) {
				n++;
			}
		}
		return n;
	}
}
