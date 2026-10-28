-- V10: hiển thị kiểu "Word đã chấm" (track changes + comment) — docs/04 mục 4.5, docs/08 mục 4.3.
-- Chữ gốc của sinh viên không đổi; sửa chỉ là lớp hiển thị lưu trong inline_comments.

-- edit: REPLACE = gạch chữ cũ, hiện chữ mới (suggestion) ngay cạnh · DELETE = gạch chữ cũ ·
--       INSERT = chèn suggestion ngay sau đoạn được chọn (đoạn đó giữ nguyên). NULL = không sửa
--       (bản ghi cũ có suggestion mà không có edit được hiểu là REPLACE).
-- kind thêm COMMENT: chỉ nhận xét (nền xám), không phải lỗi, không tính vào thống kê.
ALTER TABLE inline_comments
    ADD COLUMN edit VARCHAR(10) CHECK (edit IN ('REPLACE', 'DELETE', 'INSERT')),
    DROP CONSTRAINT inline_comments_kind_check,
    ADD CONSTRAINT inline_comments_kind_check CHECK (kind IN ('ERROR', 'HIGHLIGHT', 'GOOD', 'COMMENT')),
    DROP CONSTRAINT chk_inline_has_content,
    ADD CONSTRAINT chk_inline_has_content CHECK (
        (kind <> 'ERROR' OR num_nonnulls(error_code, comment, suggestion, edit) >= 1)
        AND (kind <> 'COMMENT' OR comment IS NOT NULL)
        AND (edit IS NULL OR edit = 'DELETE' OR suggestion IS NOT NULL));

UPDATE inline_comments SET edit = 'REPLACE' WHERE suggestion IS NOT NULL;
