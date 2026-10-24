package com.ltmtu.ielts.audit;

/** Các hành động được ghi nhật ký (docs/04 mục audit_logs). */
public enum AuditAction {
	LOGIN,
	LOGIN_FAILED,
	/** Đăng nhập bị chặn vì sai quá nhiều lần (NFR-09). */
	LOGIN_BLOCKED,
	CREATE_TEACHER,
	LOCK_USER,
	UNLOCK_USER,
	RESET_PASSWORD,
	IMPORT_STUDENTS,
	TCP_IMPORT,
	PUBLISH_ASSIGNMENT,
	RETURN_GRADE,
	REQUEST_REVISION
}
