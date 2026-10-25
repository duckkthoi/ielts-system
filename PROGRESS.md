# Tiến độ dự án — Website giao bài & chấm bài IELTS Writing

> Chỉ ghi **đã làm tới đâu · làm được gì · còn lại gì**. Chi tiết từng bước xem lịch sử git.
> Kế hoạch: [PLAN.md](PLAN.md) · Tài liệu chuẩn: [docs/](docs/) · Hướng dẫn sử dụng + tài khoản: [docs/10](docs/10-huong-dan-su-dung.md)

Cập nhật: 24/10/2026

---

## 1. Đang ở đâu

**Backend xong Mức 1 + Mức 2. Frontend xong F1 → F3, F4a (bài được giao + chi tiết bài), F4b + F4c (màn làm bài, nháp offline) — **F4 xong**; F5a (hàng đợi + màn chấm) xong, bước tiếp theo là F5b (nhận xét inline, ngân hàng nhận xét, so sánh version).**

| Phần | Trạng thái |
|---|---|
| Tài liệu phân tích thiết kế (docs 01 → 10 + Word gộp), design system, prompt Figma, quy tắc cho AI | ✅ |
| Backend Mức 1 — lõi nghiệp vụ (bước 1 → 8) | ✅ |
| Backend Mức 2 — trọng tâm mạng (bước 9 → 14) | ✅ (phần frontend của bước 10 đã làm ở F4) |
| Bước 20 — README, dữ liệu demo, tài liệu báo cáo | ✅ (ảnh Wireshark người dùng tự chụp) |
| Frontend F1 — đăng nhập, khung app, thông báo realtime | ✅ |
| Frontend F2 — quản trị; lớp học + import sinh viên | ✅ |
| Frontend F3 — bài tập: danh sách, chi tiết, giao bài, trình soạn | ✅ |
| Frontend F4 — sinh viên: bài được giao, làm bài, nộp bài | ✅ F4a (danh sách theo tab, chi tiết, lịch sử nộp) · F4b (làm bài, tự lưu, xung đột nháp, nộp) · F4c (nháp offline) |
| Frontend F5 — giáo viên chấm bài | 🟡 F5a ✅ (hàng đợi, chấm theo tiêu chí, trả bài, yêu cầu sửa, chấm 0) · F5b ⬜ **tiếp theo** |
| Frontend F6 — kết quả, tiến độ, thống kê, trao đổi | ⬜ |
| Mức 3 (bước 15 → 19) | ⬜ làm nếu còn thời gian |

Kiểm tra gần nhất: backend `mvn test` 466 test case đều đạt · frontend `lint`, `tsc`, `build` sạch, `test:unit` 49 / 49 (F5a: `build` chưa chạy lại vì dev server đang bật theo yêu cầu — `lint`, `tsc` sạch).
Commit gần nhất: F5a (hàng đợi + màn chấm); đã push lên GitHub.

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

**Giáo viên** — 7 / 13
- [ ] Tổng quan (dashboard) — `/giao-vien/tong-quan` (F6)
- [x] Danh sách lớp — `/giao-vien/lop-hoc` (F2)
- [x] Chi tiết lớp: thành viên, thêm sinh viên, import + file tài khoản — `/giao-vien/lop-hoc/[id]` (F2)
- [x] Danh sách bài tập — `/giao-vien/bai-tap` (F3)
- [x] Trình soạn bài (tạo + sửa nháp) — `/giao-vien/bai-tap/tao-moi` (F3)
- [x] Chi tiết bài: đề, rubric, cài đặt, giao bài / phát hành, đóng / nhân bản — `/giao-vien/bai-tap/[id]` (F3)
- [ ] Tiến độ bài (ma trận realtime, online, nhắc nhở) — `/giao-vien/bai-tap/[id]/tien-do` (F6)
- [ ] Thống kê một bài — `/giao-vien/bai-tap/[id]/thong-ke` (F6)
- [x] Hàng đợi chấm — `/giao-vien/cham-bai` (F5a)
- [x] Màn chấm bài — `/giao-vien/cham-bai/[id]` (F5a; nhận xét inline, ngân hàng nhận xét, so sánh version ở F5b)
- [ ] Thống kê lớp, xếp hạng, xuất Excel — `/giao-vien/thong-ke` (F6)
- [ ] Trao đổi (chat) — `/chat` (F6)
- [ ] Ngân hàng đề — `/giao-vien/ngan-hang-de` (Mức 3, bước 17)

**Sinh viên** — 3 / 5
- [x] Bài được giao (tab Cần làm / Sắp hết hạn / Quá hạn / Đã nộp / Cần sửa) — `/sinh-vien/tong-quan` (F4a)
- [x] Chi tiết bài + lịch sử nộp — `/sinh-vien/bai-tap/[id]` (F4a)
- [x] Màn làm bài: đếm từ, tự lưu nháp, xung đột nháp, nháp offline, đồng hồ, nộp — `/sinh-vien/lam-bai/[id]` (F4b, F4c)
- [ ] Kết quả học tập + kết quả từng bài (band, nhận xét, lỗi) — `/sinh-vien/ket-qua` (F6; kết quả từng bài hiện trong chi tiết bài)
- [ ] Trao đổi (chat) — `/chat` (F6)

**Tổng: 19 / 28 màn.**

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
- **Sinh viên — bài được giao:** 5 tab có số đếm (tab giữ trên URL), thẻ bài với trạng thái từng phần, hạn gần nhất + đếm ngược, nút chính Sửa bài / Làm bài / Xem kết quả / Xem chi tiết (`lib/utils/studentAction.ts`, có test); chi tiết bài: khung yêu cầu sửa (hạn, lời nhắn), từng phần (đề, ảnh, tiêu chí + checklist), lịch sử nộp, band từng phần + band cả bài, trang 404 khi không được giao.
- **Đồng hồ theo giờ server:** `lib/utils/serverClock.ts` (có test) lấy độ lệch từ `serverTime` của response; `CountdownTimer` đếm theo đó. Thêm component `BandBadge`.
- **Sinh viên — làm bài:** đề bên trái / ô soạn bên phải (điện thoại: đề thu gọn, thanh nộp dính đáy), tab theo phần, đếm từ khi gõ, **tự lưu nháp** sau 3 giây ngừng gõ / khi rời tab / khi đổi phần (gửi `draftVersion`), **409 DRAFT_CONFLICT** → hộp thoại so 2 bản để chọn, nộp có xác nhận khi thiếu từ (gửi `expectedVersion`; lỗi trạng thái cũ thì tải lại), chế độ chỉ đọc khi đã nộp / quá hạn / bài đóng, đồng bộ giờ `GET /api/time` 3 mẫu lấy mẫu nhanh nhất, 5 phút một lần (SD-11). `ApiError` mang thêm `data` của lỗi.
- **Nháp offline (F4c):** mỗi lần gõ ghi IndexedDB (`lib/offlineDraftStore.ts`, IndexedDB gốc, không thêm thư viện; khoá theo người dùng + phần), lỗi mạng / 502–504 → trạng thái OFFLINE, sự kiện `online` → gửi lại; nút nộp khoá khi mất mạng; mở lại màn mà trên máy còn bản chưa gửi → server chưa đổi thì gửi lên, đã đổi thì hỏi chọn bản (`lib/utils/offlineDraft.ts`, có test). Bản trên máy bị xoá khi đã lưu lên server hoặc đã nộp.
- **Giáo viên — chấm bài (F5a):** hàng đợi lọc theo bài / phần / lớp / từ khoá + tab trạng thái (bộ lọc trên URL); màn chấm bài làm 58% / bảng chấm 42% (dưới 1024px thành 2 tab), cảnh báo số từ, `CriterionCard` (checklist Đạt / Giới hạn điểm) + `ScorePicker` khoá mức vượt giới hạn và tự hạ điểm (BR-17, BR-18), band xem trước (BR-12, `lib/utils/grading.ts` có test), chấm 0 có lý do (BR-19), tự lưu nháp 2 giây, trả bài, yêu cầu sửa (lời nhắn + hạn, còn lượt — BR-10), chọn lần nộp để xem lại, Trước / Sau theo hàng đợi, trả xong tự sang bài kế.

### Tài liệu
- docs 01 → 10 (Markdown + Mermaid) và Word gộp `docx/00-tai-lieu-phan-tich-thiet-ke.docx`; docs/10 là hướng dẫn sử dụng + danh sách tài khoản.
- Tài khoản mẫu (profile `demo`): `admin` / `Admin@123` (bắt đổi lần đầu), `gv.demo` / `Demo@2026`, sinh viên `DEMO001` → `DEMO020` / `Demo@2026`.

---

## 3. Còn lại

### Việc chính (theo thứ tự)
1. **F5b — Chấm bài (phần còn lại)** (Prompt 6b): nhận xét inline (bôi đen → `ErrorCodePicker`, highlight + mã lỗi, gợi ý sửa; offset UTF-16 + `quote`), ngân hàng nhận xét trong ô nhận xét, bảng "Lỗi đã đánh dấu", `DescriptorPanel` (bản diễn giải tiếng Việt + link ielts.org), so sánh 2 lần nộp (diff).
2. **F6 — Kết quả & thống kê** (Prompt 5, 7, 8): kết quả phía sinh viên, ma trận tiến độ realtime + nhắc nhở, thống kê bài / lớp / sinh viên, xuất Excel, dashboard, trao đổi (chat).
3. Mức 3 (nếu còn thời gian): thi tính giờ, kiểm tra đạo văn, ngân hàng đề, gợi ý chấm AI, TCP qua TLS.

### Việc nhỏ / tồn đọng
- [ ] Trang chi tiết bài của giáo viên (`/giao-vien/bai-tap/[id]`) bị tràn ngang trên điện thoại: lưới `lg:grid-cols-[1fr_340px]` cần `grid-cols-1` + `minmax(0,1fr)` như trang sinh viên.
- [ ] Chưa chạy thử trên màn thật khung "Cần sửa" phía sinh viên (F4a / F4b) — thử khi làm F5b bằng nút "Yêu cầu sửa".
- [ ] Chạy `npm run build` cho F5a khi tắt dev server.
- [ ] Mỗi bước F5 → F6 xong: cập nhật docs/10 (mục 1, 6.4 – 6.5, 7) + xuất lại Word.
- [ ] Backend: `AdminUserService` (module `user`) gọi ngược `ClassroomService` để đếm số lớp — nên đổi sang truy vấn chỉ-đọc.
- [ ] Backend: `grades.updated_at` lấy giờ hệ thống, không qua bean `Clock` (chưa ảnh hưởng nghiệp vụ).
- [ ] Người dùng tự chụp ảnh Wireshark HTTP / HTTPS / TCP 9090 cho báo cáo (hướng dẫn trong README).
- [ ] (tuỳ chọn) File mẫu `tools/docs-export/reference.docx` theo định dạng báo cáo của trường.
