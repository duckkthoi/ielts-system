-- =====================================================================
-- V4: Bài tập + rubric theo nghiệp vụ chấm IELTS (docs/08-nghiep-vu-cham-ielts.md mục 5.2)
-- =====================================================================

-- ---------- Bài tập ----------
ALTER TABLE assignments
    ADD COLUMN open_notified_at TIMESTAMPTZ,            -- đã gửi thông báo "bài mới" khi open_at ở tương lai (SD-05)
    ADD COLUMN show_suggestions_on_revision BOOLEAN NOT NULL DEFAULT FALSE; -- BR-21: mặc định ẩn gợi ý sửa khi "Cần sửa"

-- Trọng số khi gộp band các phần (BR-12): Task 2 = 2, Task 1 / tự do = 1.
ALTER TABLE assignment_parts
    ADD COLUMN weight NUMERIC(4, 2) NOT NULL DEFAULT 1 CHECK (weight > 0 AND weight <= 10);

-- ---------- Mã tiêu chí IELTS (NULL = tiêu chí tự định nghĩa) ----------
ALTER TABLE rubric_criteria
    ADD COLUMN code VARCHAR(10) CHECK (code IN ('TA', 'TR', 'CC', 'LR', 'GRA'));
ALTER TABLE rubric_template_criteria
    ADD COLUMN code VARCHAR(10) CHECK (code IN ('TA', 'TR', 'CC', 'LR', 'GRA'));

-- ---------- Checklist hai loại (BR-16): CHECK = đặc điểm cần có; LIMIT = đặc điểm giới hạn band ----------
ALTER TABLE checklist_items
    ADD COLUMN kind     VARCHAR(10) NOT NULL DEFAULT 'CHECK' CHECK (kind IN ('CHECK', 'LIMIT')),
    ADD COLUMN max_band NUMERIC(3, 1) CHECK (max_band IS NULL OR (max_band >= 0 AND max_band <= 9)),
    ADD CONSTRAINT chk_checklist_limit CHECK ((kind = 'LIMIT') = (max_band IS NOT NULL));
ALTER TABLE rubric_template_checklist_items
    ADD COLUMN kind     VARCHAR(10) NOT NULL DEFAULT 'CHECK' CHECK (kind IN ('CHECK', 'LIMIT')),
    ADD COLUMN max_band NUMERIC(3, 1) CHECK (max_band IS NULL OR (max_band >= 0 AND max_band <= 9)),
    ADD CONSTRAINT chk_template_checklist_limit CHECK ((kind = 'LIMIT') = (max_band IS NOT NULL));

-- ---------- Cập nhật rubric mẫu của hệ thống ----------
-- IELTS chấm nguyên band cho từng tiêu chí (BR-11) → bước 1.
UPDATE rubric_template_criteria c
SET step = 1,
    code = CASE c.order_no
               WHEN 1 THEN CASE t.task_type WHEN 'WRITING_TASK1' THEN 'TA' ELSE 'TR' END
               WHEN 2 THEN 'CC'
               WHEN 3 THEN 'LR'
               WHEN 4 THEN 'GRA'
           END
FROM rubric_templates t
WHERE t.id = c.template_id AND t.owner_id IS NULL;

-- Mục "Giới hạn điểm" lấy từ các đặc điểm in đậm của bảng mô tả chính thức (docs/08 mục 2).
INSERT INTO rubric_template_checklist_items (criterion_id, content, order_no, kind, max_band)
SELECT c.id, i.content, 100 + i.order_no, 'LIMIT', i.max_band
FROM rubric_template_criteria c
JOIN rubric_templates t ON t.id = c.template_id AND t.owner_id IS NULL
JOIN (VALUES
    -- Task 1 – Task Achievement
    ('WRITING_TASK1', 'TA', 'Không có đoạn tổng quan (overview) — suy ra từ yêu cầu Band 6', 1, 5.0),
    ('WRITING_TASK1', 'TA', 'Không có số liệu minh hoạ cho phần mô tả', 2, 5.0),
    ('WRITING_TASK1', 'TA', 'Định dạng bài không phù hợp (gạch đầu dòng, dạng ghi chú…)', 3, 4.0),
    ('WRITING_TASK1', 'TA', 'Nội dung hoàn toàn không liên quan tới đề', 4, 1.0),
    -- Task 1 – Coherence and Cohesion
    ('WRITING_TASK1', 'CC', 'Toàn bộ bài lạc đề', 1, 2.0),
    -- Task 2 – Task Response
    ('WRITING_TASK2', 'TR', 'Các phần chính của đề chưa được trả lời đầy đủ', 1, 5.0),
    ('WRITING_TASK2', 'TR', 'Định dạng bài không phù hợp (gạch đầu dòng, dạng ghi chú…)', 2, 4.0),
    ('WRITING_TASK2', 'TR', 'Nội dung hoàn toàn không liên quan tới đề', 3, 1.0),
    -- Task 2 – Coherence and Cohesion
    ('WRITING_TASK2', 'CC', 'Không chia đoạn hoặc chia đoạn không đạt', 1, 5.0),
    ('WRITING_TASK2', 'CC', 'Toàn bộ bài lạc đề', 2, 2.0),
    -- Cả hai Task – Lexical Resource
    ('WRITING_TASK1', 'LR', 'Từ vựng không liên quan tới đề', 1, 4.0),
    ('WRITING_TASK2', 'LR', 'Từ vựng không liên quan tới đề', 1, 4.0),
    -- Cả hai Task – Grammatical Range and Accuracy
    ('WRITING_TASK1', 'GRA', 'Hiếm có mệnh đề phụ, câu đơn chiếm ưu thế', 1, 4.0),
    ('WRITING_TASK1', 'GRA', 'Bài quá ngắn, không đủ để đánh giá khả năng dùng câu', 2, 3.0),
    ('WRITING_TASK2', 'GRA', 'Hiếm có mệnh đề phụ, câu đơn chiếm ưu thế', 1, 4.0),
    ('WRITING_TASK2', 'GRA', 'Bài quá ngắn, không đủ để đánh giá khả năng dùng câu', 2, 3.0)
) AS i(task_type, code, content, order_no, max_band)
    ON i.task_type = t.task_type AND i.code = c.code;
