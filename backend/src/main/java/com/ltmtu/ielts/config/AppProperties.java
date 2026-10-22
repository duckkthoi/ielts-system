package com.ltmtu.ielts.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param zone múi giờ nghiệp vụ, dùng khi cần "ngày" theo giờ Việt Nam (thống kê theo ngày, hiển thị trong email).
 *             Dữ liệu thời gian vẫn luôn lưu UTC.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
		Jwt jwt,
		Admin admin,
		Cors cors,
		Mail mail,
		String uploadDir,
		Ai ai,
		ZoneId zone,
		Grading grading,
		Reminders reminders) {

	/**
	 * @param refreshReuseGrace refresh token cũ dùng lại trong khoảng này sau khi xoay vòng (vd 2 tab refresh cùng
	 *                          lúc) thì chỉ từ chối, không coi là bị đánh cắp.
	 */
	public record Jwt(String secret, Duration accessTtl, Duration refreshTtl, Duration refreshReuseGrace) {
	}

	public record Admin(String username, String password, String fullName, String email) {
	}

	public record Cors(List<String> allowedOrigins) {
	}

	/**
	 * @param loginUrl địa chỉ trang đăng nhập, ghi trong email gửi tài khoản.
	 * @param appUrl   địa chỉ gốc của frontend, ghép với {@code link} của thông báo để tạo đường dẫn trong email.
	 */
	public record Mail(String from, String loginUrl, String appUrl) {
	}

	/**
	 * Job nhắc hạn và thông báo bài mở (UC-30, SD-10).
	 *
	 * @param enabled  bật bộ lập lịch (test tắt để tự gọi job)
	 * @param interval chu kỳ chạy job
	 * @param soon     mốc nhắc thứ nhất (H24): còn ≤ khoảng này tới hạn
	 * @param urgent   mốc nhắc thứ hai (H2)
	 */
	public record Reminders(boolean enabled, Duration interval, Duration soon, Duration urgent) {
	}

	/** Giới hạn số mã lỗi riêng và số nhận xét trong ngân hàng của mỗi giáo viên (BR-22). */
	public record Grading(int maxErrorCodesPerTeacher, int maxCommentBankItemsPerTeacher) {
	}

	public record Ai(String anthropicApiKey) {

		public boolean enabled() {
			return anthropicApiKey != null && !anthropicApiKey.isBlank();
		}
	}
}
