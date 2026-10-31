package com.ltmtu.ielts.audit;

/** Các hành động được ghi nhật ký (docs/04 mục audit_logs). */
public enum AuditAction {
	LOGIN,
	LOGIN_FAILED,
	/** Đăng nhập bị chặn vì sai quá nhiều lần (NFR-09). */
	LOGIN_BLOCKED,
	CREATE_TEACHER,
	IMPORT_TEACHERS,
	LOCK_USER,
	UNLOCK_USER,
	RESET_PASSWORD,
	/** Admin thu hồi một phiên đăng nhập (UC-08). */
	REVOKE_SESSION,
	/** Admin thu hồi mọi phiên của một tài khoản. */
	REVOKE_USER_SESSIONS,
	/** Admin dọn các phiên đã hết hạn. */
	CLEANUP_SESSIONS,
	IMPORT_STUDENTS,
	TCP_IMPORT,
	PUBLISH_ASSIGNMENT,
	RETURN_GRADE,
	REQUEST_REVISION
}
