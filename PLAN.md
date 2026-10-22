# Kế hoạch: Website giao bài & chấm bài IELTS trực tuyến

## Context
Đề tài gốc: server tạo tài khoản theo mã sinh viên (MSSV) từ file giáo viên cung cấp; giáo viên giao bài cho những sinh viên được chọn; sinh viên viết và nộp bài trên web; mỗi đề có checklist để giáo viên chấm.
Bản mở rộng thêm: bài tập có nhiều phần (nộp riêng từng phần), giao theo lớp hoặc theo cá nhân, đặt hạn nộp và tiêu chí chấm, lưu nháp, chấm theo checklist, nhận xét từng phần, trả bài, yêu cầu sửa bài, nhắc hạn nộp, theo dõi tiến độ, lịch sử nộp, thống kê.
Thư mục `D:\LTMTU` đang trống, nên đây là dự án mới.
Người dùng đã chọn: **Spring Boot + React + PostgreSQL**, có **WebSocket realtime**, **Docker**, **email**, **giao diện tiếng Việt**, và 3 vai trò **Admin / Giáo viên / Sinh viên**.

Đây là đồ án môn **Lập trình mạng**, và giảng viên thích đề tài quy mô lớn. Vì vậy plan giữ toàn bộ nghiệp vụ, thêm một khối lớn về mạng (mục 9) và các tính năng mở rộng (mục 10). Tất cả được chia thành 3 mức ưu tiên (xem "Thứ tự triển khai"), để luôn có một bản chạy được trước khi làm thêm.

Môi trường: JDK 24, Node 24, npm 11 đã cài. **Chưa có Docker và PostgreSQL**: cần cài Docker Desktop (PostgreSQL và Mailpit sẽ chạy trong container).

## Tech stack
- **Backend**: Spring Boot 3.5, Java 21 (`--release 21`), Maven Wrapper, Spring Web, Spring Data JPA, Spring Security + JWT (jjwt), Spring WebSocket (STOMP), Spring Mail, Validation, Flyway, Apache POI (đọc .xlsx), Commons CSV, springdoc-openapi (Swagger), Lombok, MapStruct.
- **DB**: PostgreSQL 16, schema quản lý bằng Flyway (`db/migration/V1__init.sql`, ...).
- **Frontend**: React 18 + Vite + TypeScript, TailwindCSS, React Router, TanStack Query, axios, react-hook-form + zod, `@stomp/stompjs`, Recharts (biểu đồ thống kê), dayjs (locale `vi`). Toàn bộ chuỗi hiển thị tiếng Việt, gom vào `src/i18n/vi.ts`.
- **Docker**: `docker-compose.yml` gồm `postgres`, `mailpit` (bắt email khi dev, giao diện xem thư ở :8025), `backend`, `frontend` (build bằng Vite, phục vụ qua nginx; nginx proxy `/api` và `/ws` sang backend).
- **Kiến trúc nguyên khối (monolith)**: chỉ **một** ứng dụng Spring Boot chứa toàn bộ nghiệp vụ, REST, WebSocket và server TCP. Không chia microservice, không chạy nhiều instance, không dùng Redis.
- **Mở rộng về mạng** (đều nằm trong monolith):
  - Trạng thái online và giới hạn tần suất request (rate limit) lưu **trong bộ nhớ** của ứng dụng (`ConcurrentHashMap`).
  - **nginx**: chỉ làm reverse proxy. Nó bật HTTPS/WSS bằng chứng chỉ tự ký, phục vụ frontend, và chuyển `/api`, `/ws` sang backend.
  - **Spring Actuator + Prometheus + Grafana** (tuỳ chọn): theo dõi số request, số kết nối WebSocket, độ trễ.
  - **k6**: load test.
  - **Java thuần** (`java.net`, `java.nio`): module TCP socket tự viết.

## Cấu trúc thư mục
```
LTMTU/
├─ backend/   src/main/java/com/ltmtu/ielts/
│   ├─ config/        (Security, WebSocket, CORS, Mail, Scheduling)
│   ├─ security/      (JwtService, JwtAuthFilter, StompAuthInterceptor)
│   ├─ user/          (User, Role, AdminController, AuthController)
│   ├─ classroom/     (Classroom, ClassMember, StudentImportService)
│   ├─ assignment/    (Assignment, AssignmentPart, AssignmentTarget, Rubric*)
│   ├─ submission/    (Submission, SubmissionVersion, DraftService)
│   ├─ grading/       (Grade, CriterionScore, ChecklistMark, BandCalculator)
│   ├─ notification/  (Notification, NotificationService, DeadlineReminderJob, MailService)
│   ├─ stats/         (StatsService, ExportService)
│   ├─ realtime/      (PresenceService, ChatController, SessionGuard, LiveProgressPublisher, TimeSyncController)
│   ├─ exam/          (ExamSession, ExamEventService: chế độ thi tính giờ, ghi nhận chuyển tab)
│   ├─ plagiarism/    (SimilarityService: so độ giống nhau giữa các bài)
│   ├─ tcpgateway/    (TcpImportServer: ServerSocket + thread pool, giao thức tự định nghĩa)
│   └─ common/        (BaseEntity, ApiException, GlobalExceptionHandler, PageResponse, AuditLog)
├─ admin-cli/  (client Java thuần: gửi file danh sách sinh viên tới server qua TCP)
├─ frontend/  src/{api,components,pages/{admin,teacher,student},hooks,i18n,store}
├─ infra/     (nginx.conf: reverse proxy + TLS, prometheus.yml, grafana dashboards)
├─ loadtest/  (script k6: 200 sinh viên nộp bài cùng lúc, 500 kết nối WebSocket)
├─ docs/      (sơ đồ tuần tự, đặc tả giao thức TCP, ảnh Wireshark)
├─ docker-compose.yml
├─ .env.example
└─ README.md
```

## Mô hình dữ liệu (Flyway V1)
| Bảng | Các cột chính |
|---|---|
| `users` | id, username (= MSSV với sinh viên), password_hash, full_name, email, role (ADMIN/TEACHER/STUDENT), student_code (unique), must_change_password, active |
| `classrooms` | id, code, name, teacher_id |
| `class_members` | classroom_id, student_id (PK kép) |
| `rubric_templates` | id, name, owner_id. Seed sẵn mẫu IELTS Writing Task 1 và Task 2 |
| `assignments` | id, teacher_id, title, description, status (DRAFT/PUBLISHED/CLOSED), open_at, due_at, allow_late, max_revisions |
| `assignment_parts` | id, assignment_id, order_no, title, prompt (HTML), image_url, min_words, suggested_minutes, part_due_at (null = theo hạn của bài) |
| `rubric_criteria` | id, part_id, name, description, max_score (mặc định 9), step (0.5), weight, order_no |
| `checklist_items` | id, criterion_id, content, order_no |
| `assignment_targets` | id, assignment_id, classroom_id (nullable), student_id (nullable). Có CHECK: phải có đúng 1 trong 2 cột |
| `submissions` | id, part_id, student_id, status (NOT_STARTED/DRAFT/SUBMITTED/GRADED/RETURNED/REVISION_REQUESTED), current_version, draft_content, draft_saved_at, revision_due_at. UNIQUE(part_id, student_id) |
| `submission_versions` | id, submission_id, version_no, content, word_count, submitted_at, is_late. Đây là lịch sử nộp, không bao giờ sửa |
| `grades` | id, submission_version_id, grader_id, overall_score, general_comment, status (DRAFT/RETURNED), returned_at |
| `grade_criterion_scores` | grade_id, criterion_id, score, comment |
| `grade_checklist_marks` | grade_id, checklist_item_id, checked |
| `inline_comments` | id, grade_id, start_offset, end_offset, quote, comment (nhận xét gắn vào đoạn văn được bôi đen) |
| `notifications` | id, user_id, type, title, body, link, read_at, created_at |
| `reminder_log` | assignment_part_id, student_id, kind (H24/H2). Dùng để không gửi nhắc trùng |
| `chat_messages` | id, submission_id (nullable), classroom_id (nullable), sender_id, content, created_at. Chat theo bài hoặc theo lớp |
| `login_sessions` | id, user_id, session_id, ip, user_agent, created_at, revoked_at. Phục vụ chính sách một tài khoản một phiên |
| `exam_sessions` | id, part_id, student_id, started_at, ends_at, finished_at |
| `exam_events` | id, exam_session_id, type (TAB_HIDDEN/TAB_VISIBLE/DISCONNECT/RECONNECT/PASTE), occurred_at, ip |
| `similarity_reports` | id, part_id, version_a_id, version_b_id, score, created_at |
| `audit_logs` | id, actor_id, action, target_type, target_id, ip, created_at |

Một sinh viên được giao bài nếu có target trỏ thẳng tới sinh viên đó, HOẶC trỏ tới lớp có sinh viên đó. Truy vấn này nằm gọn trong một hàm `AssignmentRepository.findVisibleForStudent(studentId, now)`.

## Chức năng chính và API

### 1. Xác thực và tài khoản
- `POST /api/auth/login`, `/refresh`, `/change-password`. Dùng JWT: access token 30 phút, refresh token 7 ngày.
- Lần đăng nhập đầu, nếu `must_change_password = true` thì frontend bắt buộc đổi mật khẩu.
- Admin (`/api/admin/**`): CRUD giáo viên, khoá/mở tài khoản, reset mật khẩu, xem thống kê toàn hệ thống. Tài khoản admin đầu tiên được seed từ biến môi trường `ADMIN_USERNAME` / `ADMIN_PASSWORD`.

### 2. Sinh tài khoản từ file (yêu cầu cốt lõi)
- `POST /api/teacher/classes/{id}/import` (multipart, nhận `.xlsx` hoặc `.csv`). Các cột: `ma_sv, ho_ten, email` (email không bắt buộc).
- `StudentImportService`:
  1. Đọc file, kiểm tra từng dòng.
  2. MSSV đã tồn tại: chỉ thêm vào lớp. MSSV mới: tạo user với `username = MSSV` và mật khẩu ngẫu nhiên 8 ký tự, đặt `must_change_password = true`.
  3. Trả về báo cáo: số dòng tạo mới, số dòng bỏ qua, số dòng lỗi (kèm số dòng và lý do).
- Có thể tải file `.xlsx` chứa danh sách tài khoản và mật khẩu vừa tạo. File chỉ tải được một lần, ngay sau khi import.
- Nếu tick "Gửi email" thì gửi thông tin đăng nhập qua `MailService` (chạy bất đồng bộ với `@Async`).
- Có sẵn file mẫu tải về tại `GET /api/teacher/import-template`.

### 3. Lớp học
CRUD lớp, xem danh sách thành viên, thêm hoặc xoá sinh viên thủ công.

### 4. Tạo bài tập (giáo viên)
- Có trình soạn nhiều bước: (a) thông tin chung và thời hạn, (b) các phần của bài: đề bài, ảnh biểu đồ (Task 1), số từ tối thiểu, (c) tiêu chí chấm và checklist cho từng phần (nạp từ mẫu IELTS rồi sửa lại), (d) giao bài: chọn lớp và/hoặc từng sinh viên.
- `POST/PUT /api/teacher/assignments`, `POST /{id}/publish`, `POST /{id}/close`, `POST /{id}/duplicate`.
- Upload ảnh đề bài: `POST /api/files`. File lưu ở thư mục volume `uploads/`.
- Khi publish: tạo thông báo cho toàn bộ sinh viên được giao, qua WebSocket và email.

### 5. Làm bài (sinh viên)
- Dashboard chia các tab: Cần làm / Sắp hết hạn / Quá hạn / Đã nộp / Cần sửa.
- Trang làm bài: mỗi phần là một tab riêng, có textarea đếm số từ realtime và đồng hồ gợi ý thời gian. **Bản nháp tự lưu** sau 3 giây ngừng gõ (gọi `PUT /api/student/parts/{partId}/draft`) và khi rời trang. Trạng thái "Đã lưu lúc hh:mm" hiển thị trên giao diện.
- `POST /api/student/parts/{partId}/submit`: tạo `submission_version` mới. Nếu nộp sau hạn thì đánh dấu `is_late`; nếu bài không cho nộp muộn thì từ chối.
- Mỗi phần được nộp độc lập với các phần khác.
- Xem kết quả: điểm từng tiêu chí, checklist, nhận xét chung, nhận xét gắn vào đoạn văn được bôi vàng.
- Khi bị yêu cầu sửa: soạn lại dựa trên bản cũ rồi nộp version mới. Có xem lịch sử các version và so sánh (diff) giữa 2 version bằng thư viện `diff`.

### 6. Chấm bài (giáo viên)
- Hàng đợi chấm có bộ lọc theo bài, phần, lớp và trạng thái.
- Màn hình chia đôi: bên trái là bài làm (bôi đen để thêm nhận xét), bên phải là các tiêu chí.
  - Mỗi tiêu chí gồm: các ô checklist, ô chọn điểm theo bước 0.5, và nhận xét.
- `BandCalculator`: tính điểm tổng của phần theo trung bình có trọng số, làm tròn theo quy tắc IELTS (.25 làm tròn lên .5, .75 làm tròn lên số nguyên). Điểm của cả bài là trung bình các phần.
- Các hành động:
  - **Lưu nháp chấm**.
  - **Trả bài** (`RETURNED`): sinh viên mới thấy điểm từ lúc này.
  - **Yêu cầu sửa** (`REVISION_REQUESTED`): kèm hạn sửa và lời nhắn.
- Mỗi hành động đều tạo thông báo cho sinh viên.
- Khi sinh viên nộp bài, giáo viên nhận thông báo realtime.

### 7. Thông báo
- `NotificationService.notify(userId, type, ...)`: lưu vào DB, đẩy qua STOMP tới `/user/queue/notifications`, và gửi email nếu loại thông báo đó có bật email.
- WebSocket endpoint `/ws`: JWT được gửi trong header STOMP CONNECT và được kiểm tra bởi `StompAuthInterceptor`.
- `DeadlineReminderJob` (`@Scheduled` chạy mỗi 15 phút): nhắc sinh viên chưa nộp khi còn 24 giờ và còn 2 giờ đến hạn (bao gồm cả hạn sửa bài). Mỗi lần nhắc được ghi vào `reminder_log` để không gửi trùng.
- Frontend: chuông thông báo hiển thị số chưa đọc, có toast realtime, và trang danh sách thông báo.
- API: `GET /api/notifications`, `POST /{id}/read`, `POST /read-all`.

### 8. Theo dõi tiến độ và thống kê
- **Tiến độ một bài** (giáo viên): ma trận sinh viên × phần, mỗi ô là trạng thái có màu (chưa làm / nháp / đã nộp / muộn / đã chấm / cần sửa). Kèm phần trăm hoàn thành. Có nút "Nhắc nhở" gửi thông báo thủ công cho những sinh viên chưa nộp.
- **Thống kê bài**: điểm trung bình mỗi tiêu chí, phân bố band (biểu đồ cột), tỉ lệ nộp đúng hạn.
- **Thống kê sinh viên**: biểu đồ đường điểm theo thời gian, điểm trung bình theo từng tiêu chí (radar), số bài nộp muộn.
- **Thống kê lớp**: tổng hợp toàn lớp và bảng xếp hạng.
- Xuất bảng điểm ra `.xlsx`: `GET /api/teacher/assignments/{id}/export`.
- Sinh viên có trang "Kết quả học tập" của riêng mình.

### 9. Phần mạng (trọng tâm của môn Lập trình mạng)

**9.1 Realtime qua WebSocket (STOMP)**
- **Trạng thái online** (`PresenceService`):
  - Ghi nhận khi client kết nối hoặc ngắt (`SessionConnectedEvent` / `SessionDisconnectEvent`), lưu vào `ConcurrentHashMap` trong bộ nhớ kèm thời gian hoạt động cuối.
  - Giáo viên thấy chấm xanh "đang online" và "đang làm bài X".
- **Tiến độ live** (`LiveProgressPublisher`): khi sinh viên lưu nháp hoặc nộp bài, server phát sự kiện tới `/topic/assignments/{id}/progress`, ma trận tiến độ của giáo viên cập nhật ngay.
- **Chat**:
  - Kênh hỏi đáp riêng cho từng bài nộp, giữa giáo viên và sinh viên.
  - Kênh chung cho mỗi lớp.
  - Có hiển thị "đang gõ…" và trạng thái đã xem.
  - Lịch sử chat lưu vào DB.
- **Một tài khoản, một phiên** (`SessionGuard`): khi đăng nhập ở nơi mới, server thu hồi phiên cũ và gửi lệnh `FORCE_LOGOUT` tới client cũ qua WebSocket.
- **Heartbeat và kết nối lại**:
  - STOMP heartbeat 10 giây.
  - Client tự kết nối lại theo exponential backoff (1s, 2s, 4s… tối đa 30s).
  - Giao diện hiển thị thanh "Mất kết nối… đang thử lại".
- **Không mất thông báo khi rớt kết nối**:
  - Thông báo luôn được lưu vào DB trước khi đẩy qua WebSocket.
  - Khi client kết nối lại (ví dụ sau khi server khởi động lại), nó gọi `GET /api/notifications?after=<thời điểm cuối>` để lấy các thông báo bị lỡ.
  - Demo: khởi động lại backend trong lúc đang mở trang; client tự kết nối lại và hiện đủ thông báo.

**9.2 Chịu lỗi mạng và đồng bộ**
- **Lưu nháp khi mất mạng**:
  - Nháp luôn được lưu vào IndexedDB của trình duyệt.
  - Khi có mạng lại, client đồng bộ lên server. Server dùng `draft_version` (optimistic locking) để phát hiện xung đột. Nếu có xung đột, hiển thị cho sinh viên chọn giữ bản nào.
- **Đồng bộ giờ với server** (`GET /api/time`, kiểu NTP đơn giản):
  - Client đo thời gian khứ hồi (RTT) và tính độ lệch giờ so với server.
  - Đồng hồ đếm ngược hạn nộp chạy theo giờ server.
  - Server luôn là nơi quyết định nộp muộn hay không.

**9.3 Module TCP socket tự viết** (khớp với yêu cầu "server gen tài khoản" của đề)
- **Server** `TcpImportServer`:
  - Dùng `ServerSocket` ở cổng 9090, mỗi kết nối được xử lý bởi một luồng trong `ExecutorService` (thread pool).
  - Bật bằng cấu hình `tcp.enabled`.
- **Giao thức tự định nghĩa** (dạng text, đặc tả trong `docs/tcp-protocol.md`):
  - Khung tin: `AUTH <token>` → `IMPORT <classCode> <bytes>` + nội dung CSV → server trả từng dòng `PROGRESS n/total` → `DONE created=.. skipped=.. errors=..`.
  - Mã lỗi riêng: `ERR 401`, `ERR 400 <line>`, ...
- **Client** `admin-cli`: chương trình Java thuần (`Socket`, `DataOutputStream`), chạy dạng `java -jar admin-cli.jar import --host .. --class .. --file students.csv`, có thanh tiến trình.
- **Bản mở rộng**: hỗ trợ TLS bằng `SSLServerSocket`, và có thêm bản NIO (`Selector`) để so sánh hiệu năng với mô hình mỗi kết nối một luồng.

**9.4 Bảo mật khi truyền trên mạng**
- HTTPS/WSS qua nginx, tự chuyển HTTP sang HTTPS.
- Mật khẩu mã hoá bằng BCrypt.
- JWT gồm access token và refresh token xoay vòng (rotation).
- Giới hạn tần suất (lưu trong bộ nhớ): tối đa 5 lần đăng nhập sai trong 5 phút cho mỗi IP và mỗi tài khoản.
- `audit_logs` ghi lại IP của các thao tác quan trọng.
- Báo cáo có ảnh Wireshark so sánh: HTTP lộ mật khẩu, HTTPS thì không.

**9.5 Hiệu năng và giám sát**
- Script k6 mô phỏng 200 sinh viên nộp bài trong 5 phút cuối trước hạn, cùng với 500 kết nối WebSocket. Đo p95 latency và tỉ lệ lỗi, đưa kết quả vào báo cáo.
- Dùng Spring Actuator + Micrometer để xuất số liệu. Prometheus thu thập, Grafana hiển thị dashboard: số request/giây, số phiên WebSocket, số luồng đang hoạt động, thời gian phản hồi.
- Email được đưa vào hàng đợi `@Async` để request nộp bài không phải chờ server SMTP.

### 10. Tính năng mở rộng (tăng quy mô đề tài)
- **Chế độ thi tính giờ**:
  - Giáo viên bật "Chế độ thi" cho một phần. Khi sinh viên bấm Bắt đầu, server tạo `exam_session` với `ends_at` cố định.
  - Hết giờ thì server tự nộp bản nháp mới nhất.
  - Client gửi các sự kiện chuyển tab, dán (paste), mất kết nối qua WebSocket. Giáo viên xem được dòng thời gian (timeline) các sự kiện này.
- **Kiểm tra đạo văn nội bộ**: so độ giống nhau giữa các bài của cùng một phần (n-gram + Jaccard/cosine), chạy nền sau khi nộp. Bài vượt ngưỡng sẽ được gắn cờ cảnh báo cho giáo viên.
- **Gợi ý chấm bằng AI** (tuỳ chọn, bật bằng API key):
  - Gọi Claude API để gợi ý band từng tiêu chí và liệt kê lỗi ngữ pháp, từ vựng.
  - Gợi ý chỉ hiện cho giáo viên tham khảo. Giáo viên luôn là người quyết định điểm.
- **Ngân hàng đề**: lưu đề và rubric để dùng lại, gắn tag theo dạng đề (Task 1 kiểu Line/Bar/Map/Process, Task 2 kiểu Opinion/Discussion…).
- **Nhật ký hệ thống cho admin**: xem audit log, danh sách phiên đăng nhập, và buộc đăng xuất người dùng.
- **Giao diện responsive**: dùng được trên điện thoại. Demo qua mạng LAN để các bạn khác dùng điện thoại đăng nhập vào làm sinh viên.

## Phân quyền
- `@PreAuthorize` theo vai trò.
- Kiểm tra quyền sở hữu ở tầng service:
  - Giáo viên chỉ được truy cập lớp và bài của mình.
  - Sinh viên chỉ thấy bài đã PUBLISHED, đã tới `open_at`, và được giao cho mình (qua `findVisibleForStudent`).
  - Sinh viên chỉ thấy điểm khi grade ở trạng thái `RETURNED`.

## Thứ tự triển khai
Làm xong mức nào là có một bản hoàn chỉnh để demo ở mức đó, rồi mới sang mức tiếp theo.

**Mức 1: Lõi nghiệp vụ** (bắt buộc)
1. Khung dự án: Spring Boot + Vite, docker-compose (postgres, mailpit), Flyway V1, seed admin và mẫu rubric IELTS.
2. Đăng nhập JWT, đổi mật khẩu, layout frontend theo vai trò, admin quản lý giáo viên.
3. Lớp học và import sinh viên từ file (CSV/XLSX), xuất file danh sách tài khoản, gửi email.
4. Bài tập: các phần, rubric, checklist, giao bài, publish.
5. Sinh viên làm bài: lưu nháp tự động, nộp từng phần, version.
6. Chấm bài: checklist, điểm, nhận xét inline, trả bài, yêu cầu sửa. Lịch sử nộp và diff.
7. Thông báo: WebSocket, email, job nhắc hạn nộp.
8. Tiến độ, thống kê, xuất Excel.

**Mức 2: Trọng tâm mạng** (đây là phần ghi điểm của môn)

9. Realtime nâng cao: trạng thái online, tiến độ live, chat, một tài khoản một phiên, heartbeat và kết nối lại.
10. Đồng bộ giờ với server, lưu nháp khi mất mạng bằng IndexedDB, giải quyết xung đột bản nháp.
11. Module TCP: `TcpImportServer` và `admin-cli`, đặc tả giao thức.
12. Bảo mật: nginx HTTPS/WSS, rate limit trong bộ nhớ, audit log.
13. Docker hoá toàn bộ: postgres + mailpit + backend (monolith) + frontend + nginx reverse proxy. Prometheus và Grafana là tuỳ chọn.
14. Load test bằng k6, ghi kết quả vào `docs/`.

**Mức 3: Mở rộng** (làm theo thời gian còn lại, mỗi tính năng độc lập)

15. Chế độ thi tính giờ và theo dõi sự kiện chuyển tab, dán, mất kết nối.
16. Kiểm tra đạo văn nội bộ.
17. Ngân hàng đề và gắn tag.
18. Gợi ý chấm bằng AI.
19. TCP qua TLS và bản NIO.
20. README, dữ liệu demo (profile `demo`: 1 giáo viên, 2 lớp, 20 sinh viên, 2 bài tập), sơ đồ tuần tự, ảnh Wireshark cho báo cáo.

## Kiểm thử và xác minh
- **Unit test** (JUnit 5 + Mockito): `BandCalculator` (các trường hợp làm tròn), `StudentImportService` (file hợp lệ, trùng MSSV, thiếu cột), kiểm tra hạn nộp và nộp muộn, logic chọn người cần nhắc của `DeadlineReminderJob`.
- **Integration test**: `@SpringBootTest` + Testcontainers PostgreSQL (cần Docker), dùng MockMvc chạy luồng giáo viên tạo bài → sinh viên nộp → giáo viên chấm → sinh viên xem điểm. Có một test phân quyền: sinh viên không được giao bài thì nhận 403/404.
- **Frontend**: `npm run build` và `npm run lint` phải sạch. Thêm Vitest cho hook autosave và phần đếm số từ.
- **End-to-end thủ công**:
  1. `docker compose up --build` rồi mở http://localhost:3000.
  2. Admin tạo giáo viên.
  3. Giáo viên tạo lớp, import `samples/students.xlsx`, kiểm tra Mailpit (http://localhost:8025) đã nhận email tài khoản.
  4. Giáo viên tạo bài 2 phần (Task 1 + Task 2), giao cho lớp, publish.
  5. Đăng nhập sinh viên bằng MSSV, đổi mật khẩu, kiểm tra đã nhận thông báo realtime.
  6. Gõ bài, tải lại trang, kiểm tra bản nháp vẫn còn. Nộp Task 1.
  7. Giáo viên nhận thông báo, chấm bằng checklist, yêu cầu sửa.
  8. Sinh viên nộp version 2, xem diff.
  9. Giáo viên trả bài. Kiểm tra trang thống kê và file Excel xuất ra.
  10. Đặt hạn nộp còn dưới 2 giờ để kiểm tra email nhắc hạn.
- **Kiểm tra phần mạng**:
  - **Một phiên**: đăng nhập cùng tài khoản trên 2 trình duyệt; trình duyệt cũ phải bị đăng xuất ngay.
  - **Mất mạng**: bật Offline trong DevTools rồi gõ bài; bật mạng lại, nháp phải được đồng bộ. Mở 2 tab cùng sửa thì phải hiện hộp thoại xung đột.
  - **Kết nối lại**: `docker compose restart backend` trong lúc đang mở trang. Client phải tự kết nối lại và lấy được các thông báo phát ra trong lúc mất kết nối.
  - **TCP**: chạy `java -jar admin-cli.jar import ...` và xem tiến trình. Gửi token sai thì phải nhận `ERR 401`. Có unit test cho phần đọc khung tin của giao thức.
  - **Rate limit**: đăng nhập sai 6 lần thì phải nhận 429.
  - **Load test**: chạy `k6 run loadtest/submit-rush.js` với điều kiện p95 < 500ms, lỗi < 1%. Theo dõi trên Grafana tại http://localhost:3001.
  - **Wireshark**: bắt gói HTTP Upgrade của WebSocket và gói TLS để đưa vào báo cáo.
