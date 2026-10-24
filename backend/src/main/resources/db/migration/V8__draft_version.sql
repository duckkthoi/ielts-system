-- =====================================================================
-- V8: Khoá lạc quan cho bản nháp (SD-06, bước 10) — docs/04-erd.md bảng submissions.
-- =====================================================================

-- Tăng 1 sau mỗi lần lưu nháp thành công. Client gửi số mình đang có; lệch nghĩa là tab / máy khác vừa lưu
-- → server trả 409 DRAFT_CONFLICT thay vì ghi đè.
ALTER TABLE submissions
    ADD COLUMN draft_version INT NOT NULL DEFAULT 0 CHECK (draft_version >= 0);
