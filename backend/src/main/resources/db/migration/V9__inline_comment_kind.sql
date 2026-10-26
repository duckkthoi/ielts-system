-- V9: đánh dấu trên bài làm (F5b) — docs/04 mục 4, docs/05 mục A.7.
-- Không tạo bảng mới: nhận xét inline (inline_comments) và nhận xét mẫu (comment_bank) đã có từ V1 / V5.

-- Kiểu đánh dấu: ERROR = lỗi (tô đỏ, tính vào thống kê lỗi) · HIGHLIGHT = lưu ý (tô vàng) · GOOD = dùng hay (tô xanh).
-- resolution: giáo viên đánh giá lại lỗi của lần nộp CŨ khi so sánh với lần nộp sau
-- (NULL = để hệ thống tự đoán · FIXED = đã sửa · NOT_FIXED = chưa sửa · CHANGED = đã đổi nhưng khác gợi ý).
ALTER TABLE inline_comments
    ADD COLUMN kind       VARCHAR(10) NOT NULL DEFAULT 'ERROR' CHECK (kind IN ('ERROR', 'HIGHLIGHT', 'GOOD')),
    ADD COLUMN resolution VARCHAR(10) CHECK (resolution IN ('FIXED', 'NOT_FIXED', 'CHANGED'));

-- Nhận xét cũ dùng mã khen (GOOD) chuyển sang kiểu GOOD.
UPDATE inline_comments ic SET kind = 'GOOD'
WHERE EXISTS (SELECT 1 FROM error_codes e WHERE e.owner_id IS NULL AND e.positive AND upper(e.code) = upper(ic.error_code));

-- Lỗi phải có mã lỗi / nhận xét / gợi ý sửa; đánh dấu vàng / xanh được để trống (chỉ tô màu).
ALTER TABLE inline_comments
    DROP CONSTRAINT chk_inline_has_content,
    ADD CONSTRAINT chk_inline_has_content CHECK (kind <> 'ERROR' OR num_nonnulls(error_code, comment, suggestion) >= 1);

-- Mã nhóm cho 7 nút chọn nhanh (Ngữ pháp, Chính tả, Từ vựng, Collocation, Liên kết, Dấu câu, Khác):
-- 5 nhóm dùng lại mã có sẵn (SP, WW, COL, LINK, P), thêm 2 mã chung.
INSERT INTO error_codes (owner_id, code, name, criterion, positive, order_no) VALUES
    (NULL, 'GR',    'Ngữ pháp (chung)', 'GRA', FALSE, 23),
    (NULL, 'OTHER', 'Khác',             NULL,  FALSE, 24);
