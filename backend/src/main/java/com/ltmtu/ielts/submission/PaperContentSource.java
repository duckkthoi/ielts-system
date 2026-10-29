package com.ltmtu.ielts.submission;

/**
 * Nội dung nộp của bài PAPER (B25a, BR-28, BR-29): lấy từ tài liệu Yjs của nhóm do module {@code paper} giữ. Module bài
 * nộp đứng trước module paper trong chiều phụ thuộc nên chỉ khai báo interface ở đây; {@code paper} cài đặt (cùng cách
 * với {@code StompAccessRule}). Được gọi <b>trong transaction nộp bài</b>, sau khi đã khoá dòng bài làm của nhóm.
 */
public interface PaperContentSource {

	/**
	 * Bản nộp của nhóm cho giai đoạn này, lấy từ snapshot mới nhất của tài liệu. Snapshot cũ hơn thay đổi cuối →
	 * {@code 409 SNAPSHOT_STALE} (client lưu snapshot rồi nộp lại); mục bắt buộc trống → {@code 422}; tài liệu trống →
	 * {@code 400}.
	 */
	PaperContent capture(Long partId, Long groupId);

	/**
	 * @param text           chữ thuần (tiêu đề mục + nội dung) — lưu vào {@code content}
	 * @param wordCount      số từ phần thân các mục (không tính tiêu đề mục, công thức)
	 * @param contentJson    JSON trình soạn
	 * @param referencesJson các tài liệu được trích trong bài, dạng {@code [{id, csl}]}
	 */
	record PaperContent(String text, int wordCount, String contentJson, String referencesJson) {
	}
}
