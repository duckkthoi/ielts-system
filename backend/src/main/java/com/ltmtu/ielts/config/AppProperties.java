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
		Reminders reminders,
		Tcp tcp,
		RateLimit rateLimit,
		Session session,
		Demo demo) {

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

	/**
	 * Server TCP import sinh viên (docs/05 phần C).
	 *
	 * @param port           cổng lắng nghe; 0 = cổng ngẫu nhiên (test)
	 * @param threads        số phiên xử lý đồng thời; đầy thì kết nối mới nhận {@code ERR 503}
	 * @param readTimeout    không nhận được byte nào trong khoảng này → {@code ERR 408}
	 * @param sessionTimeout thời gian tối đa của một phiên
	 * @param maxPayload     số byte CSV tối đa của một lệnh IMPORT
	 */
	public record Tcp(boolean enabled, int port, int threads, Duration readTimeout, Duration sessionTimeout,
			int maxPayload) {
	}

	/**
	 * Giới hạn tần suất trong bộ nhớ (bước 12).
	 *
	 * @param loginMaxFailures số lần đăng nhập sai tối đa trong {@code loginWindow}, tính riêng cho mỗi IP và mỗi tài
	 *                         khoản (NFR-09); vượt → 429
	 * @param remindMax        số lần nhắc thủ công tối đa trong {@code remindWindow} cho mỗi (giáo viên, bài)
	 */
	public record RateLimit(int loginMaxFailures, Duration loginWindow, int remindMax, Duration remindWindow) {
	}

	/** @param singleLogin một tài khoản một phiên (FR-06): đăng nhập nơi mới thì các phiên cũ bị thu hồi. */
	public record Session(boolean singleLogin) {
	}

	/**
	 * Dữ liệu mẫu, chỉ dùng khi bật profile {@code demo} (bước 20, kiểm thử tải bước 14).
	 *
	 * @param prefix       tiền tố tên đăng nhập / mã lớp / mã SV, để không trùng dữ liệu thật: {@code DEMO} → giáo viên
	 *                     {@code gv.demo}, lớp {@code DEMO-A}, sinh viên {@code DEMO001}
	 * @param password     mật khẩu chung của mọi tài khoản mẫu (không bắt đổi lần đầu)
	 * @param loadStudents số sinh viên của lớp kiểm thử tải {@code <prefix>-LOAD} ({@code <prefix>-T0001}…); 0 = không tạo
	 */
	public record Demo(String prefix, String password, int studentsPerClass, int loadStudents) {
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
