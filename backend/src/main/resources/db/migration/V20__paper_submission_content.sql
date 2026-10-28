-- Mức 4 / bước 25a: lần nộp của bài PAPER giữ cả tài liệu có định dạng và thư viện trích dẫn lúc nộp — docs/12 mục 5.4.
-- Bản nộp vẫn bất biến (NFR-11): `content` = chữ thuần (đếm từ, tìm kiếm, đạo văn), `content_json` = JSON trình soạn
-- (hiển thị, nhận xét theo khối, xuất file), `references_json` = các tài liệu được trích trong bài [{id, csl}] để danh
-- mục không đổi khi nhóm sửa thư viện sau đó. Bài luận để NULL cả hai.

ALTER TABLE submission_versions
    ADD COLUMN content_json    JSONB,
    ADD COLUMN references_json JSONB,
    ADD CONSTRAINT chk_submission_version_paper_json
        CHECK ((content_json IS NULL AND references_json IS NULL)
            OR (jsonb_typeof(content_json) = 'object' AND jsonb_typeof(references_json) = 'array'));
