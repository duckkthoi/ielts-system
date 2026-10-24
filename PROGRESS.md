# Tiến độ dự án — Website giao bài & chấm bài IELTS Writing

> Chỉ ghi **đã làm tới đâu · làm được gì · còn lại gì**. Chi tiết từng bước xem lịch sử git.
> Kế hoạch: [PLAN.md](PLAN.md) · Tài liệu chuẩn: [docs/](docs/) · Hướng dẫn sử dụng + tài khoản: [docs/10](docs/10-huong-dan-su-dung.md)

Cập nhật: 24/10/2026

---

## 1. Đang ở đâu

**Backend xong Mức 1 + Mức 2. Frontend xong F1 → F3, bước tiếp theo là F4 (màn sinh viên).**

| Phần | Trạng thái |
|---|---|
| Tài liệu phân tích thiết kế (docs 01 → 10 + Word gộp), design system, prompt Figma, quy tắc cho AI | ✅ |
| Backend Mức 1 — lõi nghiệp vụ (bước 1 → 8) | ✅ |
| Backend Mức 2 — trọng tâm mạng (bước 9 → 14) | ✅ (phần frontend của bước 10 làm ở F4) |
| Bước 20 — README, dữ liệu demo, tài liệu báo cáo | ✅ (ảnh Wireshark người dùng tự chụp) |
| Frontend F1 — đăng nhập, khung app, thông báo realtime | ✅ |
| Frontend F2 — quản trị; lớp học + import sinh viên | ✅ |
| Frontend F3 — bài tập: danh sách, chi tiết, giao bài, trình soạn | ✅ |
| Frontend F4 — sinh viên: bài được giao, làm bài, nộp bài | ⬜ **tiếp theo** |
| Frontend F5 — giáo viên chấm bài | ⬜ |
| Frontend F6 — kết quả, tiến độ, thống kê, trao đổi | ⬜ |
| Mức 3 (bước 15 → 19) | ⬜ làm nếu còn thời gian |

Kiểm tra gần nhất: backend `mvn test` 466 test case đều đạt · frontend `lint`, `tsc`, `build` sạch, `test:unit` 27 / 27.
Commit gần nhất: F3b + docs/10 + thư mục thiết kế `design/figma-make/` (chưa push).

---

## 1b. Tiến độ theo màn hình

Đánh dấu `[x]` khi màn đã có giao diện thật, gọi API thật và đã chạy thử với backend. Màn `[ ]` hiện vẫn là khung trống (`PagePlaceholder`). Cột trong ngoặc là bước sẽ làm.

**Dùng chung (mọi vai trò)** — 4 / 5
- [x] Đăng nhập — `/dang-nhap` (F1)
- [x] Đổi mật khẩu (bắt buộc lần đầu + chủ động) — `/doi-mat-khau` (F1)
- [x] Khung app: sidebar, header, chuông thông báo, trạng thái kết nối, banner mất kết nối, hộp thoại bị đăng xuất (F1)
- [x] Thông báo — `/thong-bao` (F1)
- [ ] Thông tin cá nhân — `/ca-nhan` (F6)

**Quản trị viên** — 5 / 5
- [x] Tổng quan hệ thống + biểu đồ bài nộp 14 ngày — `/quan-tri/tong-quan` (F2)
- [x] Giáo viên: danh sách, thêm, sửa, khoá, đặt lại mật khẩu — `/quan-tri/giao-vien` (F2)
- [x] Người dùng: tra cứu, chi tiết, khoá, đặt lại mật khẩu — `/quan-tri/nguoi-dung` (F2)
- [x] Nhật ký hệ thống — `/quan-tri/nhat-ky` (F2)
- [x] Phiên đăng nhập — `/quan-tri/phien-dang-nhap` (F2; chỉ hiện số phiên, danh sách chi tiết thuộc Mức 3)

**Giáo viên** — 5 / 13
- [ ] Tổng quan (dashboard) — `/giao-vien/tong-quan` (F6)
- [x] Danh sách lớp — `/giao-vien/lop-hoc` (F2)
- [x] Chi tiết lớp: thành viên, thêm sinh viên, import + file tài khoản — `/giao-vien/lop-hoc/[id]` (F2)
- [x] Danh sách bài tập — `/giao-vien/bai-tap` (F3)
- [x] Trình soạn bài (tạo + sửa nháp) — `/giao-vien/bai-tap/tao-moi` (F3)
- [x] Chi tiết bài: đề, rubric, cài đặt, giao bài / phát hành, đóng / nhân bản — `/giao-vien/bai-tap/[id]` (F3)
- [ ] Tiến độ bài (ma trận realtime, online, nhắc nhở) — `/giao-vien/bai-tap/[id]/tien-do` (F6)
- [ ] Thống kê một bài — `/giao-vien/bai-tap/[id]/thong-ke` (F6)
- [ ] Hàng đợi chấm — `/giao-vien/cham-bai` (F5)
- [ ] Màn chấm bài — `/giao-vien/cham-bai/[id]` (F5)
- [ ] Thống kê lớp, xếp hạng, xuất Excel — `/giao-vien/thong-ke` (F6)
- [ ] Trao đổi (chat) — `/chat` (F6)
- [ ] Ngân hàng đề — `/giao-vien/ngan-hang-de` (Mức 3, bước 17)

**Sinh viên** — 0 / 5
- [ ] Bài được giao (tab Cần làm / Sắp hết hạn / Quá hạn / Đã nộp / Cần sửa) — `/sinh-vien/tong-quan` (F4)
- [ ] Chi tiết bài + lịch sử nộp — `/sinh-vien/bai-tap/[id]` (F4)
- [ ] Màn làm bài: đếm từ, tự lưu nháp, nháp offline, đồng hồ, nộp — `/sinh-vien/lam-bai/[id]` (F4)
- [ ] Kết quả học tập + kết quả từng bài (band, nhận xét, lỗi) — `/sinh-vien/ket-qua` (F6; kết quả từng bài hiện trong chi tiết bài)
- [ ] Trao đổi (chat) — `/chat` (F6)

**Tổng: 14 / 28 màn.**

---

## 2. Đã làm được

### Backend (Spring Boot monolith, PostgreSQL, không Redis)
- **Tài khoản:** đăng nhập JWT + refresh token xoay vòng, đổi mật khẩu (bắt đổi lần đầu), một tài khoản một phiên (`FORCE_LOGOUT`), giới hạn đăng nhập sai 5 lần / 5 phút, admin quản lý giáo viên / người dùng (khoá, đặt lại mật khẩu).
- **Lớp học:** tạo / sửa / xoá lớp, thành viên, import sinh viên từ Excel / CSV (báo lỗi từng dòng, file tài khoản tải một lần, gửi email), import qua TCP bằng `admin-cli`.
- **Bài tập:** nhiều phần, rubric IELTS mẫu, checklist Cần có / Giới hạn điểm, ảnh đề, giao cho lớp / cá nhân, phát hành / hẹn giờ mở, đóng / mở lại / nhân bản, sửa cài đặt sau phát hành (BR-06).
- **Làm bài:** đếm từ (BR-20), lưu nháp có khoá lạc quan (`409 DRAFT_CONFLICT`), nộp từng phần, lịch sử version, giờ server `/api/time`.
- **Chấm bài:** checklist, band theo tiêu chí có giới hạn, mã lỗi, nhận xét inline, ngân hàng nhận xét, trả bài / yêu cầu sửa, band cả bài.
- **Thông báo:** trong app + WebSocket STOMP + email, job nhắc hạn, giáo viên nhắc thủ công.
- **Realtime Mức 2:** online, tiến độ live, chat theo bài nộp / lớp, heartbeat.
- **Thống kê:** ma trận tiến độ, thống kê bài / lớp / sinh viên, xếp hạng, xuất Excel.
- **Bảo mật & triển khai:** nhật ký hệ thống, nginx HTTPS / WSS, Docker 5 container, kiểm thử tải k6 đạt NFR-01/02/03 (docs/09).
- **Quy ước API:** mọi response **HTTP 200**, lỗi nằm trong body `{success: false, errorCode, errorMessage, status}` (docs/05 A.1).

### Frontend (Next.js 16 + antd 6 + Tailwind, theo thiết kế Figma Make trong `design/figma-make/`)
- **Chung:** đăng nhập (lỗi hiện trên form), đổi mật khẩu có danh sách điều kiện, sidebar thu gọn được / drawer trên điện thoại, header (trạng thái kết nối, chuông, tài khoản).
- **Realtime:** một STOMP client cho cả app, tự kết nối lại (1 s → 30 s), lấy bù thông báo bị lỡ, banner mất kết nối, hộp thoại bị đăng xuất; trang Thông báo.
- **Quản trị:** tổng quan + biểu đồ 14 ngày, giáo viên (thêm → mật khẩu tạm một lần), người dùng (chi tiết, khoá, đặt lại mật khẩu), nhật ký (lọc hành động / IP / ngày). Trang Phiên đăng nhập chỉ hiện số phiên (API thuộc Mức 3).
- **Giáo viên — lớp học:** danh sách thẻ lớp, tạo / sửa / xoá, thành viên, thêm theo mã SV, import 3 bước (kết quả, bảng lỗi, tải file tài khoản, cảnh báo khi đóng chưa tải).
- **Giáo viên — bài tập:** danh sách (lọc trạng thái, đếm ngược hạn, đã nộp, chờ chấm), chi tiết (đề, rubric, đã giao cho), cài đặt, giao bài / phát hành (liệt kê lý do chưa phát hành được), đóng / mở lại / nhân bản / xoá, **trình soạn 4 bước** (thông tin, các phần + ảnh đề, tiêu chí + nạp rubric mẫu, giao bài).

### Tài liệu
- docs 01 → 10 (Markdown + Mermaid) và Word gộp `docx/00-tai-lieu-phan-tich-thiet-ke.docx`; docs/10 là hướng dẫn sử dụng + danh sách tài khoản.
- Tài khoản mẫu (profile `demo`): `admin` / `Admin@123` (bắt đổi lần đầu), `gv.demo` / `Demo@2026`, sinh viên `DEMO001` → `DEMO020` / `Demo@2026`.

---

## 3. Còn lại

### Việc chính (theo thứ tự)
1. **F4 — Sinh viên** (Prompt 7): danh sách bài được giao theo tab (Cần làm / Sắp hết hạn / Quá hạn / Đã nộp / Cần sửa), chi tiết bài, **màn làm bài** (`EssayEditor`, đếm từ, tự lưu nháp + xử lý `409 DRAFT_CONFLICT`, nháp offline IndexedDB, đồng hồ theo `GET /api/time`), nộp bài, lịch sử version. Dự kiến tách F4a (danh sách + chi tiết) / F4b (màn làm bài).
2. **F5 — Chấm bài** (Prompt 6 + 6b): hàng đợi chấm, màn chấm (ScorePicker có khoá giới hạn, mã lỗi khi bôi đen, ngân hàng nhận xét, so sánh 2 version), trả bài / yêu cầu sửa.
3. **F6 — Kết quả & thống kê** (Prompt 5, 7, 8): kết quả phía sinh viên, ma trận tiến độ realtime + nhắc nhở, thống kê bài / lớp / sinh viên, xuất Excel, dashboard, trao đổi (chat).
4. Mức 3 (nếu còn thời gian): thi tính giờ, kiểm tra đạo văn, ngân hàng đề, gợi ý chấm AI, TCP qua TLS.

### Việc nhỏ / tồn đọng
- [ ] Push lên GitHub (commit `58e64f9` trở đi chưa push).
- [ ] `CountdownTimer` chưa bù độ lệch giờ máy theo `GET /api/time` — làm cùng F4.
- [ ] Mỗi bước F4 → F6 xong: cập nhật docs/10 (mục 1, 6.4, 7) + xuất lại Word.
- [ ] Backend: `AdminUserService` (module `user`) gọi ngược `ClassroomService` để đếm số lớp — nên đổi sang truy vấn chỉ-đọc.
- [ ] Backend: `grades.updated_at` lấy giờ hệ thống, không qua bean `Clock` (chưa ảnh hưởng nghiệp vụ).
- [ ] Người dùng tự chụp ảnh Wireshark HTTP / HTTPS / TCP 9090 cho báo cáo (hướng dẫn trong README).
- [ ] (tuỳ chọn) File mẫu `tools/docs-export/reference.docx` theo định dạng báo cáo của trường.
