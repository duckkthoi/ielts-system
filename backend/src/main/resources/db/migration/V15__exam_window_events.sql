-- Mức 3 / bước 15e: giám sát thi chặt hơn (BR-25).
-- 1) Rời cửa sổ làm bài mà tab vẫn hiện (Alt+Tab sang ứng dụng khác, màn hình thứ 2, chia đôi màn hình).
-- 2) Sự kiện xảy ra lúc mất mạng được client giữ lại và gửi bù khi có mạng lại.

ALTER TABLE exam_events DROP CONSTRAINT exam_events_type_check;
ALTER TABLE exam_events ADD CONSTRAINT exam_events_type_check
    CHECK (type IN ('STARTED', 'TAB_HIDDEN', 'TAB_VISIBLE', 'WINDOW_BLUR', 'WINDOW_FOCUS', 'PASTE',
                    'DISCONNECT', 'RECONNECT', 'OVERTIME', 'SUBMITTED'));

-- true = client gửi bù sau khi có mạng lại; occurred_at khi đó là giờ client báo (đã quy về giờ server, bị kẹp
-- trong khoảng phiên thi), không phải giờ server nhận.
ALTER TABLE exam_events ADD COLUMN reported_late BOOLEAN NOT NULL DEFAULT false;

-- Bảng tin sự kiện của màn giám sát: sự kiện mới nhất của các phiên trong một bài.
CREATE INDEX idx_exam_events_recent ON exam_events (occurred_at DESC);
