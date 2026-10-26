# Tiến độ dự án — Website giao bài & chấm bài IELTS Writing

> Chỉ ghi **đã làm tới đâu · làm được gì · còn lại gì**. Chi tiết từng bước xem lịch sử git.
> Kế hoạch: [PLAN.md](PLAN.md) · Tài liệu chuẩn: [docs/](docs/) · Hướng dẫn sử dụng + tài khoản: [docs/10](docs/10-huong-dan-su-dung.md)

Cập nhật: 26/10/2026

---

## 1. Đang ở đâu

**Backend xong Mức 1 + Mức 2. Frontend xong F1 → F3, F4a (bài được giao + chi tiết bài), F4b + F4c (màn làm bài, nháp offline) — **F4 xong**; F5a (hàng đợi + màn chấm) xong; F5b (branch `feat/essay-annotations`, đã push, chờ PR) xong. F6 tách F6a → F6g; **F6a → F6d xong** trên branch `feat/student-results` (tách từ `feat/essay-annotations`), tiếp theo F6e.**

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
| Frontend F5 — giáo viên chấm bài | ✅ F5a ✅ (hàng đợi, chấm theo tiêu chí, trả bài, yêu cầu sửa, chấm 0) · F5b-1 ✅ (backend V9) · F5b-2 ✅ (đánh dấu kiểu Word, V10) · F5b-3 ✅ (so sánh 2 lần nộp) |
| Frontend F6 — kết quả, tiến độ, thống kê, trao đổi | 🟡 F6a ✅ (kết quả từng phần + so sánh) · F6b ✅ (kết quả học tập) · F6c ✅ (tiến độ realtime) · F6d ✅ (thống kê bài / lớp, xếp hạng) · F6e ⬜ **tiếp theo** (dashboard GV · F6f chat · F6g thông tin cá nhân |
| Mức 3 (bước 15 → 19) | ⬜ làm nếu còn thời gian |

Kiểm tra gần nhất: backend `mvn test` 477 test case đều đạt · frontend `lint`, `tsc`, `build` sạch, `test:unit` 88 / 88.
Commit gần nhất trên `main`: `a72b50e` (F5a, đã push). Branch `feat/essay-annotations`: proxy tunnel + F5b-1 → F5b-3, **đã push** (`main` đã bật bảo vệ → gộp qua PR, mở trên web vì máy chưa có `gh`).

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

**Giáo viên** — 10 / 13
- [ ] Tổng quan (dashboard) — `/giao-vien/tong-quan` (F6)
- [x] Danh sách lớp — `/giao-vien/lop-hoc` (F2)
- [x] Chi tiết lớp: thành viên, thêm sinh viên, import + file tài khoản — `/giao-vien/lop-hoc/[id]` (F2)
- [x] Danh sách bài tập — `/giao-vien/bai-tap` (F3)
- [x] Trình soạn bài (tạo + sửa nháp) — `/giao-vien/bai-tap/tao-moi` (F3)
- [x] Chi tiết bài: đề, rubric, cài đặt, giao bài / phát hành, đóng / nhân bản — `/giao-vien/bai-tap/[id]` (F3)
- [x] Tiến độ bài (ma trận realtime, online, đang mở phần nào, nhắc nhở, xuất Excel) — `/giao-vien/bai-tap/[id]/tien-do` (F6c)
- [x] Thống kê một bài: phân bố band, theo tiêu chí, lỗi hay gặp, Excel — `/giao-vien/bai-tap/[id]/thong-ke` (F6d)
- [x] Hàng đợi chấm — `/giao-vien/cham-bai` (F5a)
- [x] Màn chấm bài — `/giao-vien/cham-bai/[id]` (F5a, F5b: đánh dấu kiểu Word, nhận xét mẫu, so sánh 2 lần nộp)
- [x] Thống kê lớp, xếp hạng, kết quả học tập từng sinh viên — `/giao-vien/thong-ke` (F6d)
- [ ] Trao đổi (chat) — `/chat` (F6)
- [ ] Ngân hàng đề — `/giao-vien/ngan-hang-de` (Mức 3, bước 17)

**Sinh viên** — 5 / 6
- [x] Bài được giao (tab Cần làm / Sắp hết hạn / Quá hạn / Đã nộp / Cần sửa) — `/sinh-vien/tong-quan` (F4a)
- [x] Chi tiết bài + lịch sử nộp — `/sinh-vien/bai-tap/[id]` (F4a)
- [x] Màn làm bài: đếm từ, tự lưu nháp, xung đột nháp, nháp offline, đồng hồ, nộp — `/sinh-vien/lam-bai/[id]` (F4b, F4c)
- [x] Kết quả từng phần: band, tiêu chí + checklist, đánh dấu, lỗi trong bài, so sánh các lần nộp — `/sinh-vien/bai-tap/[id]/ket-qua?phan&lan` (F6a)
- [x] Kết quả học tập: band theo thời gian, trung bình theo tiêu chí, hồ sơ lỗi, kết quả từng bài — `/sinh-vien/ket-qua` (F6b)
- [ ] Trao đổi (chat) — `/chat` (F6)

**Tổng: 24 / 29 màn.**

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
- **Đánh dấu trên bài (F5b-1, V9):** `inline_comments.kind` (lỗi / lưu ý / dùng hay — chỉ lỗi tính vào thống kê), `resolution` + `PATCH …/inline-comments/{id}` để giáo viên đánh giá "đã sửa" lỗi của lần nộp cũ khi so sánh; mã nhóm `GR`, `OTHER` cho 7 nút chọn nhanh. Dùng lại `inline_comments` / `comment_bank` thay vì tạo bảng mới.
- **Đánh dấu kiểu "Word đã chấm" (F5b-2, V10):** `EssayAnnotator` (giáo viên + sinh viên chỉ đọc): tách đoạn theo offset, không innerHTML, xử lý chồng lấn; lỗi gạch gợn sóng màu theo 7 nhóm; sửa thay / xoá / thêm chữ (track changes, chữ gốc không đổi — `inline_comments.edit`); lưu ý vàng, dùng hay xanh, nhận xét xám (`kind COMMENT`); bong bóng nhận xét ở lề phải tự dàn, thanh dọc lề trái; bật / tắt 4 lớp. Bôi đen → 7 nút; form 7 nhóm lỗi + mã IELTS, tiêu chí, nhận xét mẫu (chèn / lưu / xoá); tab "Đánh dấu (N)" có thống kê số lỗi theo nhóm + tiêu chí và danh sách ↔ cuộn tới đoạn. Sinh viên xem bản đã trả cùng giao diện ở màn làm bài.
- **So sánh 2 lần nộp (F5b-3):** nút "So sánh với lần trước" trên màn chấm (khi đã nộp lại); `VersionDiff` Gộp / Song song, diff theo từ bằng LCS tự viết (`lib/utils/textDiff.ts`, không thêm thư viện); tự đoán từng lỗi lần cũ Đã sửa / Đổi khác gợi ý / Chưa sửa (đoạn trích + 3 từ mỗi bên, đối chiếu gợi ý); "Đã sửa X/Y" theo nhóm lỗi + tiêu chí; chỉnh tay / về tự đoán bằng `PATCH …/inline-comments/{id}`; bấm lỗi → viền đoạn trích trên bài cũ. Đã chạy thử với backend thật (chấm → yêu cầu sửa → sinh viên nộp lại → so sánh).
- **Chạy qua tunnel:** proxy của Next (`/api`, `/uploads`, `/ws`), CORS nhận mẫu origin (`https://*.trycloudflare.com`), `allowedDevOrigins`.
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
- **Sinh viên — kết quả từng phần (F6a):** band + mức, khung "Cần sửa" + ghi chú ẩn gợi ý (BR-21), bài kèm đánh dấu (chỉ đọc), "Lỗi trong bài", tiêu chí + checklist (đạt / chưa đạt / điểm yếu giới hạn) + lý do giới hạn, nhận xét chung; chọn lần nộp; **so sánh các lần nộp** (dùng chung `components/VersionCompare` với màn chấm, chỉ xem: đánh giá của giáo viên hoặc tự đoán). Mở từ "Xem kết quả" và từ từng lần trong Lịch sử nộp. Đăng nhập lại giữ cả query của `returnUrl`.
- **Sinh viên — kết quả học tập (F6b):** band trung bình + mức, số phần đã nộp / có kết quả / nộp muộn; biểu đồ đường **band theo thời gian** (band cả bài, Task 1, Task 2; SVG tự vẽ, `lib/utils/bandTrend.ts` có test), thanh **trung bình theo tiêu chí**, bảng **hồ sơ lỗi**, kết quả từng bài (mở ra xem từng phần → kết quả chi tiết). Phần hiển thị là component dùng chung `components/StudentResults` (giáo viên xem một sinh viên ở F6d), cùng `BandTrendChart`, `CriteriaBars`, `ErrorStatsTable`; màu chuỗi biểu đồ `CHART_SERIES` (đã kiểm tra mù màu).
- **Giáo viên — tiến độ realtime (F6c):** ma trận sinh viên × phần (`ProgressCell`: màu trạng thái, band, muộn, quá hạn, `v2`; bấm → màn chấm với bộ lọc hàng đợi theo trạng thái ô), 4 thẻ tóm tắt, lọc lớp / trạng thái / tìm kiếm, `PresenceDot` + "đang mở Task 2"; nghe `/topic/assignments/{id}/progress|presence` → đổi ô ngay + nháy viền, gọi lại REST sau 1,5 s để số liệu tóm tắt do server tính, kết nối lại thì tải lại (`lib/utils/progress.ts` có test). Nhắc nhở (chọn người hoặc mọi người còn phần phải nộp, theo phần), xuất Excel. Màn làm bài của sinh viên giờ gửi `/app/presence/viewing` khi mở / đổi phần, sau khi kết nối lại và khi rời màn. Khung `AssignmentSubNav` chuyển Chi tiết · Tiến độ · Thống kê. Đã chạy thử realtime với backend thật (sinh viên nộp → ô đổi; sinh viên mở Task 2 → giáo viên thấy ngay).
- **Giáo viên — thống kê (F6d):** một bài: band cả bài trung bình, % đúng hạn, bảng từng phần, **phân bố band** (`BandDistributionChart`, đủ mọi mức 0.5 — `lib/utils/bandDistribution.ts` có test), trung bình theo tiêu chí theo phần, lỗi hay gặp + số SV, lọc lớp (`?lop=`), xuất Excel. Lớp: band trung bình lớp, các bài đã giao (hoàn thành, đủ kết quả, band TB), **bảng xếp hạng** (cùng hạng như server trả) → bấm một sinh viên mở drawer **kết quả học tập** (dùng lại `StudentResults`, `GET /api/teacher/stats/students/{id}`). Nút "Thống kê" ở chi tiết bài.
- **Giáo viên — chấm bài (F5a):** hàng đợi lọc theo bài / phần / lớp / từ khoá + tab trạng thái (bộ lọc trên URL); màn chấm bài làm 58% / bảng chấm 42% (dưới 1024px thành 2 tab), cảnh báo số từ, `CriterionCard` (checklist Đạt / Giới hạn điểm) + `ScorePicker` khoá mức vượt giới hạn và tự hạ điểm (BR-17, BR-18), band xem trước (BR-12, `lib/utils/grading.ts` có test), chấm 0 có lý do (BR-19), tự lưu nháp 2 giây, trả bài, yêu cầu sửa (lời nhắn + hạn, còn lượt — BR-10), chọn lần nộp để xem lại, Trước / Sau theo hàng đợi, trả xong tự sang bài kế.

### Tài liệu
- docs 01 → 10 (Markdown + Mermaid) và Word gộp `docx/00-tai-lieu-phan-tich-thiet-ke.docx`; docs/10 là hướng dẫn sử dụng + danh sách tài khoản.
- Tài khoản mẫu (profile `demo`): `admin` / `Admin@123` (bắt đổi lần đầu), `gv.demo` / `Demo@2026`, sinh viên `DEMO001` → `DEMO020` / `Demo@2026`.

---

## 3. Còn lại

### Việc chính (theo thứ tự)
1. **Mở PR** `feat/essay-annotations` → `main` trên GitHub và gộp (branch đã push).
2. `DescriptorPanel` (bản diễn giải tiếng Việt + link ielts.org) — làm sau F5b nếu còn thời gian.
3. **F6 — Kết quả & thống kê** (Prompt 5, 7, 8): ~~kết quả từng phần (F6a)~~ ✅, ~~kết quả học tập (F6b)~~ ✅, ~~ma trận tiến độ realtime + nhắc nhở (F6c)~~ ✅, ~~thống kê bài / lớp / sinh viên, xuất Excel (F6d)~~ ✅, dashboard, trao đổi (chat).
4. Mức 3 (nếu còn thời gian): thi tính giờ, kiểm tra đạo văn, ngân hàng đề, gợi ý chấm AI, TCP qua TLS.

### Việc nhỏ / tồn đọng
- [ ] Chưa chạy thử trên màn thật khung "Cần sửa" phía sinh viên (F4a / F4b) — thử khi làm F5b bằng nút "Yêu cầu sửa".
- [ ] Mỗi bước F5 → F6 xong: cập nhật docs/10 (mục 1, 6.4 – 6.5, 7) + xuất lại Word.
- [ ] Backend: `AdminUserService` (module `user`) gọi ngược `ClassroomService` để đếm số lớp — nên đổi sang truy vấn chỉ-đọc.
- [ ] Backend: `grades.updated_at` lấy giờ hệ thống, không qua bean `Clock` (chưa ảnh hưởng nghiệp vụ).
- [ ] Người dùng tự chụp ảnh Wireshark HTTP / HTTPS / TCP 9090 cho báo cáo (hướng dẫn trong README).
- [ ] (tuỳ chọn) File mẫu `tools/docs-export/reference.docx` theo định dạng báo cáo của trường.
