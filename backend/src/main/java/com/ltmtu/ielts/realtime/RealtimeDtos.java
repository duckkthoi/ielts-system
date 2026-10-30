package com.ltmtu.ielts.realtime;

import java.math.BigDecimal;
import java.time.Instant;

import com.ltmtu.ielts.submission.SubmissionStatus;

/** Payload của các kênh WebSocket Mức 2 — docs/05-api.md mục B.2. */
public final class RealtimeDtos {

	private RealtimeDtos() {
	}

	/** {@code /user/queue/session}: phiên này đã bị thu hồi, client đăng xuất ngay. */
	public record ForceLogout(String type, String reason) {

		static ForceLogout of(String reason) {
			return new ForceLogout("FORCE_LOGOUT", reason);
		}
	}

	/** {@code /topic/assignments/{id}/presence}: sinh viên online / offline, đang mở phần nào. */
	public record PresenceEvent(Long studentId, boolean online, Long viewingPartId, Instant at) {
	}

	/** {@code /topic/assignments/{id}/progress}: một ô của ma trận tiến độ đổi trạng thái. */
	public record ProgressEvent(Long submissionId, Long studentId, Long partId, SubmissionStatus status, boolean late,
			BigDecimal score, Instant at) {
	}

	/** SEND {@code /app/presence/viewing}: mở / đổi phần ({@code partId}), rời trang ({@code partId = null}). */
	public record ViewingMessage(Long assignmentId, Long partId) {
	}

	// ─── Xem trực tiếp bài đang gõ (B15b, BR-26) ─────────────────────────────

	/** SEND {@code /app/live/{partId}/content}: toàn bộ nội dung ô soạn hiện tại. */
	public record LiveContentMessage(String content) {
	}

	public enum LiveType {
		/** Nội dung mới. */
		CONTENT,
		/** Sinh viên vừa nộp phần này: dừng xem trực tiếp, chuyển sang xem bài nộp. */
		SUBMITTED
	}

	/** {@code /topic/live/parts/{partId}/students/{studentId}}. {@code content} rỗng khi {@code SUBMITTED}. */
	public record LiveEvent(LiveType type, Long partId, Long studentId, String content, int wordCount, Instant at) {
	}

	public enum LiveSource {
		/** Nội dung sinh viên vừa gõ (trong bộ nhớ). */
		LIVE,
		/** Chưa gõ gì từ lúc server chạy: lấy bản nháp đã lưu. */
		DRAFT,
		/** Chưa có gì. */
		EMPTY,
		/** Phần không còn ở trạng thái đang làm (đã nộp, đang chấm…): xem ở màn chấm. */
		NOT_WORKING
	}

	/**
	 * Trạng thái ban đầu của màn xem trực tiếp. {@code submissionId} để mở kênh chat {@code submission-{id}} bình luận
	 * trực tiếp ({@code null} khi sinh viên chưa lưu lần nào).
	 */
	public record LiveSnapshot(Long partId, Long studentId, Long submissionId, SubmissionStatus status, LiveSource source,
			String content, int wordCount, Instant updatedAt, boolean online, Long viewingPartId) {
	}
}
