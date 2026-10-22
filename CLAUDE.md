# Hướng dẫn cho AI agent — dự án IELTS Writing (LTMTU)

> Đọc file này **trước tiên**, rồi đọc quy tắc của phần mình sửa:
> - sửa `backend/` → [`backend/AI-CODING-RULES.md`](backend/AI-CODING-RULES.md)
> - sửa `frontend/` → [`frontend/AI-CODING-RULES.md`](frontend/AI-CODING-RULES.md)
>
> Kế hoạch: [`PLAN.md`](PLAN.md) · Tiến độ: [`PROGRESS.md`](PROGRESS.md) · Tài liệu chuẩn: [`docs/`](docs/)

---

## 1. Quy trình bắt buộc cho mỗi bước

1. **Đọc trước khi code:** mục "Bước tiếp theo" cuối `PROGRESS.md`, phần liên quan trong `docs/`, và code hiện có của module đó (để viết giống code xung quanh).
2. **Làm đúng một bước nhỏ.** Bước lớn thì tách (vd B8a, B8b) và báo trước cách tách.
3. **Kiểm tra:** chạy test / lint / build của phần đã sửa (lệnh ở mục 4). Không báo "xong" khi chưa chạy.
4. **Cập nhật tài liệu trong cùng bước:** API → `docs/05`; bảng → `docs/04` + migration mới; trạng thái → `docs/06`; luồng → `docs/02`, `docs/03`. Sửa `docs/` thì xuất lại Word (lệnh ở mục 4).
5. **Ghi `PROGRESS.md`:** thêm một mục nhật ký (đã làm gì, ở file nào, **khác gì so với tài liệu**, kiểm tra thế nào, lỗi gặp phải) và cập nhật bảng trạng thái + "Tồn đọng / Bước tiếp theo".
6. **Báo cáo ngắn bằng tiếng Việt rồi DỪNG**, chờ người dùng xác nhận ("ok", "tiếp") mới làm bước sau.

Không tự ý: commit / push (chỉ khi người dùng bảo), thêm thư viện mới (phải ghi lý do vào PROGRESS), đổi kiến trúc đã chốt (monolith, không Redis).

---

## 2. Ràng buộc KHÔNG ĐƯỢC vi phạm

| # | Ràng buộc |
|---|---|
| 1 | Mật khẩu superuser PostgreSQL **không ghi vào bất kỳ file nào** và không nhắc lại. Cần quyền superuser thì hỏi người dùng. |
| 2 | **Không commit `cbs-fe.zip` / `cbs-fe/`** (dự án của đơn vị khác, chỉ để tham khảo cấu trúc). Không chép giao diện của nó; không dùng thư viện `@super/ui`, `@super/sdk`. |
| 3 | **Không chép nguyên văn** bảng mô tả band IELTS. Chỉ dùng bản diễn giải tiếng Việt + link bản chính thức ielts.org. |
| 4 | **Không log** mật khẩu, token, nội dung file import, nội dung bài làm. Mật khẩu tạm khi import chỉ giữ trong bộ nhớ, không ghi DB. |
| 5 | Không commit `.env`, `uploads/`, `node_modules/`, `target/`, `.next/`, `tools/mailpit/`. |
| 6 | Mọi chuỗi hiển thị cho người dùng (message lỗi, thông báo, giao diện) bằng **tiếng Việt**. |

---

## 3. Bản đồ thư mục

```
LTMTU/
├─ CLAUDE.md, AGENTS.md     ← file này
├─ PLAN.md                  kế hoạch, lộ trình, kịch bản demo
├─ PROGRESS.md              nhật ký từng bước + tồn đọng + bước tiếp theo
├─ docs/                    TÀI LIỆU CHUẨN (Markdown + Mermaid)
│   ├─ 01-tong-quan.md      FR, NFR, quy tắc nghiệp vụ BR-01 → BR-22
│   ├─ 02-use-case.md       32 use case    · 03-sequence.md  sơ đồ tuần tự SD-01 → SD-13
│   ├─ 04-erd.md            bảng, cột, lộ trình migration V1 → V7
│   ├─ 05-api.md            REST, WebSocket, giao thức TCP, mã lỗi
│   ├─ 06-trang-thai.md     sơ đồ trạng thái · 07-design-system.md · 08-nghiep-vu-cham-ielts.md
├─ docx/                    bản Word sinh từ docs/ (không sửa tay)
├─ design/                  prompt cho Figma Make
├─ shared/                  dữ liệu dùng chung FE + BE (vd bộ ca đếm từ)
├─ backend/                 Spring Boot 4 (xem backend/AI-CODING-RULES.md)
├─ frontend/                Next.js 16 (xem frontend/AI-CODING-RULES.md)
└─ tools/docs-export/       script xuất Word
```

Thứ tự ưu tiên khi các nguồn lệch nhau: **`docs/` > code hiện có > `PLAN.md`**. Thấy lệch thì sửa cho khớp và ghi vào PROGRESS.

---

## 4. Lệnh thường dùng (Windows)

| Việc | Lệnh |
|---|---|
| Chạy backend (profile dev) | `cd backend` → `mvn spring-boot:run` |
| Test backend | `cd backend` → `mvn test` (DB `ielts_test`). Gặp `NoClassDefFoundError` lạ → `mvn clean test` |
| Chạy frontend | `cd frontend` → `npm run dev` (http://localhost:3000) |
| Kiểm tra frontend | `npm run lint` · `npm run build` · `npm run test:unit` |
| Email khi dev | `tools\mailpit\mailpit.exe` → xem thư ở http://localhost:8025 |
| Xuất Word | `python tools/docs-export/export_docx.py <tên-file-không-đuôi>` rồi `… --merge`. **Chạy bằng PowerShell** (Git Bash không in được ký tự `✔`) |

CSDL dev: PostgreSQL 18 trên `localhost:5432`, user / mật khẩu `ielts` / `ielts`, DB `ielts` (dev) và `ielts_test` (test).

---

## 5. Thế nào là "xong một bước"

```
[ ] Đúng phạm vi bước đã báo; không làm lan sang bước khác
[ ] Test / lint / build của phần đã sửa đều chạy qua (ghi số lượt test vào PROGRESS)
[ ] Mỗi quy tắc BR-xx mới cài có ít nhất một test, ghi mã trong @DisplayName / tên test
[ ] docs/ đã cập nhật nếu đổi API, bảng, trạng thái, luồng; đã xuất lại Word
[ ] PROGRESS.md có mục mới + bảng trạng thái + "Bước tiếp theo"
[ ] Không vi phạm mục 2
[ ] Đã báo cáo và dừng chờ xác nhận
```
