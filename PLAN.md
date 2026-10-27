# Kế hoạch dự án — Website giao bài & chấm bài IELTS Writing

> Đồ án môn **Lập trình mạng** (LTMTU). File này nói **làm gì, theo thứ tự nào, xong thì kiểm tra ra sao**.
> Chi tiết kỹ thuật (bảng, API, trạng thái, quy tắc nghiệp vụ) nằm trong [`docs/`](docs/) — **`docs/` là bản chuẩn**, chỗ nào lệch thì theo `docs/`.
> Tiến độ thực tế, từng bước đã làm gì: [`PROGRESS.md`](PROGRESS.md). Quy tắc viết code: [`CLAUDE.md`](CLAUDE.md).
> Cập nhật lần cuối: 23/10/2026, sau bước 14 và 20.

---

## 1. Mục tiêu

**Đề gốc:** server tạo tài khoản sinh viên theo mã sinh viên từ danh sách giáo viên cung cấp. Giáo viên tạo bài tập nhiều phần, giao theo lớp hoặc cá nhân, đặt hạn và tiêu chí chấm. Sinh viên làm bài, lưu nháp, nộp từng phần. Giáo viên chấm theo checklist, nhận xét, trả bài, yêu cầu sửa. Hệ thống nhắc hạn, theo dõi tiến độ, lưu lịch sử nộp, thống kê kết quả.

**Mở rộng** (giảng viên thích đề tài lớn), chia 3 mức để lúc nào cũng có một bản chạy được:

| Mức | Nội dung | Vai trò trong báo cáo |
|---|---|---|
| **1 — Lõi nghiệp vụ** | Toàn bộ đề gốc + nghiệp vụ chấm IELTS đúng chuẩn ([docs/08](docs/08-nghiep-vu-cham-ielts.md)) | Bắt buộc |
| **2 — Trọng tâm mạng** | WebSocket nâng cao, chịu lỗi mạng, **server TCP tự viết + admin-cli**, HTTPS/WSS, rate limit, Docker + nginx, load test | Phần ghi điểm của môn |
| **3 — Mở rộng** | Chế độ thi, đạo văn, ngân hàng đề, gợi ý AI, TCP qua TLS / NIO | Làm khi còn thời gian |

---

## 2. Cách làm việc (đã thống nhất)

1. **Làm từng bước nhỏ.** Xong mỗi bước: chạy test → cập nhật tài liệu liên quan → **ghi vào `PROGRESS.md`** → báo cáo → **dừng, chờ người dùng xác nhận** rồi mới làm bước sau.
2. Bước lớn tách thành bước con (B6a, B6b…), mỗi bước con cũng dừng như trên.
3. **Backend trước, frontend sau.** Giao diện làm theo thiết kế Figma Make (prompt ở [`design/figma-make-prompts.md`](design/figma-make-prompts.md)); chưa có thiết kế thì không code giao diện.
4. Chỉ **commit / push khi người dùng yêu cầu**. Repo: https://github.com/duckkthoi/ielts-system (nhánh `main`).
5. Ràng buộc bắt buộc (bảo mật, bản quyền): xem [`CLAUDE.md`](CLAUDE.md) mục 2.

---

## 3. Công nghệ (thực tế đang dùng)

| Phần | Công nghệ |
|---|---|
| Backend | Spring Boot **4.0** (Java 21, máy dev JDK 24), Spring Security + JWT (jjwt), Data JPA (Hibernate 7), WebSocket STOMP, Mail, Flyway, Jackson 3, Apache POI, Commons CSV, jsoup, springdoc |
| CSDL | PostgreSQL **18** cài trên máy (`localhost:5432`): DB `ielts` (dev), `ielts_test` (test); trong Docker dùng container `postgres:18` (bước 13) |
| Frontend | Next.js **16** (App Router), React 19, Ant Design 6, Tailwind CSS 4, Zustand 5, axios, dayjs, `@stomp/stompjs` |
| Email khi dev | Mailpit (`tools/mailpit/`, SMTP 1025, giao diện 8025) |
| Tài liệu | Markdown + Mermaid trong `docs/`, xuất Word bằng `tools/docs-export/export_docx.py` (pandoc + mermaid-cli) |

**Kiến trúc: monolith** — một ứng dụng Spring Boot chứa REST, WebSocket, server TCP, job định kỳ. Không microservice, không Redis, không chạy nhiều instance. Bên trong chia **module theo nghiệp vụ**, phụ thuộc chỉ đi một chiều:

```
common, audit  ←  (mọi module đều dùng được)
user ← classroom ← assignment ← submission ← grading ← notification
                                                 ↖ stats (chỉ đọc), tcpgateway, realtime, chat
demo (dữ liệu mẫu, profile demo) — trên cùng, chỉ gọi service của các module khác
```
Module sau gọi module trước qua **service "cửa"** (vd `StudentAssignmentAccess`, `TeacherSubmissionAccess`); module trước báo cho module sau qua **sự kiện** (`AssignmentEvents`, `SubmissionEvents`, `GradingEvents`).

---

## 4. Trạng thái hiện tại

| # | Hạng mục | Trạng thái |
|---|---|---|
| A1–A5 | Kế hoạch, khung frontend, 8 tài liệu phân tích thiết kế (+ Word), design system, 11 prompt Figma | ✅ |
| A6 | Quy tắc code cho AI agent: backend, frontend, file gốc | ✅ (viết lại ngày 22/10) |
| 1–5 | Khung dự án · đăng nhập JWT · lớp + import sinh viên · bài tập + rubric + giao bài · làm bài, nộp, version | ✅ backend |
| 6 | Chấm bài: checklist giới hạn band, mã lỗi, nhận xét inline, ngân hàng nhận xét, BandCalculator, trả bài / yêu cầu sửa, kết quả phía sinh viên | ✅ backend |
| 7 | Thông báo (DB + WebSocket + email), job nhắc hạn, bài hẹn giờ mở, nhắc thủ công | ✅ backend |
| 8 | Tiến độ, thống kê, xuất Excel | ✅ backend (B8a–B8e) |
| F | Giao diện frontend (thiết kế: code Figma Make trong `design/figma-make/`) | 🟡 F1 ✅ (khung app, đăng nhập, thông báo realtime) · F2 ✅ (quản trị, lớp học + import) · F3 ✅ (bài tập: danh sách, chi tiết, giao bài, trình soạn) · F4 → F6 ⬜ |
| 9–14 | Mức 2 | ✅ backend: 11 (TCP + admin-cli) · 12 (nhật ký, rate limit, nginx) · 9 (một phiên, online, tiến độ live, chat) · 10 (khoá lạc quan bản nháp; IndexedDB ở frontend) · 13 (Docker) · 14 (k6: NFR-01/02/03 đạt — [docs/09](docs/09-kiem-thu-tai.md)) |
| 20 | README, dữ liệu demo, tài liệu | ✅ `README.md`, profile `demo`, `docs/09`; ảnh Wireshark người dùng tự chụp (hướng dẫn trong README) |
| 15–19 | Mức 3 | ⬜ làm khi còn thời gian |

Backend hiện có **468 lượt test tự động** (+ 6 test của `admin-cli`), tất cả pass.

---

## 5. Lộ trình còn lại

Mỗi bước ghi: **làm gì** → **xong khi nào** (điều kiện nghiệm thu). Bước có thể tách nhỏ hơn khi làm.

### Mức 1

**B8 — Tiến độ, thống kê, xuất Excel** (UC-16, UC-19, UC-28; [docs/05 A.6](docs/05-api.md))
- B8a ✅: ma trận tiến độ `GET /api/teacher/assignments/{id}/progress?classId&status` — hàng = sinh viên nhận bài, cột = phần, ô = trạng thái + muộn + band (nếu đã trả); tóm tắt tỉ lệ hoàn thành, số nộp muộn.
- B8b ✅: thống kê một bài `…/stats?classId` (số nộp, tỉ lệ nộp đúng hạn, band trung bình từng phần và từng tiêu chí, phân bố band, band cả bài, **mã lỗi hay gặp**).
- B8c ✅: kết quả học tập của một sinh viên — giáo viên xem `GET /api/teacher/stats/students/{id}` (chỉ bài của mình), sinh viên xem `GET /api/student/results` (band theo thời gian, theo tiêu chí, số nộp muộn, **hồ sơ lỗi**).
- B8d ✅: thống kê lớp `GET /api/teacher/stats/classes/{id}` (các bài giao cho lớp, bảng xếp hạng).
- B8e ✅: xuất bảng điểm `GET /api/teacher/assignments/{id}/export` (`.xlsx`, Apache POI).
- *Xong khi:* số liệu đúng với dữ liệu test dựng sẵn (kể cả người chưa nộp, nộp muộn, đang "Cần sửa", "Chấm 0"); sinh viên chỉ thấy điểm đã trả (BR-13); file Excel mở được, đúng cột; truy vấn thống kê không N+1.

### Frontend (làm khi có thiết kế Figma — người dùng gửi link)
Thứ tự theo luồng demo; mỗi bước là một nhóm màn hình, gọi API đã có:

| Bước | Màn hình |
|---|---|
| F1 | Khung app theo thiết kế: layout, sidebar, header, **chuông thông báo + WebSocket client** (kết nối lại, lấy thông báo bị lỡ), ConnectionBanner |
| F2 | Admin: giáo viên, người dùng · Giáo viên: lớp học, **import sinh viên** (kết quả import, tải file tài khoản) |
| F3 | Soạn bài tập nhiều bước (phần, rubric mẫu, checklist Đạt / Giới hạn điểm, ảnh đề), giao bài |
| F4 | Sinh viên: danh sách bài theo tab, **màn làm bài** (đếm từ, tự lưu nháp, đồng hồ theo giờ server), lịch sử nộp |
| F5 | Giáo viên: hàng đợi chấm, **màn chấm** (ScorePicker có khoá giới hạn, mã lỗi khi bôi đen, ngân hàng nhận xét, so sánh 2 version) |
| F6 | Kết quả phía sinh viên (bảng lỗi, ẩn gợi ý khi "Cần sửa"), tiến độ, thống kê, biểu đồ |

*Xong mỗi bước khi:* khớp thiết kế; `npm run lint`, `npm run build`, `npm run test:unit` sạch; chạy thử tay đúng luồng trên backend thật.

### Mức 2 — Trọng tâm mạng
Phần backend của Mức 2 làm được trước khi có Figma; phần giao diện đi kèm làm cùng bước F tương ứng.

| Bước | Làm gì | Xong khi |
|---|---|---|
| **9** Realtime nâng cao | Online / đang làm bài nào (`/topic/assignments/{id}/presence`), tiến độ live (`/progress`), chat theo bài nộp và theo lớp (đang gõ, đã xem), **một tài khoản một phiên** (`FORCE_LOGOUT`), phân quyền từng kênh `/topic` | Test WebSocket thật: đúng người nhận, sai quyền → ERROR; đăng nhập máy 2 thì máy 1 bị đá ra ngay |
| **10** Đồng bộ giờ, nháp offline | `GET /api/time` + client tính độ lệch (kiểu NTP); `draft_version` + `409 DRAFT_CONFLICT` (migration V6); frontend lưu nháp IndexedDB, đồng bộ khi có mạng | Tắt mạng khi đang gõ không mất chữ; 2 tab sửa cùng lúc → hộp thoại chọn bản |
| **11** Server TCP + admin-cli | `TcpImportServer` (`ServerSocket` + thread pool, `SmartLifecycle`, timeout), giao thức text theo [docs/05 phần C](docs/05-api.md); project `admin-cli` (Java thuần); parser khung tin tách riêng để unit test | `java -jar admin-cli.jar import …` chạy có tiến trình; token sai → `ERR 401`; dòng lỗi → `ERR 400 <dòng>`; tái dùng `StudentImportService` |
| **12** Bảo mật mạng | nginx HTTPS/WSS (chứng chỉ tự ký, HTTP → HTTPS); rate limit trong bộ nhớ (đăng nhập sai 5 lần / 5 phút → `429`, nhắc thủ công); bảng `audit_logs` (V6) | Đăng nhập sai lần 6 → 429; ảnh Wireshark HTTP lộ mật khẩu vs HTTPS không lộ |
| **13** Docker hoá | `docker-compose.yml`: postgres, mailpit, backend, frontend, nginx; `.env.example`; profile `docker` | `docker compose up --build` chạy đủ luồng demo; `docker compose restart backend` → client tự kết nối lại, không mất thông báo (NFR-04) |
| **14** Load test ✅ | k6: 200 sinh viên nộp trong 5 phút cuối + 500 kết nối WebSocket | p95 < 500 ms, lỗi < 1%; kết quả ghi vào `docs/` — đã đạt: p95 9,9 ms, 0% lỗi; 500 / 500 kết nối, thông báo p95 228 ms ([docs/09](docs/09-kiem-thu-tai.md)) |

### Mức 3 — Mở rộng (mỗi mục độc lập, migration từ V13)
15. Chế độ thi tính giờ: `exam_sessions`, tự nộp khi hết giờ, ghi sự kiện chuyển tab / dán / mất kết nối.
16. Kiểm tra đạo văn nội bộ (n-gram + Jaccard), cảnh báo cho giáo viên.
17. Ngân hàng đề + tag dạng đề. **B17a backend ✅** (V13, CRUD/tìm kiếm/lọc, rubric riêng, phân quyền theo giáo viên) · **B17b trình soạn ✅** (lưu/nạp đề + rubric, giữ hạn riêng/trọng số) · còn B17c giao diện `/giao-vien/ngan-hang-de`.
18. Gợi ý chấm bằng AI (Claude API, chỉ khi có `ANTHROPIC_API_KEY`; chỉ gợi ý, giáo viên quyết định).
19. TCP qua TLS (`SSLServerSocket`) + bản NIO (`Selector`) để so sánh hiệu năng.
20. ✅ README, profile `demo` (1 giáo viên, 2 lớp, 20 sinh viên, 2 bài), hướng dẫn chụp ảnh Wireshark, tài liệu báo cáo (`docs/09` + file Word gộp). Ảnh Wireshark do người dùng tự chụp. *(Làm trước Mức 3 vì cần cho nộp đồ án.)*

**Thứ tự đề xuất (người dùng chốt 22/10: hoàn thiện backend trước, frontend để sau):** B8 → phần backend của 11, 12, 9, 10 → 13 → 14 → 20 (đã xong 23/10) → frontend F1–F6 khi có Figma → Mức 3 (15–19) nếu còn thời gian.

---

## 6. Kịch bản demo / nghiệm thu cuối

1. `docker compose up --build` (`.env`: `SPRING_PROFILES_ACTIVE=docker,demo` để có sẵn giáo viên `gv.demo`, 2 lớp, 20 sinh viên — README), mở `https://localhost`.
2. Admin tạo giáo viên. Giáo viên tạo lớp, **import file sinh viên** (qua web và qua **admin-cli TCP**), Mailpit nhận email tài khoản.
3. Giáo viên tạo bài 2 phần (Task 1 + Task 2, rubric IELTS), giao cho lớp. Sinh viên đang online nhận thông báo realtime.
4. Sinh viên đăng nhập bằng mã SV, đổi mật khẩu, làm bài; **tắt mạng khi đang gõ** rồi bật lại — nháp không mất. Nộp Task 1.
5. Giáo viên thấy ô tiến độ đổi màu ngay; chấm bằng checklist (thử mục "Giới hạn điểm"), mã lỗi, nhận xét inline; yêu cầu sửa.
6. Sinh viên thấy mã lỗi (gợi ý sửa đang ẩn), nộp v2, so sánh 2 version. Giáo viên trả bài; sinh viên thấy band phần và band cả bài.
7. Đặt hạn còn < 2 giờ → nhận nhắc hạn (thông báo + email), không bị nhắc trùng.
8. Thống kê, xuất Excel. Đăng nhập cùng tài khoản ở máy khác → máy cũ bị đăng xuất.
9. `docker compose restart backend` → client tự kết nối lại, lấy đủ thông báo bị lỡ. Chạy k6 và mở kết quả.

---

## 7. Quyết định đã chốt (tóm tắt)

| Quyết định | Lý do / nơi ghi |
|---|---|
| Monolith, không Redis, một instance | Người dùng chốt; online / rate limit giữ trong bộ nhớ |
| Điểm tiêu chí nguyên band (bước 1), cho phép 0.5; Task 2 trọng số 2; chỉ làm tròn .25 / .75 khi hiển thị | [docs/08](docs/08-nghiep-vu-cham-ielts.md) mục 3 |
| Checklist 2 loại: Đạt / Giới hạn điểm (`max_band`); bài ≤ 20 từ tối đa Band 1; "Chấm 0" phải có lý do | BR-16 → BR-19 |
| Không chép nguyên văn bảng mô tả band IELTS — chỉ diễn giải tiếng Việt + link bản chính thức | Bản quyền, docs/08 mục 5.4 |
| Thông báo lưu cùng transaction nghiệp vụ, đẩy WebSocket / email sau commit | Không có thông báo "ma", không mất thông báo |
| Chống trùng bằng ràng buộc DB (`FOR UPDATE`, `ON CONFLICT DO NOTHING`, UPDATE có điều kiện), không "kiểm tra rồi mới làm" | Có test đồng thời cho từng chỗ |
| Frontend chỉ clone **cấu trúc** từ dự án tham khảo cbs-fe, không chép giao diện, không dùng thư viện độc quyền | Ràng buộc của người dùng |
