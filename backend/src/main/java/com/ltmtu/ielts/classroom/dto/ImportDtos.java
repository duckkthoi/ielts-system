package com.ltmtu.ielts.classroom.dto;

import java.time.Instant;
import java.util.List;

/** Kết quả import sinh viên — docs/05-api.md mục A.4. */
public final class ImportDtos {

	private ImportDtos() {
	}

	public record RowError(int row, String studentCode, String reason) {
	}

	/**
	 * @param created        số tài khoản mới tạo
	 * @param existing       đã có tài khoản, được thêm vào lớp
	 * @param skipped        đã là thành viên lớp
	 * @param credentialsToken dùng để tải file tài khoản một lần; {@code null} nếu không có tài khoản mới
	 */
	public record ImportReport(int totalRows, int created, int existing, int skipped, List<RowError> errors,
			boolean emailQueued, String credentialsToken, Instant credentialsExpiresAt) {
	}
}
