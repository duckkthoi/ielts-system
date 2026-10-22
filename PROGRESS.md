# Nhật ký tiến độ dự án — Website giao bài & chấm bài IELTS

> Cập nhật sau **mỗi bước** làm xong. Mỗi mục ghi: đã làm gì, file nào, quyết định nào khác so với kế hoạch, đã kiểm tra ra sao, còn tồn đọng gì.
> Kế hoạch tổng: [PLAN.md](PLAN.md).

---

## Tổng quan trạng thái

Ký hiệu: ✅ xong · 🟡 đang làm / xong một phần · ⬜ chưa làm

### Chuẩn bị (tài liệu, thiết kế, quy tắc)
| # | Hạng mục | Trạng thái |
|---|---|---|
| A1 | Kế hoạch tổng (`PLAN.md`) | ✅ |
| A2 | Khung frontend Next.js (chỉ cấu trúc, theo cbs-fe) | ✅ |
| A3 | Tài liệu: use case, sequence, ERD, đặc tả API (Markdown + Mermaid, xuất thêm `.docx`) | ✅ 6 tài liệu + file Word gộp |
| A4 | Design system | ✅ tài liệu + token trong code |
| A5 | Prompt chi tiết cho Figma Make | ✅ 11 prompt |
| A6 | Coding rules cho AI agent (FE + BE) | 🟡 xong backend, còn frontend + file gốc |

### Mức 1 — Lõi nghiệp vụ
| # | Hạng mục | Trạng thái |
|---|---|---|
| 1 | Khung dự án: backend, frontend, Flyway, seed admin + rubric IELTS, docker-compose | ✅ chạy với PostgreSQL trên máy (docker-compose để bước 13) |
| 2 | Đăng nhập JWT, đổi mật khẩu, layout theo vai trò, admin quản lý giáo viên | ✅ backend (frontend chờ Figma) |
| 3 | Lớp học, import sinh viên từ file, xuất danh sách tài khoản, gửi email | ✅ backend (frontend chờ Figma) |
| 4 | Bài tập: các phần, rubric, checklist, giao bài, publish | ✅ backend (thông báo khi phát hành nối ở B7) |
| 5 | Sinh viên làm bài: lưu nháp, nộp từng phần, version | ✅ backend (frontend chờ Figma; nháp offline + xung đột ở Mức 2) |
| 6 | Chấm bài: checklist, điểm, nhận xét inline, trả bài, yêu cầu sửa, diff | ✅ backend (B6a–c). So sánh 2 version (diff) làm ở frontend từ nội dung 2 version mà API đã trả |
| 7 | Thông báo: WebSocket, email, job nhắc hạn | ✅ backend (B7a: thông báo, WebSocket, email; B7b: job nhắc hạn, bài hẹn giờ mở, nhắc thủ công) |
| 8 | Tiến độ, thống kê, xuất Excel | ⬜ |

### Mức 2 — Trọng tâm mạng
| # | Hạng mục | Trạng thái |
|---|---|---|
| 9 | Realtime nâng cao: online, tiến độ live, chat, một phiên, heartbeat | ⬜ |
| 10 | Đồng bộ giờ server, nháp offline (IndexedDB), xung đột bản nháp | ⬜ |
| 11 | Module TCP: `TcpImportServer` + `admin-cli` | ⬜ |
| 12 | Bảo mật: HTTPS/WSS, rate limit, audit log | ⬜ |
| 13 | Docker hoá toàn bộ: postgres + mailpit + backend (monolith) + frontend + nginx reverse proxy | ⬜ |
| 14 | Load test k6 | ⬜ |

### Mức 3 — Mở rộng
| # | Hạng mục | Trạng thái |
|---|---|---|
| 15 | Chế độ thi tính giờ | ⬜ |
| 16 | Kiểm tra đạo văn nội bộ | ⬜ |
| 17 | Ngân hàng đề | ⬜ |
| 18 | Gợi ý chấm bằng AI (tuỳ chọn) | ⬜ |
| 19 | TCP qua TLS + bản NIO | ⬜ |
| 20 | README, dữ liệu demo, tài liệu báo cáo | ⬜ |

---

## Nhật ký chi tiết

### 2026-10-22 · Bước A1 — Lập kế hoạch
**Đã làm**
- Phân tích đề gốc + yêu cầu mở rộng, chốt công nghệ với người dùng: Spring Boot + React (Next.js) + PostgreSQL, WebSocket, Docker, email, giao diện tiếng Việt, 3 vai trò Admin / Giáo viên / Sinh viên.
- Vì là môn **Lập trình mạng** và giảng viên thích đề tài lớn → bổ sung khối mạng (mục 9) và tính năng mở rộng (mục 10), chia 3 mức ưu tiên.

**File:** [PLAN.md](PLAN.md)

---

### 2026-10-22 · Mức 1 / Bước 1 (một phần) — Khung backend + database
**Đã làm**
- Tạo backend `backend/` từ start.spring.io: Maven, Java 21, group `com.ltmtu`, artifact `ielts`.
- Thư viện: web MVC, security, data-jpa, postgresql, flyway, validation, websocket, mail, actuator, data-redis, lombok, testcontainers; thêm jjwt 0.12.6, Apache POI 5.4.1, commons-csv 1.14.0, springdoc-openapi 3.0.0, MapStruct 1.6.3.
- Cấu hình `application.yml` đọc biến môi trường (DB, Redis, mail, JWT, admin, CORS, upload, AI key) + 2 profile `dev` và `docker`.
- Gói `common/`:
  - `BaseEntity`: id, createdAt, updatedAt tự điền bằng JPA auditing.
  - `ApiException`: lỗi nghiệp vụ, có mã HTTP và mã lỗi.
  - `GlobalExceptionHandler`: trả lỗi JSON, message tiếng Việt.
  - `PageResponse`.
- `config/`:
  - `AppProperties`: gom cấu hình `app.*`.
  - `SecurityConfig` tạm thời: stateless, CORS, BCrypt; JWT làm ở bước 2.
  - `AsyncConfig`: bật `@Async` và `@Scheduled`.
  - `JpaConfig`: bật auditing.
- `user/`:
  - Entity `User`, enum `Role`, `UserRepository`.
  - `AdminSeeder`: tạo admin đầu tiên từ biến môi trường, bắt đổi mật khẩu lần đầu.
- Tạo sẵn các package rỗng (`package-info.java` có mô tả): security, classroom, assignment, submission, grading, notification, stats, realtime, exam, plagiarism, tcpgateway.
- Flyway:
  - [V1__core_schema.sql](backend/src/main/resources/db/migration/V1__core_schema.sql) gồm 19 bảng của Mức 1, có ràng buộc CHECK, khoá ngoại và index.
    - Giao bài: mỗi dòng `assignment_targets` trỏ tới đúng 1 lớp hoặc 1 sinh viên (`num_nonnulls = 1`).
    - `submission_versions` là lịch sử nộp, không bao giờ sửa.
  - [V2__seed_rubric_ielts.sql](backend/src/main/resources/db/migration/V2__seed_rubric_ielts.sql): rubric mẫu IELTS Task 1 và Task 2, mỗi bài 4 tiêu chí, kèm checklist tiếng Việt.

**Khác so với kế hoạch**
- Dùng **Spring Boot 4.0.8**, không dùng 3.5, vì start.spring.io không còn cung cấp bản 3.x.
- Thêm 2 bảng `rubric_template_criteria` và `rubric_template_checklist_items` để lưu tiêu chí và checklist của rubric mẫu. Plan chỉ có bảng `rubric_templates` thì không đủ chỗ lưu.
- Thêm cột `assignment_parts.task_type`, `submissions.revision_note`, `users.last_login_at`.
- Bảng `reminder_log` có thêm loại nhắc hạn sửa bài: `REVISION_H24`, `REVISION_H2`.

**Kiểm tra:** `mvn compile` ✅. **Chưa chạy migration thật** vì máy chưa có PostgreSQL/Docker.

---

### 2026-10-22 · Bước A2 — Khung frontend Next.js (clone cấu trúc cbs-fe)
**Đã làm**
- Ban đầu tạo frontend bằng Vite, sau đó **bỏ và làm lại bằng Next.js** để theo cấu trúc dự án tham khảo `cbs-fe`.
  - Chỉ học cấu trúc và quy ước. **Không** dùng `@super/ui` / `@super/sdk`, vì đó là thư viện nội bộ của đơn vị khác.
- Công nghệ: Next.js 16.4 (App Router), React 19, Ant Design 6 (+ `@ant-design/nextjs-registry`, icons), Tailwind CSS 4, Zustand 5, axios, dayjs, clsx, lodash, lucide-react, `@stomp/stompjs`. Prettier cấu hình giống cbs-fe; ESLint có thêm `eslint-config-prettier`.
- Cấu trúc:
  - `app/(admin)/` là layout có sidebar, dùng cho cả 3 vai trò, menu đổi theo vai trò (`const.tsx` → `MENU_BY_ROLE`).
  - `app/(main)/` là layout toàn màn hình: làm bài, chấm bài, chat.
  - `dang-nhap/` và `doi-mat-khau/` đi theo mẫu `page.tsx` + `index.tsx` + `type.ts` + `const.tsx`.
  - 24 trang khung tạm (`PagePlaceholder`) theo đúng các route trong plan, ví dụ `/giao-vien/bai-tap/[id]/tien-do`, `/sinh-vien/lam-bai/[id]`.
- `lib/`:
  - `axiosClient.ts`: gắn JWT; khi gặp 401 thì tự refresh token (gộp các request 401 cùng lúc thành một lần refresh), lỗi thì quay về đăng nhập; hiện toast lỗi và loading toàn cục.
  - `api/base.ts`: `formatUrl`, `createBaseCrud` cho REST.
  - `api/auth.ts`.
  - `constants/`: vai trò, trang chủ theo vai trò, quyền đường dẫn.
  - `types/`, `storage.ts` (namespace `ielts:`), `tokens.ts`.
  - `utils/date.ts`.
  - `utils/hooks.ts` → `useAuthGuard`: chưa đăng nhập thì về `/dang-nhap?returnUrl=…`; bị bắt đổi mật khẩu thì về `/doi-mat-khau`; sai vai trò thì về trang chủ của vai trò.
- `stores/`:
  - `useAuthStore`: đăng nhập, lấy phiên, đăng xuất; persist user.
  - `useAppStore`: loading, và cầu nối `message` để dùng toast ngoài React.
- `components/`: `AppDrawer`, `AppModal`, `IconButton`, `AppHeaderActions` (chuông thông báo + menu tài khoản), `PagePlaceholder`.
- Trang đăng nhập và đổi mật khẩu đã có **logic** (form, validate, điều hướng, chống open redirect qua `returnUrl`). **Giao diện** chờ bản Figma.

**Khác so với kế hoạch**
- Dùng Next.js thay cho Vite + React Router. Bỏ TanStack Query, react-hook-form, zod, vì cbs-fe dùng AntD Form và Zustand.
- **Tắt `cacheComponents` / `partialPrefetching`** mà template Next 16.4 bật sẵn. Lý do: antd sinh `Math.random()` khi prerender làm build lỗi, và dữ liệu lấy ở phía client nên không cần.

**Kiểm tra:** `npm run build` ✅ (28 route) · `npm run lint` ✅ (0 lỗi).

---

### 2026-10-22 · Backend trả response cùng một dạng
**Đã làm**
- Thêm [ApiResponse.java](backend/src/main/java/com/ltmtu/ielts/common/ApiResponse.java) với dạng `{ success, data, message, totalRecord }`. Có các hàm `ok`, `message`, `page`.
- `ErrorResponse` đổi sang cùng khung `{ success: false, data: null, message, code, status, path, timestamp, fieldErrors }`, để `axiosClient` xử lý lỗi ở một chỗ.

**Kiểm tra:** `mvn compile` ✅.

---

### 2026-10-22 · Bước A3.1 — Tài liệu tổng quan + công cụ xuất Word
**Đã làm**
- [docs/01-tong-quan.md](docs/01-tong-quan.md) gồm:
  - bối cảnh và mục tiêu, trọng tâm Lập trình mạng;
  - phạm vi: trong và ngoài phạm vi;
  - 7 tác nhân;
  - **52 yêu cầu chức năng `FR-xx`**, chia 7 nhóm, mỗi yêu cầu ghi mức ưu tiên 1/2/3;
  - **15 yêu cầu phi chức năng `NFR-xx`**, mỗi yêu cầu có cách kiểm chứng;
  - **15 quy tắc nghiệp vụ `BR-xx`**: import, giao bài, hạn nộp, nộp muộn, số lần sửa, làm tròn band, quyền xem điểm;
  - sơ đồ kiến trúc tổng thể (Mermaid), bảng công nghệ, bảng thuật ngữ.
  - Các file tài liệu sau sẽ tham chiếu các mã FR, NFR, BR này.
- Công cụ xuất Word [tools/docs-export/export_docx.py](tools/docs-export/export_docx.py):
  - Render sơ đồ Mermaid thành PNG bằng mermaid-cli, dùng Chrome có sẵn trên máy, không tải thêm trình duyệt.
  - Chuyển sang `.docx` bằng pandoc (gói `pypandoc_binary`), có mục lục.
  - Kết quả nằm trong thư mục **`docx/`**, ảnh sơ đồ trong `docx/hinh/`.
  - Lệnh: `python tools/docs-export/export_docx.py [tên-file]`.
- Đã xuất [docx/01-tong-quan.docx](docx/01-tong-quan.docx).

**Khác so với kế hoạch:** ban đầu định gộp toàn bộ vào một file `.docx` ở cuối. Theo yêu cầu của người dùng, giờ mỗi tài liệu được xuất ra Word ngay khi viết xong, đặt chung trong thư mục `docx/`. File gộp vẫn làm ở cuối bước A3.

**Kiểm tra:** đã xuất thành công. Ảnh sơ đồ hiển thị đúng tiếng Việt.

**Còn lại:** chưa có file mẫu định dạng `reference.docx` (font Times New Roman 13 theo chuẩn báo cáo). Script tự dùng file này nếu đặt vào `tools/docs-export/`.

---

### 2026-10-22 · Chốt kiến trúc nguyên khối (monolith)
**Quyết định của người dùng:** không cần microservice, chỉ cần **một ứng dụng backend duy nhất**.

**Đã sửa**
- **Bỏ** phương án 2 instance backend + nginx cân bằng tải + Redis pub/sub. **Bỏ hẳn Redis.**
- Trạng thái online và rate limit đăng nhập chuyển sang lưu **trong bộ nhớ** của backend (`ConcurrentHashMap`).
- nginx **chỉ làm reverse proxy**: bật HTTPS/WSS, chuyển `/` sang frontend, `/api` và `/ws` sang backend.
- Server TCP (`tcpgateway`) chạy ngay trong backend.
- Thay kịch bản demo "tắt 1 instance" bằng **"khởi động lại backend → client tự kết nối lại và lấy thông báo bị lỡ từ DB"**. Thông báo luôn được lưu DB trước rồi mới đẩy qua WebSocket. NFR-04 được sửa theo.
- Prometheus và Grafana chuyển thành **tuỳ chọn**.
- Backend: bỏ `spring-boot-starter-data-redis` (cả bản test), cấu hình `spring.data.redis`, và container Redis trong Testcontainers.
- Cập nhật [PLAN.md](PLAN.md), [docs/01-tong-quan.md](docs/01-tong-quan.md): sơ đồ kiến trúc vẽ lại dạng monolith. Đã xuất lại [docx/01-tong-quan.docx](docx/01-tong-quan.docx).

**Kiểm tra:** `mvn test-compile` ✅. Grep không còn chỗ nào nhắc Redis, nhiều instance hay load balancer (ngoài phần ghi chú lịch sử trong file này).

---

### 2026-10-22 · Bước A3.2 — Use case
**Đã làm:** viết [docs/02-use-case.md](docs/02-use-case.md), gồm:
- **Danh sách 32 use case** (UC-01 → UC-32), chia theo tác nhân, mỗi use case ghi mã chức năng FR và mức ưu tiên.
- **5 sơ đồ use case**: tổng quát, Quản trị viên, Giáo viên (lớp học và bài tập), Giáo viên (theo dõi và chấm bài), Sinh viên. Có quan hệ `«include»` / `«extend»` theo đúng chiều mũi tên UML.
- **Đặc tả chi tiết 15 use case chính**: UC-01, 02, 11, 12, 13, 14, 16, 17, 18, 23, 24, 26, 29, 30, và 27/31.
  - Mỗi đặc tả có: tác nhân, tiền điều kiện, hậu điều kiện, luồng chính, luồng thay thế, ngoại lệ, quy tắc nghiệp vụ liên quan.
  - Riêng UC-12 (import qua TCP) có mô tả các khung tin `AUTH`, `IMPORT`, `PROGRESS`, `DONE`, `ERR`.
- Bảng mô tả ngắn cho 16 use case quản lý đơn giản còn lại.
- **Ma trận truy vết** giữa nhóm chức năng FR và use case.
- Đã xuất [docx/02-use-case.docx](docx/02-use-case.docx).

**Khác so với kế hoạch / bổ sung**
- UC-11 thêm luồng: file tài khoản chỉ tải được một lần, không tải thì phải đặt lại mật khẩu.
- UC-24 thêm ngoại lệ: chống tạo trùng version khi bấm nộp nhiều lần hoặc mạng chập chờn.
- UC-12 thêm: mỗi dòng import là một giao dịch riêng, nên client mất kết nối giữa chừng thì các dòng đã xử lý vẫn được giữ.

**Kiểm tra:** đã xem ảnh từng sơ đồ. Sơ đồ Giáo viên ban đầu bị rối nên tách thành 2 sơ đồ; sửa chiều mũi tên `«extend»` cho đúng UML.

---

### 2026-10-22 · Bước A3.3 — Sơ đồ tuần tự
**Đã làm:** viết [docs/03-sequence.md](docs/03-sequence.md) gồm **13 sơ đồ tuần tự** (SD-01 → SD-13):
- Đăng nhập + mở WebSocket (có chính sách một phiên).
- Tự refresh token.
- Import sinh viên qua HTTP và qua TCP.
- Phát hành bài + thông báo.
- Tự lưu nháp: mất mạng, xung đột.
- Nộp bài.
- Chấm và trả bài / yêu cầu sửa.
- Vòng đời WebSocket: heartbeat, mất kết nối, backoff, lấy thông báo bị lỡ.
- Job nhắc hạn.
- Đồng bộ giờ server.
- Chat.
- Chế độ thi.

Đã xuất [docx/03-sequence.docx](docx/03-sequence.docx).

**Quyết định thiết kế mới chốt trong file này** (file ERD/API và code phải làm theo)
- **Refresh token lưu dạng hash trong DB** để thu hồi được, có **xoay vòng**. Nếu một token đã thu hồi lại được dùng thì thu hồi toàn bộ phiên của user. → Cần thêm bảng `refresh_tokens` ở bước 2 (V1 chưa có).
- **Xác thực WebSocket** bằng header `Authorization` trong khung STOMP `CONNECT`, không đi qua bước HTTP Upgrade.
- **Mật khẩu tạm khi import không bao giờ lưu dạng rõ trong DB.** Danh sách tài khoản giữ trong bộ nhớ, tải được một lần qua `credentialsToken`, tự huỷ sau 15 phút.
- **Chống nộp trùng:** client gửi `expectedVersion`, server dùng `SELECT … FOR UPDATE`. Hai request đồng thời chỉ tạo đúng 1 version.
- **Chống ghi đè nháp (Mức 2):** dùng `draftVersion` (optimistic locking), lệch thì trả 409 kèm bản trên server. → Cột `submissions.draft_version` thêm ở migration Mức 2.
- **Thông báo luôn lưu DB trước rồi mới đẩy WebSocket.** Client kết nối lại thì gọi `GET /api/notifications?after=…` để lấy phần bị lỡ.
- **Job nhắc hạn chống gửi trùng** bằng `INSERT … ON CONFLICT DO NOTHING` trên `reminder_log`.
- **Giao thức TCP:**
  - các lệnh `AUTH`, `IMPORT <lớp> <số byte>`, `PROGRESS`, `DONE`, `ERRLINE`, `CRED`, `END`, `ERR <mã>`;
  - các trường trong `CRED` ngăn bằng `|`;
  - server có timeout đọc, mỗi kết nối chạy trên một luồng của thread pool.
- **Endpoint chấm bài** (chưa có trong plan):
  - `GET /api/teacher/submissions/{id}`;
  - `PUT …/{id}/grade`;
  - `POST …/{id}/return`;
  - `POST …/{id}/request-revision`.

**Kiểm tra:** xuất đủ 13 sơ đồ và đã xem ảnh từng sơ đồ.
- Ban đầu SD-04 lỗi vì dấu `;` trong nội dung tin nhắn (Mermaid hiểu `;` là ký tự kết thúc câu lệnh), đã sửa.
- Script xuất cũng được sửa để đọc thông báo lỗi của mermaid-cli bằng UTF-8.

---

### 2026-10-22 · Bước A3.4 → A3.7 — ERD, API, trạng thái, file Word gộp
**Đã làm**
- [docs/04-erd.md](docs/04-erd.md):
  - quy ước chung của schema;
  - **lộ trình migration V1 → V6** (mỗi migration gắn với một bước trong PLAN);
  - **5 sơ đồ ERD** theo nhóm: tài khoản và lớp, bài tập và rubric, bài nộp và chấm, thông báo/chat/nhật ký, mở rộng Mức 3;
  - mô tả **từng bảng và từng cột** (kiểu, ràng buộc, ý nghĩa, index);
  - câu SQL "sinh viên được giao bài";
  - bảng các dữ liệu **không** lưu trong DB.
- [docs/05-api.md](docs/05-api.md):
  - **REST**: quy ước chung, khung response và lỗi, **bảng mã lỗi**; khoảng 70 endpoint chia theo vai trò và Mức; JSON mẫu cho các DTO chính (LoginResponse, ImportReport, tạo bài tập, ma trận tiến độ, thống kê, màn hình chấm, body chấm điểm, NotificationDto); quy tắc các tab của sinh viên.
  - **WebSocket**: thông số kết nối, xác thực, heartbeat, kết nối lại; bảng **10 kênh** STOMP kèm payload và quyền; ví dụ khung tin.
  - **TCP**: thông số; các lệnh `AUTH`, `IMPORT`, `PING`, `QUIT`; các phản hồi; **9 mã lỗi**; máy trạng thái của một phiên; ví dụ một phiên đầy đủ; ghi chú cài đặt.
- [docs/06-trang-thai.md](docs/06-trang-thai.md): **8 sơ đồ trạng thái**:
  - bài tập, bài nộp, kết quả chấm, tài khoản, phiên đăng nhập;
  - kết nối WebSocket phía client, bản nháp phía client, phiên thi.
  - Mỗi sơ đồ có bảng điều kiện chuyển trạng thái và cách hiển thị trên giao diện.
- **File Word gộp** [docx/00-tai-lieu-phan-tich-thiet-ke.docx](docx/00-tai-lieu-phan-tich-thiet-ke.docx): 6 tài liệu, có mục lục, 33 sơ đồ.
- Script xuất có thêm `--merge` và **cache ảnh sơ đồ theo hash**: sơ đồ không đổi thì không render lại.

**Quyết định thiết kế mới**
- Gộp `refresh_tokens` và `login_sessions` thành **một bảng `user_sessions`**:
  - lưu hash của refresh token hiện tại và token trước đó (để phát hiện token bị dùng lại);
  - `revoke_reason` gồm LOGOUT / NEW_LOGIN / PASSWORD_CHANGED / ADMIN_FORCED / TOKEN_REUSE / ACCOUNT_LOCKED;
  - access token mang claim `sid` (mã phiên).
- Thêm cột `assignments.open_notified_at` (V4), dùng để gửi thông báo đúng lúc khi `open_at` ở tương lai.
- **Truy cập tài nguyên của giáo viên khác trả 404**, không phải 403, để không lộ việc tài nguyên đó tồn tại.
- `submission_versions` đánh dấu `@Immutable`; `word_count` do server đếm.
- Bài `CLOSED` **mở lại được** (về `PUBLISHED`).
- Bài nộp phải qua `GRADED` thì mới trả bài hoặc yêu cầu sửa được. Số lần đã yêu cầu sửa = `current_version − 1` (BR-10).
- Giao thức TCP:
  - thêm lệnh `PING`/`PONG` và `QUIT`/`BYE`, cờ `EMAIL` trong lệnh `IMPORT`;
  - giới hạn 1024 byte mỗi dòng lệnh, 5 MB cho phần CSV, timeout 30 giây, toàn phiên tối đa 5 phút;
  - thread pool 16 luồng, đầy thì trả `ERR 503`;
  - bản TLS dùng cổng 9443.
- WebSocket: `channelId` của chat có dạng `submission-{id}` hoặc `class-{id}`; sự kiện "đang gõ" gửi tối đa 1 lần mỗi 3 giây.

**Kiểm tra**
- Xem ảnh các sơ đồ.
- Sơ đồ trạng thái lúc đầu bị chồng chữ, đã sửa bằng nhãn ngắn và bảng điều kiện bên dưới.
- Sơ đồ trạng thái bài nộp có lỗi logic: trả bài thẳng từ `SUBMITTED` mà chưa chấm. Đã bỏ chuyển trạng thái đó.
- Chuyển file Word gộp ngược về text để kiểm tra: các ký tự `<…>` và `|` trong bảng còn nguyên. Không có `<…>` nào nằm ngoài code (pandoc sẽ hiểu nhầm là thẻ HTML và xoá mất).

---

### 2026-10-22 · Bước A4 — Design system
**Đã làm**
- [docs/07-design-system.md](docs/07-design-system.md), gồm:
  - 7 nguyên tắc thiết kế.
  - **Màu**:
    - brand xanh dương học thuật `#2457E6`, accent hổ phách;
    - 9 bậc màu trung tính;
    - 4 màu ngữ nghĩa (thành công, cảnh báo, lỗi, thông tin);
    - **màu cho 7 trạng thái bài nộp** (gồm `LATE` chỉ dùng để hiển thị) và **3 trạng thái bài tập**;
    - **thang 5 mức màu band IELTS**;
    - màu cho highlight nhận xét, diff giữa 2 version, trạng thái online.
  - **Chữ**:
    - font Roboto cho giao diện, **Noto Serif cho vùng bài làm**, JetBrains Mono cho mã;
    - 10 cỡ chữ, gồm cỡ `essay` 17/30.
  - **Khoảng cách**: lưới 4px; 3 mức bo góc, 3 mức đổ bóng.
  - **Bố cục**: breakpoint, khung trang, bố cục chi tiết màn **làm bài**, **chấm bài**, **ma trận tiến độ**, dashboard.
  - **Icon**: lucide, bảng icon theo ý nghĩa.
  - **Component**: quy cách 12 component nền AntD và **20 component nghiệp vụ** (StatusTag, BandBadge, ScorePicker, CriterionCard, EssayEditor/Viewer, CountdownTimer, AutosaveIndicator, ConnectionBanner, ProgressCell, VersionDiff, ChatPanel…).
  - Trạng thái tương tác, chuyển động, giọng văn, ánh xạ sang code.
- Code frontend:
  - [lib/theme/tokens.ts](frontend/lib/theme/tokens.ts): giá trị token cho JavaScript.
  - [lib/theme/status.ts](frontend/lib/theme/status.ts): map trạng thái → nhãn + màu, `getSubmissionDisplayStatus` (xử lý LATE, và ẩn GRADED với sinh viên), `getBandLevel`, `formatBand`.
  - [lib/theme/antdTheme.ts](frontend/lib/theme/antdTheme.ts): theme AntD, gắn vào `ConfigProvider` trong `providers.tsx`.
  - [app/globals.css](frontend/app/globals.css): `@theme static` của Tailwind với đủ token, nên dùng được các class như `bg-brand-600`, `text-essay`, `font-serif`, `animate-pulse-once`; có hỗ trợ `prefers-reduced-motion`.
  - `layout.tsx` nạp thêm font Noto Serif. Sidebar lấy kích thước từ token.
- Đã xuất [docx/07-design-system.docx](docx/07-design-system.docx) và gộp lại file Word tổng (7 tài liệu).

**Sửa lỗi phát hiện được**
- Ở bước A2, biến font `--font-sans` của next/font **trùng tên** với token font của Tailwind, gây tham chiếu vòng nên Roboto không được áp dụng. Đã đổi sang `--font-roboto`, `--font-noto-serif`, `--font-jetbrains-mono`.

**Kiểm tra:** `npm run lint` ✅ · `npm run build` ✅. Kiểm tra CSS sau build: đã có `--color-brand-600`, `--text-essay`, `--radius-lg`, `--font-sans: var(--font-roboto)…`.

---

### 2026-10-22 · Bước A5 — Prompt cho Figma Make
**Đã làm:** [design/figma-make-prompts.md](design/figma-make-prompts.md), gồm hướng dẫn dùng (tiếng Việt) và **11 prompt** (tiếng Anh, chữ trên giao diện để sẵn bằng tiếng Việt):
- **Prompt 0**: bối cảnh, toàn bộ token của design system (đặt làm Figma Variables), bố cục, **dữ liệu mẫu dùng chung** (giáo viên, lớp, 6 sinh viên, đề Task 1 và Task 2, đoạn bài mẫu).
- **Prompt 1**: trang thư viện **21 component** có biến thể.
- **Prompt 2 → 9** theo nhóm màn hình:
  - đăng nhập và khung app;
  - giáo viên: lớp học và import; trình tạo bài tập 4 bước; tiến độ realtime và thống kê; hàng đợi chấm và màn chấm bài;
  - sinh viên: danh sách bài, màn làm bài với 6 trạng thái, kết quả, so sánh version;
  - dashboard và chat;
  - quản trị viên.
- **Prompt 10**: các trạng thái chung (skeleton, 404 / 403 / 500), kiểm tra a11y và tính nhất quán.
- Checklist kiểm tra sau mỗi prompt, và yêu cầu để code dễ (tên layer và tên biến khớp với code).

**Ghi chú:** để ở `design/` chứ không ở `docs/`, để script gộp Word không đưa file này vào báo cáo.

---

### 2026-10-22 · Đổi thứ tự: làm backend trước + chuẩn bị DB + B0 coding rules backend
**Quyết định của người dùng:** làm **backend trước**, vì không phụ thuộc bản Figma. Frontend ghép sau khi có thiết kế.

**Thứ tự backend:**
- B0: coding rules backend.
- B1: chạy với DB thật.
- B2: xác thực.
- B3: lớp học và import.
- B4: bài tập.
- B5: làm bài và nộp bài.
- B6: chấm bài.
- B7: thông báo, WebSocket, nhắc hạn.
- B8: tiến độ và thống kê.
- B9+: phần mạng không cần giao diện (TCP + admin-cli, rate limit, audit).

Cách kiểm tra khi chưa có frontend: integration test + Swagger + file `.http`.

**Môi trường DB**
- Máy đã có **PostgreSQL 18.6** chạy dạng Windows service (`D:\PostgreSQL\18`, cổng 5432), nên **chưa cần Docker** để phát triển.
- Đã tạo role `ielts` / mật khẩu `ielts`, và 2 database: `ielts` (dev), `ielts_test` (test tự động). Mật khẩu superuser `postgres` không lưu vào file nào của dự án.

**B0 — coding rules backend**
- [backend/AI-CODING-RULES.md](backend/AI-CODING-RULES.md): 15 mục + checklist bàn giao. Gồm:
  - nguồn sự thật là `docs/`;
  - cấu trúc package theo module; quy định từng tầng Controller / Service / Repository;
  - DTO dạng record, validate bằng message tiếng Việt;
  - mã lỗi theo docs, tài nguyên của người khác trả 404;
  - phân quyền: vai trò ở controller, quyền sở hữu ở service;
  - không log dữ liệu nhạy cảm;
  - quy tắc entity: cấm `@Data`, không cascade của JPA;
  - Flyway: không sửa migration đã chạy;
  - **thời gian qua bean `Clock`**;
  - đồng thời: `FOR UPDATE`, `expectedVersion`, `ON CONFLICT`, mỗi dòng import một transaction;
  - **thông báo đẩy sau khi commit** (`@TransactionalEventListener`);
  - quy tắc cho module TCP;
  - test: mỗi BR-xx có ít nhất một test, ghi mã trong `@DisplayName`;
  - phong cách code.
- [backend/CLAUDE.md](backend/CLAUDE.md) (nạp file rules + các lệnh thường dùng) và [backend/AGENTS.md](backend/AGENTS.md).

**Quy định mới so với tài liệu:**
- bean `Clock` dùng chung, sẽ thêm ở B1;
- profile `test` chạy trên DB `ielts_test`, thay cho Testcontainers khi chưa có Docker;
- mỗi module có file `backend/http/<module>.http` để gọi thử API.

---

### 2026-10-22 · B1 — Chạy backend với PostgreSQL thật
**Đã làm**
- Thêm bean `Clock` ([config/TimeConfig.java](backend/src/main/java/com/ltmtu/ielts/config/TimeConfig.java)), là nguồn thời gian duy nhất theo coding rules.
- Profile `test` ([src/test/resources/application-test.yml](backend/src/test/resources/application-test.yml)) dùng DB `ielts_test`, thay cho Testcontainers (chưa có Docker).
  - `TestcontainersConfiguration` vẫn giữ lại, để dùng khi có Docker.
- Tắt health check của mail (`management.health.mail.enabled=false`), vì chưa có mail server nên nó làm `/actuator/health` báo DOWN.
- Sửa `GlobalExceptionHandler`: trước đây mọi lỗi Spring MVC đều thành **500**. Giờ có handler riêng:
  - đường dẫn không tồn tại → **404**;
  - sai method → **405**;
  - JSON hỏng, thiếu tham số, sai kiểu → **400**;
  - sai content-type → **415**.
- `ErrorResponse` luôn có `data: null`, đúng khung lỗi ở docs/05.
- Plugin `spring-boot-maven-plugin` ép `stdout/stderr` dùng UTF-8, để log tiếng Việt không lỗi font trên console Windows.
- Test:
  - `IeltsApplicationTests`: kiểm tra Flyway chạy đủ V1 và V2 không lỗi, admin được tạo, rubric mẫu có 2 bộ × 4 tiêu chí kèm checklist.
  - `ErrorResponseIntegrationTest`: kiểm tra health UP, đường dẫn lạ → 404 đúng khung lỗi.

**Kiểm tra**
- `mvn test`: **5/5 pass** trên `ielts_test`.
- Chạy `mvn spring-boot:run` trên DB `ielts`:
  - Flyway "Successfully applied 2 migrations", DB có 19 bảng + `flyway_schema_history`;
  - admin `admin` có hash BCrypt và `must_change_password = true`;
  - `GET /actuator/health` → `{"status":"UP"}`; Swagger UI → 200;
  - `POST` tới đường dẫn chưa có → 404 JSON đúng khung lỗi;
  - chạy lần 2: "Schema is up to date", không chạy lại migration.

**Chưa kiểm tra được:** chưa nhìn thấy dòng log tiếng Việt nào sau khi sửa encoding, vì lần chạy thứ 2 không sinh log tiếng Việt (admin đã có sẵn). Sẽ kiểm tra lại ở B2.

**Còn lại (sang B2)**
- Request thiếu token hiện trả **403 không có body**; phải là **401** JSON `UNAUTHORIZED`. Làm cùng JWT.
- Mật khẩu admin mặc định `Admin@123` (cấu hình trong `application.yml`). Khi triển khai thật phải đặt qua biến môi trường `ADMIN_PASSWORD`.

---

### 2026-10-22 · B2a — Xác thực (JWT, phiên đăng nhập, đổi mật khẩu)
**Đã làm**
- **Migration** [V3__user_sessions.sql](backend/src/main/resources/db/migration/V3__user_sessions.sql): bảng `user_sessions` có thêm cột `rotated_at` để tính thời gian ân hạn, và CHECK `revoked_at` đi đôi với `revoke_reason`.
- **Entity / repository**:
  - `UserSession`, `RevokeReason`, `UserSessionRepository`.
  - Truy vấn refresh có **khoá dòng** (`PESSIMISTIC_WRITE`).
  - `findAuthView` là truy vấn nhẹ dùng cho mỗi request.
  - `revokeAllOfUser` thu hồi mọi phiên, có thể chừa phiên hiện tại.
- **Gói `security`**:
  - `JwtService`: HS256; claim `sub`, `usr`, `role`, `sid`; dùng `Clock` của app; khoá bí mật dưới 32 byte thì không cho khởi động.
  - `TokenHasher`: refresh token 256 bit, lưu SHA-256.
  - `AuthPrincipal`.
  - `JwtAuthFilter`: ngoài kiểm tra JWT, còn kiểm tra **phiên chưa thu hồi và tài khoản còn hoạt động trong DB**; tài khoản đang bị bắt đổi mật khẩu chỉ gọi được `/api/auth/*`.
  - `SecurityErrorWriter`: lỗi 401/403 sinh trong filter chain cũng trả JSON đúng khung lỗi.
- `SecurityConfig`: phân quyền theo đường dẫn `/api/admin`, `/api/teacher`, `/api/student`; mở công khai login / refresh / logout / time / health / swagger.
- **`AuthService` + `AuthController`**: `POST /api/auth/login`, `/refresh`, `/logout`, `/change-password`, `GET /api/auth/me`. Thêm `GET /api/time` (`TimeController`).
  - Đăng nhập: sai tên hay sai mật khẩu đều trả cùng một lỗi. Khi không tìm thấy user vẫn chạy BCrypt với một hash giả, để thời gian phản hồi như nhau (chống dò tên đăng nhập).
  - Đổi mật khẩu: kiểm tra mật khẩu hiện tại; mật khẩu mới ≥ 8 ký tự, có chữ và số, khác mật khẩu cũ; xoá cờ bắt đổi; thu hồi các phiên khác.
- **Frontend** `axiosClient`: refresh lỗi mà tab khác đã lưu refresh token mới thì dùng token đó, không đăng xuất.
- File gọi thử [backend/http/auth.http](backend/http/auth.http).
- Cập nhật tài liệu:
  - `docs/05`: 6 mã lỗi mới; `change-password` trả `AccountInfo`; ghi chú kiểm tra phiên ở mỗi request.
  - `docs/03` SD-02: thời gian ân hạn.
  - `docs/04`: cột `rotated_at`, V3 ✅.

**Khác so với tài liệu / quyết định mới**
- **Thời gian ân hạn 30 giây** khi refresh token cũ bị gửi lại (`app.jwt.refresh-reuse-grace`): trả `401 REFRESH_TOKEN_ROTATED`, không thu hồi. Lý do: 2 tab cùng refresh một lúc là tình huống bình thường, không nên coi là bị đánh cắp và đăng xuất người dùng khỏi mọi nơi.
- Thêm mã lỗi `PASSWORD_CHANGE_REQUIRED` (403): bắt buộc đổi mật khẩu được chặn ở **server**, không chỉ ở frontend.
- Mỗi request có token sẽ truy vấn DB thêm 1 lần, để thu hồi phiên và khoá tài khoản có tác dụng ngay. Đã đánh đổi có chủ đích, chấp nhận được với quy mô đồ án.
- Chính sách một tài khoản một phiên (FR-06, Mức 2) **chưa bật**: đăng nhập nhiều nơi vẫn được.

**Kiểm tra**
- `mvn test`: **29/29 pass**.
  - `JwtServiceTest` (5 test): đọc lại token đúng, hết hạn, token bị sửa hoặc ký khoá khác, khoá ngắn, refresh token và hash.
  - `AuthIntegrationTest` (18 test): đăng nhập (thành công, sai thông tin cùng một lỗi, bị khoá, validate); access token (/me, thiếu hoặc rác → 401 JSON, hết hạn 30 phút, sai vai trò → 403 JSON, BR-02 chặn API khác, bị khoá sau khi đăng nhập → bị từ chối ngay); refresh (xoay vòng; dùng lại trong 30 giây → ROTATED và phiên còn; dùng lại sau 30 giây → mọi phiên bị thu hồi; quá 7 ngày); đăng xuất; đổi mật khẩu (sai mật khẩu hiện tại, mật khẩu yếu, thành công thì phiên khác bị thu hồi và phiên hiện tại vẫn dùng được).
  - Tạo test support: `MutableClock` để tua thời gian, `IntegrationTest` base, mỗi test dùng user riêng.
- **Chạy thật trên DB dev**:
  - Flyway áp V3.
  - curl: không token → 401 JSON; sai mật khẩu → 401 `BAD_CREDENTIALS`; admin đăng nhập → `mustChangePassword: true`; `/me` → 200; `/api/admin/teachers` → 403 `PASSWORD_CHANGE_REQUIRED`; logout → access token bị từ chối ngay.
  - Log tiếng Việt hiển thị đúng ("Đăng nhập: user 1 phiên 1…"). Đã xong phần còn lại của B1.

---

### 2026-10-22 · B2b — Admin quản lý giáo viên, người dùng, thống kê hệ thống
**Đã làm**
- `AdminUserService` + `AdminUserController` (`/api/admin`, `@PreAuthorize("hasRole('ADMIN')")`):
  - `GET/POST /teachers`, `PUT /teachers/{id}`;
  - `GET /users`, `GET /users/{id}`;
  - `POST /users/{id}/lock`, `/unlock`, `/reset-password`.
- **Quy tắc đã cài**:
  - Tạo giáo viên và đặt lại mật khẩu đều sinh **mật khẩu tạm** và bắt đổi ở lần đăng nhập sau (BR-02).
  - **Khoá** tài khoản: thu hồi mọi phiên ngay, token đang dùng bị từ chối, đăng nhập lại → 403 `ACCOUNT_LOCKED`.
  - **Đặt lại mật khẩu**: thu hồi mọi phiên (`ADMIN_FORCED`).
  - Admin **không tự khoá được mình** (409).
  - Trùng tên đăng nhập → 409 `DUPLICATE`. Sửa id không phải giáo viên qua `/teachers/{id}` → 404.
- `PasswordGenerator` ([security/PasswordGenerator.java](backend/src/main/java/com/ltmtu/ielts/security/PasswordGenerator.java)): 8 ký tự, luôn có chữ và số, **bỏ các ký tự dễ nhầm** (0/O, 1/l/I) vì mật khẩu được phát qua file hoặc email. Sẽ dùng lại khi import sinh viên ở B3.
- `Pageables` (common):
  - chỉ cho sắp xếp theo whitelist trường (tránh lỗi 500 và lộ tên trường nội bộ);
  - chuẩn hoá từ khoá thành `%…%`, có escape `% _ \`.
- Cấu hình phân trang chung: mặc định 20, tối đa 100.
- `UserRepository.search`: lọc theo từ khoá (tên đăng nhập, họ tên, mã SV, email), vai trò, trạng thái.
- **Thống kê hệ thống** (`stats/SystemStatsService`, `GET /api/admin/stats`, UC-09):
  - số người dùng theo vai trò, số bị khoá, số lớp, số bài tập theo trạng thái, số bài nộp hôm nay, **biểu đồ 14 ngày**, số phiên đang hoạt động;
  - SQL tổng hợp qua `JdbcTemplate`;
  - "ngày" tính theo **giờ Việt Nam** (thêm cấu hình `app.zone = Asia/Ho_Chi_Minh`).
- File gọi thử [backend/http/admin.http](backend/http/admin.http). Cập nhật `docs/05` mục A.3: các quy tắc riêng, dạng `UserSummary`, dạng kết quả thống kê.

**Khác so với kế hoạch**
- Cột **"Số lớp"** trong danh sách giáo viên để sang B3, khi có entity lớp học.
- Thêm quy tắc không được tự khoá chính mình.

**Kiểm tra:** `mvn test`: **241 lượt test pass**, gồm:
- 200 lượt lặp của test định dạng mật khẩu tạm;
- `AdminUserIntegrationTest` (11 test): tạo giáo viên rồi đăng nhập bằng mật khẩu tạm; trùng tên / sai định dạng; sửa và 404; tra cứu + phân trang + `totalRecord`; sắp xếp sai trường → 400; khoá (token bị từ chối ngay, đăng nhập 403) rồi mở khoá; không tự khoá được; đặt lại mật khẩu (mật khẩu cũ và phiên cũ hết hiệu lực); chi tiết kèm số phiên; thống kê 14 ngày; người không phải admin → 403.

**Chưa làm (để sau theo kế hoạch):** gửi `FORCE_LOGOUT` qua WebSocket khi khoá tài khoản (B7); audit log (Mức 2); quản lý phiên đăng nhập (Mức 3).

---

### 2026-10-22 · B3a — Lớp học và thành viên
**Đã làm**
- Entity `Classroom`, `ClassMember` (khoá kép `@EmbeddedId` + `@MapsId`). Repository `ClassroomRepository`, `ClassMemberRepository`:
  - danh sách lớp kèm `memberCount` (subquery, có `countQuery` riêng cho phân trang);
  - danh sách thành viên dạng projection DTO.
- `ClassroomService` + `TeacherClassroomController` (`/api/teacher`, chỉ TEACHER):
  - CRUD lớp `/classes`;
  - thành viên `/classes/{id}/members` (xem, thêm theo mã SV, xoá);
  - tìm sinh viên để giao bài `/students/search`.
- **Quy tắc đã cài:**
  - **BR-15**: chỉ thao tác được với lớp của mình; lớp của người khác → 404 ở mọi endpoint.
  - Mã lớp lưu **viết hoa**, trùng mã không phân biệt hoa thường → 409.
  - Thêm thành viên chỉ nhận tài khoản STUDENT đã tồn tại; báo riêng `added`, `alreadyMember`, `notFound`.
  - Xoá lớp không xoá tài khoản sinh viên.
- `UserQueryService` (user): cửa duy nhất để module khác tra cứu người dùng, đúng quy tắc không gọi chéo repository.
- **Cột "Số lớp"** (`classCount`) trong danh sách giáo viên của admin (tồn đọng từ B2b); người dùng khác vai trò có `classCount = null`.
- `Pageables.sanitize` hỗ trợ ánh xạ tên trường sắp xếp sang đường dẫn thuộc tính (vd `fullName` → `student.fullName`).
- File [backend/http/classroom.http](backend/http/classroom.http). Cập nhật `docs/05` mục A.4.

**Lỗi phát hiện và đã sửa:** tìm kiếm với từ khoá có `_` hoặc `%` (vd tên đăng nhập `teacher_ab12`) **không ra kết quả**.
- Nguyên nhân: `likePattern` escape bằng `\`, nhưng query JPQL không có mệnh đề `ESCAPE`.
- Sửa: đổi ký tự escape sang `!` và thêm `ESCAPE '!'` vào mọi `LIKE` (user, lớp, thành viên).
- Thêm test riêng; thêm quy tắc vào [backend/AI-CODING-RULES.md](backend/AI-CODING-RULES.md) mục 7.

**Kiểm tra:** `mvn test`: **251 lượt pass**. `ClassroomIntegrationTest` (10 test):
- tạo lớp, viết hoa mã, trùng mã; validate;
- **quyền sở hữu** (không thấy lớp người khác; xem, sửa, xoá, xem thành viên của người khác → 404);
- sửa, tìm, xoá;
- thêm thành viên (đã có, không tồn tại, không phải sinh viên);
- danh sách thành viên: tìm, sắp xếp, xoá; xoá người không thuộc lớp → 404;
- tìm sinh viên chỉ trả STUDENT;
- admin thấy `classCount`;
- ký tự `_` và `%` được tìm theo nghĩa đen;
- sinh viên gọi API của giáo viên → 403.

**Để sau:** số "bài tập đang mở" của mỗi lớp (B4), chấm online của thành viên (B9, Mức 2).

---

### 2026-10-22 · B3b — Import sinh viên từ file, file tài khoản, email
**Đã làm**
- **`StudentFileParser`** (lớp thuần, dùng lại được cho TCP ở B9): đọc `.xlsx` (Apache POI) và `.csv` (Commons CSV).
  - Tên cột không phân biệt hoa thường, dấu, khoảng trắng (`ma_sv`, `MSSV`, `Mã sinh viên`…).
  - CSV có BOM, tự nhận dấu phân cách `, ; tab`.
  - Mã SV dạng số trong Excel đọc đúng (`DataFormatter`).
  - Bỏ dòng trống. Số dòng tính như Excel.
  - Giới hạn 2000 dòng. Lỗi cả file → `400 BAD_FILE`.
- **`StudentImportService`** (UC-11, SD-03):
  - Kiểm tra từng dòng: mã 4–30 ký tự chữ và số; họ tên 2–150; email; trùng mã trong file.
  - **Mỗi dòng một transaction riêng** (`TransactionTemplate` + `REQUIRES_NEW`, BR-03).
  - Kết quả mỗi dòng (tạo mới / đã có / đã trong lớp / lỗi) chỉ được cộng vào báo cáo và danh sách tài khoản **sau khi dòng đó commit**.
  - Có callback tiến độ cho TCP. Hai import cùng tạo một mã thì bắt lỗi UNIQUE, báo lỗi dòng.
- `StudentAccountService` (user): tra cứu mã SV và tạo tài khoản sinh viên (BR-01, BR-02). Phát hiện **mã SV trùng tên đăng nhập của tài khoản khác** → lỗi dòng, không ghi đè.
- **`ImportCredentialStore`**: mật khẩu tạm chỉ ở **trong bộ nhớ**, lấy được **một lần**, chỉ bởi đúng giáo viên đã import, **hết hạn sau 15 phút** (job dọn mỗi phút). Không ghi DB hay log.
- `ImportWorkbooks`: file tài khoản `.xlsx`; file mẫu `.xlsx` (cột mã SV định dạng chữ) và `.csv` (có BOM).
- **`MailService`** (notification): gửi email tài khoản `@Async`, lỗi chỉ ghi log (E3). Chỉ gửi cho tài khoản **mới** có email.
- API:
  - `POST /api/teacher/classes/{id}/import` (multipart `file`, `sendEmail`);
  - `GET /api/teacher/imports/{token}/credentials` (`Cache-Control: no-store`);
  - `GET /api/teacher/import-template?format=xlsx|csv`.
- **Không phân biệt hoa thường**: mã SV lưu viết hoa; đăng nhập, tra mã SV và thêm thành viên theo mã đều không phân biệt hoa thường; tạo giáo viên kiểm tra trùng tên cũng vậy.
- Công cụ: **Mailpit v1.31.6** tải từ GitHub release vào `tools/mailpit/` (gitignore). Hướng dẫn chạy ghi trong `backend/CLAUDE.md`.
- Cập nhật `docs/05` (quy tắc file import) và `backend/http/classroom.http` (import, tải file tài khoản, file mẫu).

**Lỗi phát hiện và đã sửa**
1. **Import tạo trùng tài khoản**: tra mã SV phân biệt hoa thường, nên sinh viên đã có với mã chữ thường bị coi là chưa có. → mọi tra cứu mã SV không phân biệt hoa thường.
2. Thiết kế ban đầu cộng số liệu **bên trong** transaction của dòng; commit lỗi thì báo cáo và file tài khoản sẽ sai. → tách thành `RowOutcome`, chỉ cộng sau khi commit.
3. **Email chậm ~5 giây / thư trên Windows**: JavaMail gọi `InetAddress.getLocalHost()` khi tạo Message-ID. → đặt `mail.from` và `mail.smtp.localhost`, thêm timeout. Sau khi sửa, email gửi **60 ms** sau khi import.

**Kiểm tra**
- `mvn test`: **269 lượt pass**.
  - `StudentFileParserTest` (7 test): tên cột, CSV có BOM và `;`, thứ tự cột, ngoặc kép, Excel mã dạng số, các loại file lỗi, quá 2000 dòng, kiểm tra từng dòng.
  - `StudentImportIntegrationTest` (11 test): file đủ loại dòng (tạo, đã có khác hoa thường, đã trong lớp, mã sai, trùng trong file, trùng tên đăng nhập) và tài khoản cũ không bị ghi đè; tải file tài khoản rồi **đăng nhập bằng mật khẩu tạm**, tải lần 2 → 404; giáo viên khác không tải được, quá 15 phút → hết hạn; không có tài khoản mới → không có token; email (chỉ sinh viên mới, có mã + mật khẩu); upload `.xlsx`; file lỗi; lớp người khác → 404; file mẫu import lại được; đăng nhập không phân biệt hoa thường.
  - Test support: `CapturingMailSender` giữ email trong bộ nhớ.
- **Chạy thật** (backend dev + Mailpit, script Python): admin đổi mật khẩu → tạo giáo viên → giáo viên đăng nhập bằng mật khẩu tạm và đổi mật khẩu → tạo lớp → import CSV (BOM, `;`, 1 dòng lỗi) → báo cáo đúng → tải file tài khoản (lần 2 → 404) → **email tới Mailpit** đúng tiêu đề và nội dung → sinh viên **đăng nhập bằng mật khẩu trong email** (gõ mã chữ thường) → bị bắt đổi mật khẩu.

**Thay đổi dữ liệu dev:** mật khẩu admin trên DB `ielts` đổi từ `Admin@123` → **`Admin@2026`**. DB dev cũng có thêm vài giáo viên `gv.demo…`, lớp `DEMO-…` và sinh viên từ lần chạy thử.

---

### 2026-10-22 · Nghiên cứu nghiệp vụ chấm IELTS Writing và phiếu chấm điện tử (trước B4)
**Lý do:** người dùng hỏi đã nghiên cứu nghiệp vụ chấm IELTS (chấm theo checklist) và cách làm phiếu chấm điện tử có nhận xét chưa. Trước đó thiết kế chỉ dựa trên hiểu biết chung, chưa đối chiếu tài liệu chính thức.

**Nguồn đã đọc:**
- **Bản PDF chính thức của ielts.org**: *Writing Band Descriptors (Updated May 2023)*, 9 trang; *Writing Key Assessment Criteria*, 4 trang.
- Hướng dẫn Turnitin Feedback Studio (QuickMarks, rubric, gắn nhận xét với tiêu chí) và Moodle Marking guide (frequently used comments).
- Tài liệu về mã sửa lỗi ESL, quy ước đếm từ.

**Kết quả:** [docs/08-nghiep-vu-cham-ielts.md](docs/08-nghiep-vu-cham-ielts.md). Phát hiện **3 chỗ thiết kế cũ sai so với IELTS**:
1. Điểm tiêu chí bước 0.5 → **IELTS chấm nguyên band 0–9** cho từng tiêu chí. Mặc định đổi sang bước 1; giáo viên vẫn được chọn 0.5 cho bài luyện tập.
2. Band bài = trung bình các phần → **IELTS: Task 2 có trọng số lớn hơn Task 1**. Dùng tỉ lệ 2:1 theo thông lệ (IELTS không công bố công thức), chỉ làm tròn khi hiển thị, và ghi rõ là ước lượng.
3. Checklist chỉ có mục "đạt / chưa đạt" → **bảng mô tả IELTS có các đặc điểm in đậm "giới hạn band"**. Thêm loại mục **Giới hạn điểm** (kèm band tối đa): tick vào thì ô chọn điểm bị khoá các mức cao hơn. Đã liệt kê 11 đặc điểm giới hạn lấy từ bảng mô tả chính thức.

**Bổ sung từ quy định chính thức:**
- bài ≤ 20 từ → Band 1;
- Band 0 chỉ khi không làm bài, không phải tiếng Anh, hoặc học thuộc (bắt buộc ghi lý do);
- phần chép lại đề không tính;
- quy ước đếm từ (dùng chung cho client và server).

**Bổ sung từ cách chấm điện tử (Turnitin / Moodle) + nghiên cứu sư phạm:**
- nhận xét inline có **mã lỗi** (22 mã mặc định, gắn với tiêu chí) và **gợi ý sửa**;
- **ngân hàng nhận xét** cho từng giáo viên;
- **bảng mô tả band thu gọn** cạnh từng tiêu chí;
- **hồ sơ lỗi** của sinh viên;
- gợi ý sửa **ẩn khi bài đang "Cần sửa"**, để sinh viên tự sửa theo mã lỗi. Lý do: nghiên cứu cho thấy phản hồi bằng mã lỗi có tác dụng lâu dài tốt hơn sửa trực tiếp.

**Tài liệu đã cập nhật:**
- `docs/01`: sửa BR-11, BR-12; thêm **BR-16 → BR-22**; sửa FR-41 → FR-43; thêm **FR-47 → FR-49**.
- `docs/04`: lộ trình migration đánh số lại, V4 = bài tập + rubric, V5 = phản hồi chấm, V6 = Mức 2, V7 = Mức 3.
- `docs/07`: thêm 6 component.
- `design/figma-make-prompts.md`: thêm **Prompt 6b**.
- Đã xuất Word cho 08, 01, 04, 07 và gộp lại file tổng (8 tài liệu).

**Bản quyền:** không chép nguyên văn bảng mô tả band. Chỉ dùng bản diễn giải tiếng Việt, kèm link PDF chính thức.

**Việc người dùng cần làm:** dán thêm **Prompt 6b** vào Figma Make để cập nhật màn chấm.

---

### 2026-10-22 · B4a — Soạn bài tập, rubric theo nghiệp vụ IELTS
**Đã làm**
- **Migration** [V4__assignment_rubric.sql](backend/src/main/resources/db/migration/V4__assignment_rubric.sql), theo tài liệu 08:
  - `assignments.open_notified_at`, `show_suggestions_on_revision`;
  - `assignment_parts.weight`;
  - `code` cho tiêu chí (TA / TR / CC / LR / GRA);
  - checklist `kind` (CHECK / LIMIT) + `max_band`, có ràng buộc: LIMIT ⇔ có max_band;
  - rubric mẫu hệ thống chuyển **`step = 1`**, gắn mã, và thêm **16 mục "Giới hạn điểm"** lấy từ bảng mô tả chính thức.
- **Bảng mô tả band thu gọn** [resources/ielts/band-descriptors-vi.json](backend/src/main/resources/ielts/band-descriptors-vi.json): 5 tiêu chí × 10 mức, **diễn giải tiếng Việt** (không chép nguyên văn), kèm link PDF ielts.org. Phục vụ qua `BandDescriptorService`.
- **Entity**:
  - `Assignment` là gốc aggregate: `AssignmentPart` → `RubricCriterion` → `ChecklistItem`;
  - `RubricTemplate` (kèm `Criterion`, `Item`);
  - `AssignmentEnums` (trạng thái, loại phần, mã tiêu chí, loại checklist). Trọng số mặc định Task 2 = 2.
- **`RubricRules`**:
  - bước điểm chỉ 1 hoặc 0.5; điểm tối đa là bội số của bước;
  - mục LIMIT bắt buộc có band tối đa hợp lệ;
  - không lặp mã tiêu chí trong một phần;
  - `openAt` < `dueAt`; link ảnh chỉ nhận `/uploads/` hoặc http(s).
- **`HtmlSanitizer`** (jsoup 1.18.3): lọc HTML đề bài, bỏ `<script>` và thuộc tính sự kiện (chống XSS). Đề rỗng sau khi lọc → 400.
- **`AssignmentService` + `TeacherAssignmentController`** (`/api/teacher`):
  - `GET /rubric-templates`, `GET /rubric-templates/descriptors`;
  - `GET/POST /assignments`, `GET/PUT/DELETE /assignments/{id}`;
  - `PATCH /assignments/{id}/settings`.
- Cập nhật tài liệu:
  - `docs/05` mục A.5: PUT chỉ cho bài nháp, thêm PATCH settings, mẫu JSON mới (checklist dạng object), quy tắc kiểm tra khi lưu;
  - `docs/04`: V4 ✅;
  - `backend/AI-CODING-RULES.md`: ngoại lệ cascade cho aggregate, quy tắc request DTO không dùng kiểu nguyên thuỷ.
  - Thêm file [backend/http/assignment.http](backend/http/assignment.http).

**Khác so với tài liệu**
- Tách **PATCH `/settings`** (sửa bài ở mọi trạng thái) khỏi **PUT** (chỉ thay bài nháp), thay vì một PUT xử lý hai kiểu.
- Bài nháp được phép thiếu phần hoặc tiêu chí. Kiểm tra "đủ để phát hành" làm ở B4b.

**Lỗi phát hiện và đã sửa**
1. **Jackson 3** (Spring Boot 4) trả 400 khi request **thiếu trường `boolean` nguyên thuỷ** trong record (Jackson 2 tự cho `false`). → đổi sang `Boolean`; thêm log debug cho lỗi 400 do sai định dạng; ghi quy tắc vào coding rules.
2. Đề phòng trước: thay toàn bộ danh sách phần trong một lần flush sẽ vi phạm UNIQUE `(assignment_id, order_no)`, vì Hibernate chạy INSERT trước DELETE. → `clear()` + flush trước khi thêm mới; có test đổi số phần và thứ tự.

**Kiểm tra:** `mvn test`: **282 lượt pass**. `AssignmentIntegrationTest` (13 test):
- rubric mẫu Task 2 có mã đúng, bước 1, mục "Không chia đoạn" là LIMIT tối đa 5;
- Task 1 có mục giới hạn "tổng quan";
- bảng mô tả Task 2 (không có TA, đủ mức, link ielts.org);
- tạo bài 2 phần từ rubric mẫu: Task 2 trọng số 2, mục CHECK bỏ `maxBand`;
- lọc XSS;
- 5 kiểu vi phạm rubric, và bước 0.5 cho phép band 4.5;
- 4 kiểu dữ liệu sai khác;
- thay toàn bộ bài nháp với số phần và thứ tự khác;
- bài đã phát hành: PUT / DELETE → 409, PATCH được, phần không thuộc bài → 400;
- xoá nháp;
- quyền sở hữu (4 endpoint → 404, không có trong danh sách);
- danh sách lọc theo trạng thái và từ khoá;
- sinh viên → 403.

---

### 2026-10-22 · B4b — Phát hành, giao bài, vòng đời bài tập, upload ảnh
**Đã làm**
- Entity `AssignmentTarget` (đúng 1 lớp hoặc 1 sinh viên) + `AssignmentTargetRepository`:
  - **`findRecipientIds`**: người nhận = giao trực tiếp ∪ thành viên lớp, loại trùng, tính tại thời điểm truy vấn.
- **`AssignmentRepository.findVisibleForStudent`** (danh sách và theo id), điều kiện viết một lần trong hằng `VISIBLE_TO_STUDENT`:
  - **BR-05**: PUBLISHED hoặc CLOSED, và đã tới `openAt`;
  - **BR-04**: được giao trực tiếp, hoặc là thành viên lớp được giao.
  - Đây là cửa duy nhất cho mọi API phía sinh viên ở B5.
- **`PublishService`**:
  - **phát hành**: kiểm tra đủ điều kiện, liệt kê **mọi** vấn đề cùng lúc (`400 NOT_READY`); không có người nhận → rollback cả các target vừa thêm;
  - **giao thêm** (chỉ người mới được tính);
  - xem danh sách đã giao;
  - **đóng / mở lại**;
  - **nhân bản** (đủ phần + rubric, không chép người nhận và mốc thời gian).
- **Sự kiện** `AssignmentEvents.Published` / `RecipientsAdded`, có cờ `opensNow` (false khi `openAt` ở tương lai, để job gửi thông báo đúng lúc mở). B7 sẽ nghe sau khi commit.
- `ClassroomService.requireOwnedClassrooms`, `UserQueryService.requireStudents`: kiểm tra lớp và sinh viên khi giao bài, qua service của module khác.
- **Upload ảnh đề** (`FileStorageService`, `FileController`, `WebConfig`):
  - nhận diện PNG / JPG / WEBP theo **magic bytes**, tối đa 5 MB;
  - lưu vào `uploads/yyyy/MM/<uuid>.<đuôi>`;
  - `GET /uploads/**` công khai, cache 30 ngày.
- `ClassroomSummary.openAssignmentCount` (tồn đọng từ B3a).
- Endpoint mới trong `TeacherAssignmentController`: publish, targets (GET / PUT), close, reopen, duplicate.
- Cập nhật `docs/05` (mục A.5 + mã lỗi `NOT_READY`) và `backend/http/assignment.http`.

**Quyết định**
- Giao thêm chỉ **thêm**, không gỡ người đã giao (gỡ sau khi sinh viên đã làm bài sẽ gây mất dữ liệu).
- Nhân bản **không** chép các mốc thời gian, vì thường đã ở quá khứ.
- Phát hành yêu cầu có hạn nộp ở tương lai.

**Kiểm tra:** `mvn test`: **292 lượt pass**. `PublishIntegrationTest` (10 test):
- giao lớp + cá nhân trùng → người nhận loại trùng (3), sự kiện có đủ người nhận, xem targets, phát hành lần 2 → 409;
- chưa đủ điều kiện → liệt kê nhiều vấn đề cùng lúc;
- lớp rỗng → 400 và **rollback** target;
- lớp của người khác / tài khoản không phải sinh viên → 400;
- **bài sinh viên được thấy**: nháp không thấy, chưa tới giờ mở không thấy, người ngoài không thấy, vào lớp sau vẫn thấy, tới giờ mở thì thấy, bài đã đóng vẫn thấy;
- mở trong tương lai → `opensNow = false`;
- giao thêm chỉ người mới, nháp / đã đóng → 409;
- đóng / mở lại và số bài đang mở của lớp;
- nhân bản giữ mục LIMIT, không có người nhận;
- upload PNG thật và xem công khai; file giả mạo đuôi → 400; sinh viên → 403.

**Ghi chú:** chưa có API phía sinh viên (làm ở B5), nên phần "bài được thấy" được test trực tiếp qua repository.

---

### 2026-10-22 · B5a — Đếm từ IELTS, lưu nháp, nộp bài, lịch sử version
**Đã làm**
- **Đếm từ theo quy ước IELTS (BR-20)**:
  - backend [submission/WordCounter.java](backend/src/main/java/com/ltmtu/ielts/submission/WordCounter.java);
  - frontend [lib/utils/wordCount.ts](frontend/lib/utils/wordCount.ts);
  - **bộ ca kiểm thử chung** [shared/word-count-cases.json](shared/word-count-cases.json) (14 ca: gạch ngang, viết tắt, số %, dấu câu đứng riêng, viết dính, NBSP, chữ có dấu…) để cả hai phía chắc chắn đếm giống nhau.
  - Frontend chạy test bằng `node --test` có sẵn trong Node (`npm run test:unit`), không cần cài thêm thư viện; bật `allowImportingTsExtensions` trong tsconfig.
- Entity `Submission`, `SubmissionVersion` (**`@Immutable`**, NFR-11), `SubmissionStatus` (`isOpenForStudent`, `forStudent()`: GRADED hiển thị như SUBMITTED, BR-13).
- **`DeadlineRules`**: hạn áp dụng **BR-07** (hạn sửa → hạn phần → hạn bài), quá hạn và được nộp muộn **BR-08**, theo giờ server.
- **`StudentAssignmentAccess`** (assignment): cửa duy nhất để module khác lấy bài / phần bài phía sinh viên, luôn qua điều kiện BR-04 + BR-05, không được giao → 404.
- **`StudentSubmissionService` + `StudentSubmissionController`** (`/api/student/parts/{partId}`):
  - `GET/PUT /draft`;
  - `POST /submit`;
  - `GET /versions`, `GET /versions/{n}`.
  - **Chống nộp trùng**: `SELECT … FOR UPDATE` + `expectedVersion`; lần nộp đầu chạy đồng thời thì UNIQUE (part, student) chặn → 409 thay vì 500.
  - Đang "Cần sửa" mà chưa có nháp thì nạp sẵn version mới nhất (UC-26 bước 2).
- **Sự kiện** `SubmissionEvents.Submitted` (có id giáo viên) để B7 báo giáo viên và cập nhật tiến độ live.
- File [backend/http/student.http](backend/http/student.http). Cập nhật `docs/05` mục A.8 (dạng dữ liệu, bảng lỗi nộp bài).

**Lỗi phát hiện và đã sửa:** **thời gian lệch nano giây ↔ micro giây**. Java `Instant` có nano giây nhưng PostgreSQL chỉ lưu micro giây, nên `savedAt` trả về ngay sau khi lưu khác giá trị đọc lại (…724673700Z so với …724674Z). → bean `Clock` nhảy theo micro giây (test clock cũng vậy); ghi thành quy tắc trong coding rules mục 9.

**Kiểm tra**
- Backend `mvn test`: **316 lượt pass**.
  - `WordCounterTest` (14 ca chung).
  - `StudentSubmissionIntegrationTest` (10 test): lưu và nạp nháp; nộp từng phần độc lập (server đếm từ, version lưu đúng nội dung, xoá nháp, sự kiện); đã nộp thì không nộp lại / lưu nháp được, `expectedVersion` sai → 409; **2 request nộp đồng thời → đúng 1 thành công, 1 version**; **2 lần nộp đầu tiên đồng thời** → 1 thành công; quá hạn không cho muộn → 422 (cả lưu nháp); cho muộn → đánh dấu muộn; hạn riêng của phần được ưu tiên; không được giao → 404, giáo viên → 403, bài đã đóng → 409 nhưng vẫn xem được; validate.
- Frontend `npm run test:unit`: **14/14 pass** (cùng bộ ca). `npm run lint` và `npm run build` không lỗi.

---

### 2026-10-22 · B5b — Danh sách bài được giao theo tab, chi tiết bài phía sinh viên
**Đã làm**
- `StudentAssignmentService` + `StudentAssignmentController`:
  - `GET /api/student/assignments?tab` trả `counts` cho cả 5 tab (dùng cho badge), danh sách đã lọc, `serverTime`;
  - `GET /api/student/assignments/{id}`.
- **Quy tắc tab** (`tabs()`; một bài có thể thuộc nhiều tab):
  - TODO = bài đang mở, có phần chưa làm hoặc đang làm, chưa quá hạn;
  - DUE_SOON = như TODO nhưng hết hạn trong 24 giờ;
  - OVERDUE = quá hạn (kể cả khi được nộp muộn) hoặc bài đã đóng mà còn phần chưa nộp;
  - REVISION = có phần "Cần sửa";
  - SUBMITTED = không còn phần nào phải làm.
- Trạng thái từng phần:
  - `status` phía sinh viên (GRADED → SUBMITTED, BR-13);
  - `late` của lần nộp gần nhất;
  - `effectiveDueAt` (BR-07);
  - `canSubmit`.
  - Chi tiết có đề, ảnh, số từ, thời gian gợi ý, lời nhắn và hạn sửa, **rubric + checklist** để sinh viên biết tiêu chí.
- Tránh N+1: một truy vấn cho mọi bài làm của sinh viên + một truy vấn lấy **version mới nhất** (`SubmissionVersionRepository.findLatest`).
- Sắp xếp theo hạn gần nhất của phần còn phải làm.
- Cập nhật `docs/05` mục A.8 (dạng dữ liệu, quy tắc tab viết lại cho chính xác) và `backend/http/student.http`.

**Kiểm tra:** `mvn test`: **321 lượt pass**. `StudentAssignmentIntegrationTest` (5 test):
- **7 bài ở đủ các tình huống**: còn 3 ngày, còn 5 giờ, quá hạn có cho muộn, đã nộp, 2 phần mới nộp 1, cần sửa, đã đóng mà chưa nộp. Kiểm tra **số đếm từng tab**, lọc từng tab, số đếm không đổi khi lọc, thứ tự sắp xếp;
- GRADED hiển thị như SUBMITTED, cờ muộn, `canSubmit`;
- chi tiết: hạn sửa được ưu tiên, lời nhắn sửa, rubric có mục LIMIT, giờ server;
- không được giao → không có trong danh sách, chi tiết → 404;
- tab sai → 400.

---

### 2026-10-22 · B6a — Migration V5, cách tính band, mã lỗi, ngân hàng nhận xét
B6 chia 3 phần:
- **B6a** (bước này): nền dữ liệu và quy tắc.
- **B6b**: hàng đợi chấm, màn chấm, lưu chấm, trả bài, yêu cầu sửa.
- **B6c**: sinh viên xem kết quả, band cả bài, số liệu trong danh sách bài của giáo viên.

**Đã làm**
- **Migration [V5__grading_feedback.sql](backend/src/main/resources/db/migration/V5__grading_feedback.sql)**:
  - bảng `error_codes`, seed **22 mã hệ thống** theo docs/08 mục 4.3;
  - bảng `comment_bank`, seed **20 nhận xét mẫu** (5 cho mỗi nhóm tiêu chí, tự viết);
  - `inline_comments` thêm `error_code`, `criterion_id`, `suggestion`; bỏ NOT NULL ở `comment`; CHECK phải có ít nhất một trong ba (BR-21);
  - `grades.zero_reason` (BR-19).
- **Entity** trong `grading/`:
  - `Grade` là gốc aggregate, gồm `GradeCriterionScore`, `GradeChecklistMark` (khoá kép `@EmbeddedId` + `@MapsId`) và `InlineComment`;
  - `ErrorCode`, `CommentBankItem`;
  - `GradingEnums`: `GradeStatus`, `ZeroReason`, `CriterionGroup`.
  - Viết entity ngay từ bước này để `ddl-auto=validate` kiểm tra luôn schema V5.
- **[BandCalculator](backend/src/main/java/com/ltmtu/ielts/grading/BandCalculator.java)** (BR-12, class thuần):
  - band phần = trung bình có trọng số các tiêu chí;
  - band bài = trung bình có trọng số band các phần;
  - giữ 6 chữ số thập phân khi tính;
  - `display()` làm tròn theo quy ước .25 / .75;
  - thiếu điểm → `null` ("Chưa đủ phần").
- **[GradingRules](backend/src/main/java/com/ltmtu/ielts/grading/GradingRules.java)** (class thuần):
  - `cap()`: điểm tối đa của một tiêu chí = `max_band` nhỏ nhất trong các mục LIMIT đang tick (BR-17), và ≤ 1 khi bài ≤ 20 từ (BR-18). Trả kèm **lý do** để hiển thị;
  - `requireValidScore()`: điểm phải trong [0, max] và là bội số của bước (BR-11), vượt giới hạn → `422 SCORE_LIMITED` (message nêu lý do).
  - B6b sẽ gọi hai hàm này khi lưu chấm.
- **API mã lỗi và ngân hàng nhận xét** (`FeedbackLibraryService`, `TeacherFeedbackController`):
  - `GET/POST/PUT/DELETE /api/teacher/error-codes`;
  - `GET/POST/PUT/DELETE /api/teacher/comment-bank`;
  - `POST /api/teacher/comment-bank/{id}/use`.
- Cấu hình `app.grading.*` (giới hạn 100 mã riêng, 500 nhận xét cho mỗi giáo viên).
- File [backend/http/grading.http](backend/http/grading.http).
- Cập nhật tài liệu:
  - `docs/04`: lộ trình V5 ✅, ERD, mô tả cột 3 bảng. Sửa nhãn cũ: `draft_version`, `chat_*`, `audit_logs` thuộc **V6**, nhóm mở rộng thuộc **V7** (khớp lộ trình đã đánh số lại);
  - `docs/05`: mã lỗi `SCORE_LIMITED`, 9 endpoint mới, quy tắc;
  - xuất lại file Word.

**Khác so với tài liệu**
- `grades.overall_score` đổi từ `NUMERIC(3,1)` sang `NUMERIC(8,6)`. Kiểu cũ làm mất phần lẻ (6.25 thành 6.3), trái với quy tắc "chỉ làm tròn khi hiển thị" ở BR-12. Đã ghi vào docs/04.
- Mã lỗi và nhận xét gắn với **nhóm tiêu chí** `TASK / CC / LR / GRA` (không gắn trực tiếp TA hay TR), vì docs/08 ghi các mã `IRR`, `DEV` là "TA/TR", dùng cho cả hai loại Task. Khi chấm, nhóm `TASK` sẽ tự khớp với tiêu chí TA hoặc TR của phần.
- `inline_comments.error_code` lưu **dạng chữ** (bản chụp), không làm khoá ngoại: giáo viên xoá mã riêng thì nhận xét cũ không bị mất.
- **Nhận xét mẫu của hệ thống** dùng chung, chỉ đọc. `…/use` **không đếm** với mẫu, vì nếu đếm thì thứ tự hiển thị của giáo viên này sẽ bị giáo viên khác làm thay đổi. Nếu muốn mẫu "hay dùng" lên đầu, giáo viên tự thêm nhận xét đó vào ngân hàng của mình.
- Sửa / xoá mục của hệ thống → `403` (giáo viên vẫn thấy mục đó, nên trả 404 là sai). Mục riêng của giáo viên khác → `404` như quy ước.

**Kiểm tra:** `mvn test`: **353 lượt pass** (321 cũ + 32 mới). Flyway chạy V5 trên `ielts_test`, Hibernate validate schema khớp các entity mới.
- `BandCalculatorTest` (17 lượt):
  - 12 ca làm tròn (6.249999 → 6.0; 6.25 → 6.5; 6.749999 → 6.5; 6.75 → 7.0…);
  - ví dụ trong docs/08: 6.25 và 6.75 với trọng số 2 → 6.583 → hiển thị 6.5;
  - chứng minh làm tròn từng phần trước rồi mới gộp sẽ ra kết quả **khác** (6.5 thay vì 6.0);
  - trọng số tuỳ chỉnh; thiếu điểm → null.
- `GradingRulesTest` (6): lấy giới hạn nhỏ nhất kèm lý do; ≤ 20 từ → 1, 21 từ thì không giới hạn; vượt giới hạn → 422 `SCORE_LIMITED`, bằng giới hạn thì hợp lệ; ngoài khoảng / sai bước → 400; điểm null hợp lệ khi lưu nháp.
- `FeedbackLibraryIntegrationTest` (9):
  - đủ 22 mã hệ thống theo thứ tự, nhóm tiêu chí, `GOOD` là mã khen;
  - mã riêng chuyển chữ hoa, xếp sau mã hệ thống, giáo viên khác không thấy;
  - trùng mã hệ thống hoặc mã của mình (không phân biệt hoa thường) → 409, giáo viên khác đặt cùng mã được;
  - sửa giữ nguyên mã không bị báo trùng; mã hệ thống → 403; của người khác → 404;
  - validate (khoảng trắng, quá 20 ký tự, thiếu tên, nhóm sai);
  - đủ 20 nhận xét mẫu, xếp sau nhận xét của tôi; lọc theo tiêu chí kèm mục dùng chung;
  - tìm theo từ khoá có `%`;
  - `use` tăng số lần, đẩy lên đầu, không đếm với mẫu;
  - sửa / xoá, 403 / 404 / 400; sinh viên → 403.

**Ghi chú:** DB dev `ielts` sẽ tự chạy V5 ở lần khởi động backend tiếp theo.

### 2026-10-22 · B6b — Hàng đợi chấm, màn chấm, lưu chấm, trả bài, yêu cầu sửa
**Đã làm**
- **Module bài nộp** — [TeacherSubmissionAccess](backend/src/main/java/com/ltmtu/ielts/submission/TeacherSubmissionAccess.java):
  - cửa duy nhất để module chấm bài đọc và chuyển trạng thái bài nộp phía giáo viên (giống `StudentAssignmentAccess` ở B5);
  - bài nộp của giáo viên khác → 404 (BR-15);
  - `requireOwnedForUpdate` **khoá dòng trước rồi mới đọc**, để trạng thái đọc được luôn mới nhất;
  - chuyển trạng thái `markGraded` / `markReturned` / `markRevisionRequested`, kiểm tra trạng thái nguồn theo docs/06;
  - truy vấn hàng đợi `SubmissionRepository.gradingQueue`: một câu JPQL, lọc bài / phần / lớp / từ khoá (mã SV, họ tên) / trạng thái, sắp theo thời điểm nộp.
- **Module chấm bài**:
  - [GradeUpdater](backend/src/main/java/com/ltmtu/ielts/grading/GradeUpdater.java): áp body lưu chấm (thay toàn bộ). **Kiểm tra hết rồi mới ghi**, nên lỗi ở tiêu chí sau không để lại thay đổi dở dang;
    - điểm dùng `GradingRules` (BR-11, BR-17, BR-18);
    - "Chấm 0" (BR-19): mọi tiêu chí = 0, kèm điểm khác 0 → 400;
    - nhận xét inline (BR-21): vị trí trong bài, `quote` phải khớp đúng đoạn văn, có ít nhất một trong mã lỗi / nhận xét / gợi ý sửa, mã lỗi là của hệ thống hoặc của chính mình, **tự gắn tiêu chí** theo nhóm mã lỗi (`TASK` → TA/TR);
    - tiêu chí / mục checklist của phần khác, gửi trùng → 400.
  - [GradingService](backend/src/main/java/com/ltmtu/ielts/grading/GradingService.java): hàng đợi (kèm band và trạng thái chấm), màn chấm theo version, danh sách version, lưu chấm, trả bài, yêu cầu sửa.
  - `GradeMapper`: `GradeDto` có `rawBand` (chưa làm tròn), `band` (đã làm tròn), `complete`, và **giới hạn hiện tại của từng tiêu chí** (`maxAllowed`, `limitReason`) để giao diện khoá ô điểm.
  - Màn chấm có cảnh báo `SHORT_ESSAY` (≤ 20 từ) / `UNDER_MIN_WORDS`, và các cờ `editable`, `canReturn`, `canRequestRevision` để giao diện bật / tắt nút.
  - `GradingEvents.Returned` / `RevisionRequested` để B7 gửi thông báo.
  - `TeacherGradingController`: 6 endpoint `/api/teacher/submissions…`.
- Cập nhật tài liệu, xuất lại file Word:
  - `docs/05` mục A.7: quy tắc và dạng dữ liệu thật của màn chấm và body lưu chấm;
  - `docs/02` UC-17 (giới hạn điểm, bài quá ngắn, mã lỗi, ngân hàng nhận xét, "Chấm 0", ngoại lệ E2 / E3) và UC-18 (E3);
  - `docs/03` SD-08 (khoá dòng, các bước kiểm tra, sự kiện sau commit).
- [grading.http](backend/http/grading.http): thêm các request chấm bài.

**Khác so với tài liệu**
- `PUT …/grade` trả **`GradeDto` đầy đủ** thay vì `{overallScore}`: giao diện cần giới hạn từng tiêu chí và nhận xét inline đã có id. Trả bài / yêu cầu sửa trả lại cả màn chấm.
- Thêm tham số `keyword` (mã SV / họ tên) cho hàng đợi.
- Chỉ chấm được **version mới nhất**. Version cũ chỉ xem (UC-17 A3), lưu vào version cũ → 409.
- **Không yêu cầu sửa được khi bài tập đã đóng** (409): bài đã đóng thì sinh viên không nộp lại được, yêu cầu sửa sẽ không có tác dụng. Vẫn chấm và trả bài được như docs/06. Đã thêm vào UC-18 E3.
- `classId` không phải lớp của mình → 400 (dùng lại `ClassroomService.requireOwnedClassrooms`), vì đây là tham số lọc.
- `quote` của nhận xét inline: nếu client gửi thì phải khớp đúng đoạn văn, để phát hiện client tính lệch vị trí. Offset tính theo đơn vị UTF-16, giống JavaScript.

**Kiểm tra:** `mvn test`: **370 lượt pass** (353 + 17).
- `GradingIntegrationTest` (11):
  - màn chấm đủ dữ liệu, cảnh báo thiếu số từ;
  - BR-15: giáo viên khác → 404, sinh viên → 403;
  - lưu dở: band null, sang GRADED, trả bài → 409 kèm tên tiêu chí còn thiếu;
  - BR-12: 6 / 7 / 6.5 / 6 → `rawBand` 6.375, `band` 6.5;
  - BR-11: sai bước, quá 9;
  - BR-17: tick "Không chia đoạn" → 6 bị 422, 5 được, nhiều mục lấy mức nhỏ nhất, mục CHECK không giới hạn;
  - BR-18: bài 9 từ → cảnh báo, điểm 2 bị 422;
  - BR-19: "Chấm 0" → band 0.0, kèm điểm khác 0 / lý do sai → 400, bỏ lý do thì chấm lại;
  - BR-21: mã lỗi chuẩn hoá, tự gắn tiêu chí GRA / TR, đoạn trích lấy từ bài, lưu lại thì thay toàn bộ;
  - 6 kiểu nhận xét inline sai (ngoài bài, độ dài 0, rỗng, mã không tồn tại, mã của giáo viên khác, đoạn trích lệch); mã riêng của mình dùng được;
  - sai version / tiêu chí của phần khác / trùng / thiếu `versionNo`.
- `GradingFlowIntegrationTest` (6):
  - hàng đợi: lọc theo bài, phần, từ khoá, lớp (chưa vào lớp → không có, vào rồi → có), giáo viên khác không thấy; sau khi chấm thì sang GRADED kèm band; `status=DRAFT` / lớp người khác → 400;
  - trả bài: chưa chấm → 409; trả xong thì kết quả và bài nộp RETURNED, có sự kiện kèm band, không sửa / trả / yêu cầu sửa được nữa;
  - **vòng sửa bài đầy đủ**: yêu cầu sửa → có sự kiện → sinh viên nạp lại bài cũ (`LAST_VERSION`) → **quá hạn bài gốc nhưng còn hạn sửa nên nộp v2 không bị muộn** (BR-07) → giáo viên chấm v2, xem v1 chỉ đọc với band cũ, lưu vào v1 → 409 → trả v2;
  - BR-10: hết lượt sửa → 422 `REVISION_LIMIT`, vẫn trả bài được;
  - hạn sửa ở quá khứ / thiếu hạn → 400; bài đã đóng → không yêu cầu sửa được nhưng vẫn trả bài được;
  - **2 request "Trả bài" đồng thời → đúng 1 thành công** (200 + 409).

**Ghi chú:** lệnh xuất Word lỗi khi chạy trong Git Bash: console cp1252 không in được ký tự `✔`, file vẫn được tạo. Chạy bằng PowerShell thì bình thường.

### 2026-10-22 · B6c — Sinh viên xem kết quả, band cả bài, số liệu danh sách bài của giáo viên
**Đã làm**
- **Kết quả phía sinh viên** ([StudentResultService](backend/src/main/java/com/ltmtu/ielts/grading/StudentResultService.java), `StudentResultController`):
  - `GET /api/student/parts/{partId}/versions`: thêm `returned`, `band`, `returnedAt`;
  - `GET …/versions/{n}`: thêm `result`, **chỉ khi đã trả bài** (BR-13). Gồm band, "Chấm 0", nhận xét chung, người chấm, điểm và nhận xét từng tiêu chí, **lý do bị giới hạn** (`limitedTo`, `limitReason`), checklist đạt / chưa đạt, nhận xét inline kèm tên mã lỗi và tiêu chí, bảng **"Lỗi trong bài"**;
  - **ẩn gợi ý sửa** khi bài đang "Cần sửa" (version đang bị yêu cầu sửa, bài tập không bật hiển thị); nộp bản sửa xong thì gợi ý của bản cũ hiện lại (BR-21);
  - mới: `GET /api/student/assignments/{id}/result`: band từng phần (lần nộp gần nhất đã được trả) và **band cả bài** theo trọng số phần. Chỉ có khi mọi phần đã có kết quả, kèm ghi chú cách tính ước lượng hoặc "Chưa đủ phần" (BR-12, docs/08 mục 3.2).
- **Module bài nộp**: thêm `StudentSubmissionAccess` để module chấm bài đọc bài làm của chính sinh viên, luôn qua điều kiện BR-04 / BR-05. Bỏ 2 endpoint lịch sử nộp khỏi `StudentSubmissionController` và `StudentSubmissionService`, chuyển sang module chấm bài, **giữ nguyên đường dẫn**.
- **Danh sách bài của giáo viên** có thêm `recipientCount`, `completedCount` (người nhận đã nộp đủ mọi phần), `pendingGradingCount` (bài nộp chờ chấm). Tính cho cả trang bằng **một câu SQL** (`AssignmentRepository.progressCounts`, CTE + UNION như cách tính người nhận). Đóng tồn đọng từ B4.
- Cập nhật `docs/05` (A.5, A.8: dạng dữ liệu kết quả, quy tắc ẩn gợi ý, endpoint band cả bài), `student.http`, xuất lại Word.

**Khác so với tài liệu / quyết định**
- **Tránh phụ thuộc vòng giữa module**: kết quả chấm thuộc module chấm bài, mà module này đã dùng module bài nộp. Nếu bài nộp gọi ngược sang chấm bài thì hai module phụ thuộc vòng. Vì vậy:
  - 2 endpoint lịch sử nộp chuyển sang module chấm bài, đường dẫn và các trường cũ giữ nguyên, test B5 vẫn pass;
  - band cả bài dùng **endpoint riêng** `…/assignments/{id}/result`, không nhét vào chi tiết bài của B5b.
- Số liệu danh sách dùng **native SQL** truy vấn cả bảng `submissions` từ repository của module bài tập. Đây là đọc số liệu, cùng kiểu với `VISIBLE_TO_STUDENT` (đã truy vấn `ClassMember`), không ghi chéo module.
- "Đã nộp đủ" chỉ tính **người nhận hiện tại**. Sinh viên đã bị rút khỏi lớp sau khi nộp thì không được tính.
- **Diff giữa 2 version**: API đã trả nội dung từng version (`?versionNo`), phần so sánh sẽ tính ở frontend. Backend không thêm endpoint diff.

**Kiểm tra:** `mvn test`: **376 lượt pass** (370 + 6). `StudentResultIntegrationTest` (6):
- BR-13: đang chấm nháp thì `returned = false`, `result = null`; trả rồi mới thấy band 6.0, nhận xét chung, người chấm;
- tiêu chí CC bị giới hạn 5 vì "Không chia đoạn", checklist tick / không tick, nhận xét inline có tên mã "Hoà hợp chủ ngữ – động từ", tiêu chí GRA, gợi ý sửa;
- bảng lỗi: LR–SP ×1, GRA–SV ×2, GRA–ART ×1, theo thứ tự rubric và số lần. Bỏ `GOOD` và nhận xét không mã;
- BR-21: đang "Cần sửa" → gợi ý ẩn, mã lỗi vẫn hiện; nộp bản sửa → gợi ý của v1 hiện lại, v2 chưa có kết quả; bài bật hiển thị → hiện ngay;
- **band cả bài**:
  - bài 2 phần, trả phần 1 trước → `complete = false`, band phần 1 = 6.5;
  - phần 2 mới chấm nháp → vẫn chưa đủ;
  - trả phần 2 → 6.25 và 6.75 (trọng số 2) → **6.5**, phần 2 hiển thị 7.0;
  - sinh viên khác chưa có kết quả; người không được giao → 404;
- danh sách giáo viên: 2 người nhận, 1 người nộp đủ, 3 bài chờ chấm, sau khi trả 2 bài còn 1.

### 2026-10-22 · B7a — Thông báo trong ứng dụng, WebSocket STOMP, email theo sự kiện
**Đã làm**
- **Module `notification`**:
  - entity `Notification` (bảng `notifications` có từ V1, không cần migration mới), `NotificationType` (loại nào có kèm email);
  - [NotificationService](backend/src/main/java/com/ltmtu/ielts/notification/NotificationService.java) `notify(userIds, …)`: **lưu DB trong transaction của nghiệp vụ**, rồi **sau khi commit** mới đẩy `/user/queue/notifications` và gửi email nền. Đẩy WebSocket lỗi chỉ ghi log, vì thông báo đã có trong DB;
  - [NotificationEventListener](backend/src/main/java/com/ltmtu/ielts/notification/NotificationEventListener.java): nghe ở pha `BEFORE_COMMIT` các sự kiện phát hành, giao thêm, nộp bài, trả bài, yêu cầu sửa; dựng tiêu đề, nội dung (giờ Việt Nam) và link frontend;
  - API `/api/notifications`: danh sách (`unreadOnly`, `after`, phân trang), số chưa đọc, đánh dấu đã đọc / tất cả;
  - `MailService.sendNotification` (`@Async`), cấu hình mới `app.mail.app-url` để ghép link trong email.
- **WebSocket STOMP** ([WebSocketConfig](backend/src/main/java/com/ltmtu/ielts/config/WebSocketConfig.java)):
  - endpoint `/ws`, WebSocket thuần, simple broker `/topic` `/queue`, heartbeat 10 giây, `/app`, `/user`;
  - origin theo `app.cors`.
- **Xác thực WebSocket** ([StompAuthInterceptor](backend/src/main/java/com/ltmtu/ielts/security/StompAuthInterceptor.java)):
  - CONNECT kiểm tra token và phiên giống hệt REST: tách phần này thành `AccessTokenVerifier`, `JwtAuthFilter` cũng dùng chung;
  - tài khoản đang bị bắt đổi mật khẩu → từ chối;
  - Mức 1 chỉ cho SUBSCRIBE kênh riêng `/user/queue/notifications|session`; `/topic/…` và mọi SEND → `ERROR`.
- `AuthPrincipal` là `java.security.Principal` (tên = tên đăng nhập), để `convertAndSendToUser(username, …)` tìm đúng phiên WebSocket.
- **Sự kiện mang thêm dữ liệu hiển thị**:
  - `Published` / `RecipientsAdded`: thêm `title`, `dueAt`;
  - `Submitted`: thêm `assignmentTitle`, `partTitle`, `studentName`;
  - `Returned` / `RevisionRequested`: thêm `assignmentTitle`, `partTitle`.
  - Module thông báo nhờ vậy không phải đọc repository của module khác.
- `UserQueryService.findAllByIds`.
- Tài liệu:
  - `docs/05`: A.9 (bảng sự kiện → thông báo, thứ tự lưu / đẩy), B.2 (phân quyền Mức 1);
  - `docs/03`: ghi chú SD-05;
  - **coding rules mục 11** viết lại theo cách làm mới;
  - [notification.http](backend/http/notification.http); xuất lại Word.

**Khác so với tài liệu / quyết định**
- **Lưu thông báo ở `BEFORE_COMMIT`** (cùng transaction), không phải `AFTER_COMMIT` như rules cũ ghi. Lúc `AFTER_COMMIT` transaction đã kết thúc: muốn ghi DB phải mở transaction mới, và nếu lỗi giữa chừng thì nghiệp vụ đã xong mà thông báo bị mất. Cách mới đúng ý SD-05 hơn ("lưu DB trước, đẩy realtime sau") và không có thông báo "ma". Đã cập nhật coding rules.
- Giáo viên **không nhận email** khi sinh viên nộp (chỉ có thông báo trong ứng dụng), để hộp thư không bị đầy khi cả lớp nộp. Đúng với FR-51 (email chỉ cho bài mới, trả bài, yêu cầu sửa, nhắc hạn).
- Thông báo "Bài tập mới" ghi **hạn chung của bài**; phần có hạn riêng thì xem trong chi tiết bài.
- Kênh `/user/queue/session` (FORCE_LOGOUT) đã cho SUBSCRIBE, nhưng việc gửi làm ở Mức 2 (một tài khoản một phiên).

**Kiểm tra:** `mvn test`: **387 lượt pass** (376 + 11). Chạy được ngay lần đầu, kể cả các test cũ có phát sự kiện.
- `NotificationIntegrationTest` (6):
  - phát hành cho 2 sinh viên → mỗi người 1 thông báo (tiêu đề, "Hạn nộp: 23:59 05/01/2027" theo giờ Việt Nam, link, `createdAt` theo giờ server), số chưa đọc = 1, **email** có link đầy đủ;
  - bài mở sau → chưa thông báo;
  - nộp bài → giáo viên có thông báo (tên SV, link màn chấm), **không** có email;
  - yêu cầu sửa → "Cần sửa bài", hạn sửa + lời nhắn, có email; trả bài → "Band: 6.5";
  - **nghiệp vụ lỗi sau khi phát sự kiện → rollback → không có thông báo, không có email**;
  - API: mới nhất trước, `?after=` chỉ lấy thông báo sau mốc, đánh dấu đã đọc (của người khác → 404), lọc chưa đọc, đọc tất cả, `after` sai → 400, chưa đăng nhập → 401.
- `WebSocketIntegrationTest` (5) — **server thật, cổng ngẫu nhiên, client STOMP thật**:
  - giáo viên phát hành → sinh viên đang online **nhận ngay** qua `/user/queue/notifications`, sinh viên khác không nhận;
  - CONNECT thiếu token / token sai → `ERROR`;
  - token của phiên đã đăng xuất → bị từ chối; tài khoản phải đổi mật khẩu → bị từ chối;
  - SUBSCRIBE `/topic/…` và SEND `/app/…` → `ERROR`;
  - `id` nhận qua WebSocket dùng được ngay với REST (đánh dấu đã đọc).

**Ghi chú:** khi từ chối một khung SEND, Spring tự ghi một dòng log mức ERROR (`StompSubProtocolHandler … Không có quyền gửi tới …`). Đây là hành vi đúng, không phải lỗi.

### 2026-10-22 · B7b — Job nhắc hạn, thông báo bài hẹn giờ mở, giáo viên nhắc thủ công
**Đã làm**
- **Nhắc hạn tự động** ([DeadlineReminderJob](backend/src/main/java/com/ltmtu/ielts/notification/DeadlineReminderJob.java); UC-30, SD-10, FR-52, BR-14):
  - `ReminderLogRepository.findCandidates`: **một câu SQL** (CTE + UNION) tìm mọi (phần, sinh viên) cần nhắc:
    - hạn nộp: hạn phần → hạn bài, bài đang mở, người nhận gồm cả thành viên lớp, chưa nộp (chưa có bài làm / mới nháp);
    - hạn sửa: bài làm đang "Cần sửa";
    - mốc H2 nếu còn ≤ 2 giờ, ngược lại H24; bỏ người đã có `reminder_log` ở mốc đó;
  - mỗi nhóm (phần, mốc) một transaction: `INSERT … ON CONFLICT DO NOTHING` cho từng người, **chỉ ai chèn được mới nhận thông báo** (`DEADLINE_REMINDER`, có email). Tạo thông báo lỗi → bản ghi "đã nhắc" cũng rollback, lượt sau nhắc lại;
  - nội dung: "Sắp hết hạn nộp / hạn sửa bài: {bài} – {phần}", hạn theo giờ Việt Nam, "còn dưới 24 / 2 giờ".
- **Vòng sửa mới được nhắc lại**: khi có `RevisionRequested`, listener xoá mốc `REVISION_*` cũ của (phần, sinh viên).
- **Bài hẹn giờ mở** ([AssignmentOpeningService](backend/src/main/java/com/ltmtu/ielts/assignment/AssignmentOpeningService.java)):
  - tìm bài đã phát hành **trước** giờ mở, nay đã tới giờ mà chưa thông báo; "giành" từng bài bằng `UPDATE … WHERE open_notified_at IS NULL` rồi phát sự kiện mới `AssignmentEvents.Opened`. Listener ở B7a tạo thông báo "Bài tập mới" cho **mọi người nhận lúc mở** (gồm cả người được giao thêm trước giờ mở);
  - phát hành khi bài đã mở thì ghi luôn `open_notified_at`, job bỏ qua.
- **Nhắc thủ công** `POST /api/teacher/assignments/{id}/remind {studentIds, partId?}` (UC-16):
  - `PublishService.remind` kiểm tra chủ bài (404), bài đang mở (409), người nhận và phần (400);
  - phát sự kiện `ManualReminder` → thông báo `MANUAL_REMINDER` có email.
- **Bộ lập lịch** `ScheduledJobs`: 2 job `@Scheduled(fixedDelay)` theo `app.reminders.interval` (mặc định 15 phút), chỉ gọi service. Tắt bằng `app.reminders.enabled=false` (profile test tắt và tự gọi job với đồng hồ giả).
- Cấu hình `app.reminders` (`enabled`, `interval`, `soon = PT24H`, `urgent = PT2H`) → `AppProperties.Reminders`.
- Tài liệu: `docs/03` (ghi chú SD-10 về cách cài), `docs/04` (`reminder_log`), `docs/05` (A.6 nhắc thủ công, bảng thông báo ở A.9), `assignment.http`; xuất lại Word.

**Khác so với tài liệu / quyết định**
- Bài hẹn giờ mở dùng **sự kiện `Opened`** qua listener, giống luồng phát hành. Module bài tập không phải gọi sang module thông báo.
- BR-14 tính **theo từng hạn sửa**: mỗi lần yêu cầu sửa là một hạn mới, nên mốc nhắc hạn sửa được đặt lại. Riêng hạn nộp, nếu giáo viên **gia hạn sau khi đã nhắc** thì không nhắc lại mốc đó (đúng chữ "một lần cho mỗi mốc").
- Nhắc thủ công **chưa giới hạn số lần**. Sẽ thêm rate limit ở bước 12 (Mức 2) nếu cần.
- Thông báo "Bài tập mới" được gửi theo danh sách người nhận **tại thời điểm mở**, không phải lúc phát hành.

**Kiểm tra:** `mvn test`: **394 lượt pass** (387 + 7), chạy lại lần nữa vẫn pass. `ReminderJobIntegrationTest` (7):
- **2 mốc, mỗi mốc 1 lần**: còn 20 giờ → người chưa làm và người đang nháp được nhắc (có email), người đã nộp không; chạy lại không nhắc trùng; còn 1 giờ → mốc 2 giờ; quá hạn → thôi;
- lần chạy đầu đã còn 90 phút → chỉ mốc 2 giờ; còn 30 giờ, bài chưa mở, bài đã đóng → không nhắc;
- người nhận **qua lớp** được nhắc; **hạn riêng của phần** (5 giờ) được dùng thay hạn bài (3 ngày);
- **hạn sửa**: nhắc "Sắp hết hạn sửa bài", nộp bản sửa thì thôi, **vòng sửa thứ 2 được nhắc lại**;
- **2 lượt job chạy đồng thời** → mỗi sinh viên vẫn chỉ 1 lần nhắc;
- bài hẹn giờ: chưa tới giờ thì chưa báo; tới giờ thì báo đúng 1 lần cho cả người được giao thêm; bài mở ngay không bị báo lại;
- nhắc thủ công: thông báo đúng nội dung; người ngoài / phần sai / danh sách rỗng → 400, giáo viên khác → 404, bài nháp → 409.

**Lỗi gặp khi chạy test (không phải lỗi code):** một lượt `mvn test` báo `NoClassDefFoundError: GradingEnums$1` (lớp phụ do `switch` trên enum sinh ra). Lỗi này xảy ra khi IDE (VS Code Java) và Maven cùng biên dịch vào `target/classes`. Chạy `mvn clean test` thì hết. Nếu gặp lại thì chạy `mvn clean` trước.

---

## Tồn đọng / việc cần làm tiếp
- [ ] Docker: chưa cần khi phát triển (đã có PostgreSQL trên máy). `docker-compose.yml` + `.env.example` làm ở bước 13 (Docker hoá).
- [ ] Chưa commit git, chờ người dùng đồng ý.
- [ ] `PLAN.md` vẫn ghi bảng `login_sessions` và các endpoint cũ. **Tài liệu trong `docs/` là bản chuẩn**; PLAN chỉ là kế hoạch ban đầu.
- [ ] (tuỳ chọn) File mẫu `tools/docs-export/reference.docx` theo định dạng báo cáo của trường.
- [ ] Người dùng chạy các prompt trên Figma Make, gửi lại link Figma để code giao diện theo thiết kế.
- [ ] A6 phần còn lại: rules frontend + `CLAUDE.md` gốc. Làm khi bắt đầu code frontend.
- [ ] Audit log cho import (IP, số dòng) — làm cùng bảng `audit_logs` ở Mức 2.
- [ ] Trường `grades.updated_at` dùng auditing của Spring Data (giờ hệ thống), không qua bean `Clock` (giống mọi bảng khác từ B1). Chưa ảnh hưởng nghiệp vụ; nếu cần test theo giờ thì cấu hình `DateTimeProvider` dùng `Clock`.
- [ ] Frontend (khi có thiết kế Figma): so sánh 2 version (diff) ở màn chấm và màn kết quả, dùng nội dung từ `…/submissions/{id}?versionNo` và `…/versions/{n}`.
- [ ] Nhắc thủ công chưa có giới hạn tần suất — xem lại ở bước 12 (rate limit).
- [ ] **Bước tiếp theo:** B8 — tiến độ, thống kê, xuất Excel (UC-16, UC-19, UC-28; docs/05 A.6):
  - ma trận tiến độ `GET /api/teacher/assignments/{id}/progress?classId&status` (sinh viên × phần: trạng thái, muộn, band);
  - thống kê một bài `…/stats` (tỉ lệ nộp đúng hạn, band trung bình, theo tiêu chí, phân bố band, **mã lỗi hay gặp**);
  - thống kê lớp / sinh viên `GET /api/teacher/stats/classes/{id}`, `/students/{id}`; kết quả học tập của sinh viên `GET /api/student/results`;
  - xuất bảng điểm `.xlsx` (Apache POI).
