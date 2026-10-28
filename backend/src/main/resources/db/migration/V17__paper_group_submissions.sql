-- Mức 4 / bước 21b: bài làm của bài PAPER thuộc NHÓM (BR-28) — docs/12.
-- Bài luận (ESSAY) giữ nguyên: một bài làm cho một (phần, sinh viên). Bài PAPER: một bài làm cho một (phần, nhóm),
-- student_id = người tạo / người nộp gần nhất.

ALTER TABLE submissions ADD COLUMN group_id BIGINT REFERENCES paper_groups (id);
CREATE INDEX idx_submissions_group ON submissions (group_id) WHERE group_id IS NOT NULL;

ALTER TABLE submissions DROP CONSTRAINT submissions_part_id_student_id_key;
CREATE UNIQUE INDEX uk_submission_student ON submissions (part_id, student_id) WHERE group_id IS NULL;
CREATE UNIQUE INDEX uk_submission_group ON submissions (part_id, group_id) WHERE group_id IS NOT NULL;

-- Bài làm thuộc về ai: bài luận → sinh viên của dòng; bài PAPER → mọi thành viên HIỆN TẠI của nhóm.
-- Các truy vấn quyền / tiến độ / nhắc hạn / chat join qua view này thay vì so submissions.student_id.
CREATE VIEW submission_members AS
SELECT s.id AS submission_id, s.student_id
FROM submissions s
WHERE s.group_id IS NULL
UNION ALL
SELECT s.id, m.student_id
FROM submissions s
JOIN paper_group_members m ON m.group_id = s.group_id;

-- Job báo giáo viên khi tới hạn chốt nhóm mà còn người chưa có nhóm (BR-27) — chống báo trùng.
ALTER TABLE assignments ADD COLUMN group_lock_notified_at TIMESTAMPTZ;
