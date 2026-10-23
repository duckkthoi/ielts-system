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
}
