-- =====================================================================
-- V23: Nhận xét trực tiếp trên Paper đang viết — loại phản hồi, gợi ý sửa, mục, trạng thái đã xử lý (H4).
-- Vẫn tách khỏi inline_comments của bản nộp; không đổi nội dung tài liệu.
-- =====================================================================

ALTER TABLE paper_live_comments
    ADD COLUMN kind        VARCHAR(20)  NOT NULL DEFAULT 'COMMENT'
        CHECK (kind IN ('COMMENT', 'GRAMMAR', 'VOCABULARY', 'COHERENCE', 'CITATION', 'FORMAT')),
    ADD COLUMN suggestion  TEXT,
    ADD COLUMN section_key VARCHAR(100),
    ADD COLUMN resolved_at TIMESTAMPTZ,
    ADD COLUMN resolved_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_paper_live_comment_resolved CHECK (resolved_by IS NULL OR resolved_at IS NOT NULL);
