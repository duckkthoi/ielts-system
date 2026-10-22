-- =====================================================================
-- V2: Mẫu tiêu chí chấm IELTS Writing của hệ thống (owner_id = NULL)
-- Giáo viên nạp mẫu vào từng phần của bài tập rồi chỉnh sửa tuỳ ý.
-- =====================================================================

INSERT INTO rubric_templates (name, task_type, description) VALUES
    ('IELTS Writing Task 1 (Academic)', 'WRITING_TASK1',
     'Bốn tiêu chí chấm chính thức của IELTS Writing Task 1, thang band 0–9, bước 0.5.'),
    ('IELTS Writing Task 2', 'WRITING_TASK2',
     'Bốn tiêu chí chấm chính thức của IELTS Writing Task 2, thang band 0–9, bước 0.5.');

-- ---------- Tiêu chí ----------
INSERT INTO rubric_template_criteria (template_id, name, description, order_no)
SELECT t.id, c.name, c.description, c.order_no
FROM rubric_templates t
JOIN (VALUES
    ('WRITING_TASK1', 'Task Achievement', 'Mức độ hoàn thành yêu cầu đề: tổng quan, đặc điểm chính, số liệu.', 1),
    ('WRITING_TASK1', 'Coherence and Cohesion', 'Bố cục, sự mạch lạc và liên kết giữa các ý.', 2),
    ('WRITING_TASK1', 'Lexical Resource', 'Độ đa dạng và chính xác của từ vựng.', 3),
    ('WRITING_TASK1', 'Grammatical Range and Accuracy', 'Độ đa dạng và chính xác của ngữ pháp.', 4),
    ('WRITING_TASK2', 'Task Response', 'Trả lời đầy đủ đề, quan điểm rõ ràng, ý được phát triển.', 1),
    ('WRITING_TASK2', 'Coherence and Cohesion', 'Bố cục, sự mạch lạc và liên kết giữa các ý.', 2),
    ('WRITING_TASK2', 'Lexical Resource', 'Độ đa dạng và chính xác của từ vựng.', 3),
    ('WRITING_TASK2', 'Grammatical Range and Accuracy', 'Độ đa dạng và chính xác của ngữ pháp.', 4)
) AS c(task_type, name, description, order_no) ON c.task_type = t.task_type
WHERE t.owner_id IS NULL;

-- ---------- Checklist cho từng tiêu chí ----------
INSERT INTO rubric_template_checklist_items (criterion_id, content, order_no)
SELECT rc.id, i.content, i.order_no
FROM rubric_template_criteria rc
JOIN rubric_templates t ON t.id = rc.template_id AND t.owner_id IS NULL
JOIN (VALUES
    -- Task 1: Task Achievement
    ('WRITING_TASK1', 1, 'Câu mở bài diễn đạt lại (paraphrase) đề bài', 1),
    ('WRITING_TASK1', 1, 'Có đoạn tổng quan (overview) nêu xu hướng / đặc điểm nổi bật', 2),
    ('WRITING_TASK1', 1, 'Chọn lọc và báo cáo các đặc điểm chính, có so sánh phù hợp', 3),
    ('WRITING_TASK1', 1, 'Số liệu dẫn chứng chính xác và đầy đủ', 4),
    ('WRITING_TASK1', 1, 'Không đưa ý kiến cá nhân hoặc thông tin ngoài biểu đồ', 5),
    ('WRITING_TASK1', 1, 'Đủ tối thiểu 150 từ', 6),
    -- Task 1: Coherence and Cohesion
    ('WRITING_TASK1', 2, 'Chia đoạn rõ ràng, nhóm thông tin hợp lý', 1),
    ('WRITING_TASK1', 2, 'Từ nối đa dạng, dùng chính xác, không lạm dụng', 2),
    ('WRITING_TASK1', 2, 'Dùng đại từ và phép thay thế để tránh lặp', 3),
    ('WRITING_TASK1', 2, 'Thông tin được sắp xếp theo trình tự logic', 4),
    -- Task 1: Lexical Resource
    ('WRITING_TASK1', 3, 'Từ vựng mô tả xu hướng và số liệu đa dạng', 1),
    ('WRITING_TASK1', 3, 'Có paraphrase, tránh lặp từ của đề', 2),
    ('WRITING_TASK1', 3, 'Collocation tự nhiên, chính xác', 3),
    ('WRITING_TASK1', 3, 'Ít lỗi chính tả và cấu tạo từ', 4),
    -- Task 1: Grammatical Range and Accuracy
    ('WRITING_TASK1', 4, 'Kết hợp câu đơn và câu phức', 1),
    ('WRITING_TASK1', 4, 'Thì động từ phù hợp với mốc thời gian của biểu đồ', 2),
    ('WRITING_TASK1', 4, 'Cấu trúc so sánh dùng chính xác', 3),
    ('WRITING_TASK1', 4, 'Phần lớn câu không có lỗi, dấu câu đúng', 4),
    -- Task 2: Task Response
    ('WRITING_TASK2', 1, 'Trả lời đầy đủ tất cả các phần của đề', 1),
    ('WRITING_TASK2', 1, 'Quan điểm rõ ràng và nhất quán xuyên suốt bài', 2),
    ('WRITING_TASK2', 1, 'Ý chính được phát triển, giải thích và có ví dụ cụ thể', 3),
    ('WRITING_TASK2', 1, 'Kết luận tóm tắt quan điểm, không thêm ý mới', 4),
    ('WRITING_TASK2', 1, 'Đủ tối thiểu 250 từ', 5),
    -- Task 2: Coherence and Cohesion
    ('WRITING_TASK2', 2, 'Bố cục mở bài – thân bài – kết bài rõ ràng', 1),
    ('WRITING_TASK2', 2, 'Mỗi đoạn có câu chủ đề (topic sentence) và một ý trung tâm', 2),
    ('WRITING_TASK2', 2, 'Từ nối và phương tiện liên kết linh hoạt, chính xác', 3),
    ('WRITING_TASK2', 2, 'Lập luận tiến triển logic giữa các đoạn', 4),
    -- Task 2: Lexical Resource
    ('WRITING_TASK2', 3, 'Từ vựng phù hợp chủ đề, có từ ít phổ biến', 1),
    ('WRITING_TASK2', 3, 'Paraphrase tốt, tránh lặp từ', 2),
    ('WRITING_TASK2', 3, 'Collocation và thành ngữ dùng tự nhiên', 3),
    ('WRITING_TASK2', 3, 'Ít lỗi chính tả và cấu tạo từ', 4),
    -- Task 2: Grammatical Range and Accuracy
    ('WRITING_TASK2', 4, 'Đa dạng cấu trúc (mệnh đề quan hệ, câu điều kiện, bị động…)', 1),
    ('WRITING_TASK2', 4, 'Kiểm soát tốt thì và sự hoà hợp chủ ngữ – động từ', 2),
    ('WRITING_TASK2', 4, 'Phần lớn câu không có lỗi', 3),
    ('WRITING_TASK2', 4, 'Dấu câu dùng chính xác', 4)
) AS i(task_type, criterion_order, content, order_no)
    ON i.task_type = t.task_type AND i.criterion_order = rc.order_no;
