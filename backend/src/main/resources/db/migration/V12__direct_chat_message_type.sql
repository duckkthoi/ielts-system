-- V11 đã thêm kênh DIRECT và khoá ngoại, nhưng CHECK sinh tự động ở V7 vẫn chỉ cho hai loại cũ.
-- V11 đã chạy trên các CSDL hiện có nên sửa bằng migration tiếp theo, không sửa lịch sử V11.
ALTER TABLE chat_messages DROP CONSTRAINT chat_messages_channel_type_check;
ALTER TABLE chat_messages ADD CONSTRAINT chat_messages_channel_type_check
    CHECK (channel_type IN ('SUBMISSION', 'CLASSROOM', 'DIRECT'));
