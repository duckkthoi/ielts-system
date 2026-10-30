package com.ltmtu.ielts.exam.dto;

import java.time.Instant;
import java.util.List;

import com.ltmtu.ielts.exam.ExamEnums.ExamEventType;
import com.ltmtu.ielts.exam.ExamEnums.ExamStatus;

/** DTO của chế độ thi — docs/05-api.md A.8, A.9 và B.2. */
public final class ExamDtos {

	private ExamDtos() {
	}

	// ─── Sinh viên ────────────────────────────────────────────────────────────

	/** Phiên thi của tôi; {@code serverTime} để client tính đồng hồ đếm ngược theo giờ server. */
	public record ExamSessionDto(Long sessionId, Long partId, Instant startedAt, Instant endsAt, Instant finishedAt,
			ExamStatus status, Instant serverTime) {
	}

	/**
	 * SEND {@code /app/exam/{sessionId}/event}. {@code pasteChars}: số ký tự đã dán (chỉ PASTE). {@code occurredAt}:
	 * lúc xảy ra theo giờ server mà client ước — để gửi bù sự kiện lúc mất mạng.
	 */
	public record EventMessage(ExamEventType type, Integer pasteChars, Instant occurredAt) {
	}

	// ─── WebSocket giáo viên ─────────────────────────────────────────────────

	/**
	 * {@code /topic/assignments/{id}/exam}: một sự kiện vừa ghi, kèm trạng thái phiên sau sự kiện đó; cũng là một dòng
	 * của bảng tin sự kiện qua REST. {@code reportedLate}: client gửi bù khi có mạng lại.
	 */
	public record ExamPush(Long sessionId, Long partId, Long studentId, String studentName, ExamEventType type,
			Integer pasteChars, ExamStatus status, Instant at, boolean reportedLate) {
	}

	// ─── Giáo viên: màn giám sát ─────────────────────────────────────────────

	public record ExamPartRef(Long id, String title, Integer durationMinutes) {
	}

	/** Một phiên thi trên màn giám sát, kèm số lần rời tab / dán / mất kết nối. */
	public record MonitorRow(Long sessionId, Long partId, String partTitle, Long studentId, String studentCode,
			String studentName, Instant startedAt, Instant endsAt, Instant finishedAt, ExamStatus status, boolean online,
			int tabHiddenCount, int pasteCount, int disconnectCount, Instant lastEventAt) {
	}

	/** {@code parts}: các phần bật chế độ thi của bài; {@code rows}: các phiên đã bắt đầu, mới nhất trước. */
	public record ExamMonitor(Long assignmentId, String title, List<ExamPartRef> parts, List<MonitorRow> rows,
			Instant serverTime) {
	}

	public record EventDto(Long id, ExamEventType type, Integer pasteChars, Instant occurredAt, boolean reportedLate) {
	}

	/** Dòng thời gian một phiên (UC-21). */
	public record ExamTimeline(MonitorRow session, List<EventDto> events) {
	}
}
