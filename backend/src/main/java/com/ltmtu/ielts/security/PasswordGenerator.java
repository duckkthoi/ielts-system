package com.ltmtu.ielts.security;

import java.security.SecureRandom;

/**
 * Sinh mật khẩu tạm (BR-02): 8 ký tự, luôn có cả chữ và số.
 * Bỏ các ký tự dễ nhầm khi đọc trên giấy (0/O, 1/l/I) vì mật khẩu tạm được phát cho sinh viên qua file / email.
 */
public final class PasswordGenerator {

	public static final int LENGTH = 8;

	private static final String LETTERS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
	private static final String DIGITS = "23456789";
	private static final String ALL = LETTERS + DIGITS;
	private static final SecureRandom RANDOM = new SecureRandom();

	private PasswordGenerator() {
	}

	public static String temporaryPassword() {
		char[] chars = new char[LENGTH];
		chars[0] = LETTERS.charAt(RANDOM.nextInt(LETTERS.length()));
		chars[1] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
		for (int i = 2; i < LENGTH; i++) {
			chars[i] = ALL.charAt(RANDOM.nextInt(ALL.length()));
		}
		// Xáo trộn để vị trí chữ / số không cố định.
		for (int i = LENGTH - 1; i > 0; i--) {
			int j = RANDOM.nextInt(i + 1);
			char tmp = chars[i];
			chars[i] = chars[j];
			chars[j] = tmp;
		}
		return new String(chars);
	}
}
