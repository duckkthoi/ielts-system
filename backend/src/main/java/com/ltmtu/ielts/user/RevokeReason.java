package com.ltmtu.ielts.user;

/** Lý do thu hồi phiên đăng nhập (docs/06-trang-thai.md mục 5). */
public enum RevokeReason {
	LOGOUT,
	/** Đăng nhập ở nơi khác (chính sách một phiên — Mức 2). */
	NEW_LOGIN,
	PASSWORD_CHANGED,
	ADMIN_FORCED,
	/** Refresh token cũ bị dùng lại sau thời gian ân hạn — nghi bị đánh cắp. */
	TOKEN_REUSE,
	ACCOUNT_LOCKED
}
