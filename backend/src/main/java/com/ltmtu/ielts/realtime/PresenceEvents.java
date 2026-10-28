package com.ltmtu.ielts.realtime;

/** Sự kiện của module realtime cho module khác nghe (vd chế độ thi ghi mất kết nối / kết nối lại). */
public final class PresenceEvents {

	private PresenceEvents() {
	}

	/** Sinh viên có kết nối đầu tiên ({@code online}) hoặc vừa mất kết nối cuối cùng. Phát ngoài transaction. */
	public record OnlineChanged(Long studentId, boolean online) {
	}
}
