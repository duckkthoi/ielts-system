package com.ltmtu.ielts.paper.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO của tài liệu paper cùng sửa — docs/05-api.md mục A.10, B.2. Nhị phân Yjs đi dạng <b>base64</b> (JSON không chở
 * byte được).
 */
public final class PaperDocumentDtos {

	private PaperDocumentDtos() {
	}

	/** Một update Yjs đã được cấp số thứ tự. */
	public record UpdateDto(long seq, String update, Long authorId, Instant createdAt) {
	}

	/**
	 * Trạng thái để dựng tài liệu: áp {@code snapshot} rồi lần lượt {@code updates} (seq &gt; snapshotSeq).
	 * {@code editable = false}: nhóm đã nộp (chờ chấm / đã trả) hoặc bài đã đóng — chỉ đọc.
	 */
	public record DocumentState(Long documentId, Long partId, Long groupId, String snapshot, long snapshotSeq,
			long lastSeq, List<UpdateDto> updates, boolean editable) {
	}

	/**
	 * Lấy bù sau khi kết nối lại. {@code resync = true}: các update cần lấy đã bị gộp vào snapshot — client tải lại toàn
	 * bộ trạng thái.
	 */
	public record UpdatesAfter(List<UpdateDto> updates, long lastSeq, boolean resync) {
	}

	/** PUT snapshot: trạng thái Yjs đã gộp tới {@code upToSeq}, kèm JSON trình soạn và chữ thuần. */
	public record SnapshotRequest(
			@NotBlank(message = "Thiếu nội dung snapshot") String state,
			@NotNull @Min(value = 0, message = "upToSeq không hợp lệ") Long upToSeq,
			String contentJson,
			String contentText) {
	}

	/** SEND {@code /app/paper/{docId}/update}. {@code clientId}: client tự đặt để nhận ra tin của mình khi server phát lại. */
	public record UpdateMessage(String update, String clientId, Integer charsAdded, Integer charsRemoved) {
	}

	/** {@code /topic/paper/{docId}}: update vừa được lưu. */
	public record UpdatePush(Long documentId, long seq, String update, Long authorId, String clientId) {
	}

	/** SEND {@code /app/paper/{docId}/awareness}: con trỏ / vùng chọn / tên (Yjs awareness, không lưu). */
	public record AwarenessMessage(String state) {
	}

	/** {@code /topic/paper/{docId}/awareness}. */
	public record AwarenessPush(Long userId, String userName, String state) {
	}

	/** Lỗi của lệnh STOMP gửi riêng người gửi trên {@code /user/queue/errors} (cùng dạng với chat). */
	public record SocketError(String errorCode, String errorMessage) {
	}
}
