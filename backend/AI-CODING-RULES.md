# Quy tắc code backend (`backend/`) — bắt buộc cho AI agent và người viết code

> Đọc [`../CLAUDE.md`](../CLAUDE.md) trước (quy trình làm từng bước, ràng buộc bảo mật). File này chỉ nói **viết code backend thế nào**.
> Quy ước trong file: **PHẢI** = bắt buộc · **KHÔNG** = cấm · *Nên* = mặc định, lệch thì ghi lý do vào PROGRESS.
> Mỗi quy tắc đều đã có ví dụ thật trong code — mở file được nêu để làm theo, đừng tự nghĩ kiểu mới.

---

## 1. Bối cảnh

- Hệ thống giao bài / chấm bài IELTS Writing, đồ án môn Lập trình mạng. **Monolith**: một ứng dụng Spring Boot chứa REST, WebSocket STOMP, server TCP, job định kỳ. KHÔNG tách microservice, KHÔNG dùng Redis hay broker ngoài.
- **`docs/` là bản chuẩn.** Code PHẢI khớp tài liệu; đổi một bên thì đổi bên kia trong cùng bước.

| Cần gì | Xem |
|---|---|
| FR / NFR / quy tắc nghiệp vụ BR-xx | `docs/01-tong-quan.md` (BR chấm bài: `docs/08` mục 5.1) |
| Luồng, ngoại lệ | `docs/02-use-case.md`, `docs/03-sequence.md` |
| Bảng, cột, lộ trình migration | `docs/04-erd.md` |
| Endpoint, DTO, mã lỗi, WebSocket, TCP | `docs/05-api.md` |
| Trạng thái và điều kiện chuyển | `docs/06-trang-thai.md` |
| Cách chấm và tính band IELTS | `docs/08-nghiep-vu-cham-ielts.md` |

## 2. Công nghệ — dùng đúng phiên bản này

| Thành phần | Ghi chú quan trọng |
|---|---|
| Java 21 (`release 21`) | Được dùng record, switch biểu thức, `List.getFirst()` |
| Spring Boot **4.0** | Spring 7, Security 7, Hibernate 7. `HttpStatus.UNPROCESSABLE_CONTENT` (không phải `UNPROCESSABLE_ENTITY`) |
| JSON = **Jackson 3** | Package `tools.jackson.*`; annotation vẫn `com.fasterxml.jackson.annotation.*` |
| PostgreSQL 18, Flyway | `ddl-auto = validate` |
| Khác | jjwt 0.12, Apache POI, Commons CSV, jsoup (lọc HTML), springdoc, Lombok |

KHÔNG thêm thư viện mới khi chưa ghi lý do vào `PROGRESS.md`. (MapStruct có trong `pom.xml` nhưng chưa dùng — ánh xạ bằng hàm static, xem mục 5.)

---

## 3. Module và phụ thuộc

```
com.ltmtu.ielts
├─ config/        cấu hình Spring: Security, WebSocket, Async + Scheduling, Clock, AppProperties
├─ security/      JwtService, AccessTokenVerifier, JwtAuthFilter, StompAuthInterceptor, AuthPrincipal
├─ common/        BaseEntity, ApiResponse, ErrorResponse, ApiException, GlobalExceptionHandler, Pageables, HtmlSanitizer…
├─ user/          tài khoản, phiên đăng nhập, xác thực, admin quản lý người dùng
├─ classroom/     lớp, thành viên, import sinh viên
├─ assignment/    bài tập, phần, tiêu chí, checklist, rubric mẫu, giao bài, mở bài, nhắc thủ công
├─ submission/    nháp, nộp, version
├─ grading/       chấm điểm, mã lỗi, ngân hàng nhận xét, BandCalculator, kết quả phía sinh viên
├─ notification/  thông báo, email, WebSocket push, job nhắc hạn
├─ stats/         tiến độ, thống kê, xuất Excel (chỉ đọc)
├─ audit/        nhật ký hệ thống (AuditService, RequestIp) — tầng dưới cùng
├─ tcpgateway/   server TCP import sinh viên (Mức 2)
├─ realtime/     một phiên + FORCE_LOGOUT, online, tiến độ live (Mức 2)
├─ chat/         chat theo bài nộp / lớp (Mức 2)
├─ demo/        dữ liệu mẫu khi bật profile demo (bước 20, lớp kiểm thử tải bước 14) — tầng trên cùng
├─ exam/ · plagiarism/ (Mức 3)
```

**Hướng phụ thuộc — PHẢI giữ một chiều, KHÔNG được vòng:**
```
common, audit  ←  (mọi module đều dùng được)
user ← classroom ← assignment ← submission ← grading ← notification
                                                  ↖ stats (chỉ đọc), tcpgateway, realtime, chat
```
Module bên phải được dùng module bên trái; module bên trái KHÔNG import module bên phải. Cần báo ngược lại thì **phát sự kiện** (mục 10). `demo` nằm trên tất cả: chỉ gọi service "cửa" / service nghiệp vụ (tạo lớp, tạo bài, phát hành, nộp bài) để dữ liệu mẫu đi qua đúng các kiểm tra; tài khoản mẫu (mật khẩu biết trước, không bắt đổi) tạo qua `user/DemoAccountService`. KHÔNG module nào gọi vào `demo`.

**Gọi sang module khác — chỉ qua service "cửa", KHÔNG gọi repository của module khác:**

| Cần | Dùng |
|---|---|
| Người dùng theo id / mã SV | `user/UserQueryService` |
| Lớp của giáo viên | `classroom/ClassroomService.requireOwnedClassrooms` |
| Bài **của giáo viên** (BR-15, khác chủ → 404) | `assignment/TeacherAssignmentAccess` |
| Bài / phần **sinh viên được thấy** (BR-04, BR-05) | `assignment/StudentAssignmentAccess` |
| Bài làm của chính sinh viên | `submission/StudentSubmissionAccess` |
| Bài nộp phía giáo viên (sở hữu, khoá dòng, chuyển trạng thái) | `submission/TeacherSubmissionAccess` |
| Tạo thông báo | `notification/NotificationService.notify` (thường qua sự kiện, không gọi thẳng) |

Ngoại lệ được phép: **truy vấn chỉ-đọc** viết bằng SQL/JPQL trong repository của module mình có thể đọc bảng của module khác (vd `AssignmentRepository.progressCounts`, `ReminderLogRepository.findCandidates`, truy vấn tổng hợp bằng `NamedParameterJdbcTemplate` trong `stats/ProgressService`). KHÔNG ghi chéo module. Kiểm tra sở hữu vẫn đi qua service cửa trước khi chạy truy vấn đó.

Bên trong module: file đặt phẳng; tách `dto/` khi có từ 5 DTO. KHÔNG tạo package chung kiểu `utils/`, `helpers/`.

---

## 4. Phân tầng

| Tầng | PHẢI | KHÔNG |
|---|---|---|
| **Controller** | Nhận request, `@Valid`, lấy `@AuthenticationPrincipal AuthPrincipal me`, gọi **đúng một** hàm service, bọc `ApiResponse` | Logic nghiệp vụ, gọi repository, `@Transactional`, trả entity |
| **Service** | Toàn bộ nghiệp vụ, kiểm tra sở hữu, `@Transactional`, phát sự kiện. Method public có Javadoc ghi mã BR / UC | Đọc `HttpServletRequest`, trả `ResponseEntity` |
| **Class quy tắc thuần** | Logic tính toán không cần DB, để unit test: `BandCalculator`, `GradingRules`, `GradeUpdater`, `DeadlineRules`, `WordCounter`, `RubricRules` | Phụ thuộc Spring, gọi repository |
| **Repository** | Spring Data JPA, `@Query` JPQL; native SQL khi JPQL không làm được (CTE, UNION, `ON CONFLICT`, `num_nonnulls`) | Logic nghiệp vụ |
| **Entity** | Ánh xạ bảng; method nhỏ chỉ đọc trạng thái của chính nó (`isReturned()`, `isSystem()`) | Gọi service / repository |

- `@Transactional` đặt ở method service; chỉ đọc thì `@Transactional(readOnly = true)`. Service "cửa" có hàm chỉ được gọi bên trong transaction của người gọi thì dùng `propagation = MANDATORY` (xem `TeacherSubmissionAccess`).
- Inject qua constructor (`@RequiredArgsConstructor` + `private final`). KHÔNG `@Autowired` trên field (trừ trong test). Cần `@Lazy` / `@Qualifier` thì viết constructor tay (xem `WebSocketConfig`).

---

## 5. API và DTO

- Đường dẫn, method, quyền, dạng dữ liệu PHẢI đúng `docs/05-api.md`. Thêm / đổi endpoint thì cập nhật file đó và thêm request vào `backend/http/<module>.http`.
- Response luôn bọc `ApiResponse`: `ApiResponse.ok(data)`, `ApiResponse.ok(data, "Đã lưu…")`, `ApiResponse.page(page)`, `ApiResponse.message("Đã xoá…")`. Tạo mới trả `201` (`@ResponseStatus(HttpStatus.CREATED)`).
- DTO là **record**, gom theo nhóm trong một class `XxxDtos` (vd `grading/dto/GradingDtos.java`). KHÔNG trả entity ra API. Ánh xạ bằng `static from(entity)` hoặc class `XxxMapper` có hàm static.
- **Request record KHÔNG dùng kiểu nguyên thuỷ** cho trường client có thể bỏ trống: Jackson 3 báo 400 khi thiếu. Dùng `Integer`, `Boolean`, xử lý null (`Boolean.TRUE.equals(x)`).
- Validate bằng Jakarta Validation, **message tiếng Việt**: `@NotBlank(message = "Vui lòng nhập mã lớp")`.
- Thời gian trong DTO là `Instant` (ISO 8601, có `Z`). Số điểm / band là `BigDecimal`.
- Phân trang: nhận `Pageable`, sort qua `Pageables.sanitize(pageable, whitelist, defaultSort)`. Query có thứ tự cố định (vd hàng đợi chấm) thì bỏ sort của client: `PageRequest.of(page, size)`.
- PUT = **thay toàn bộ** (phần không gửi coi như bỏ). PATCH = chỉ trường khác null.

---

## 6. Lỗi — chọn mã HTTP theo bảng này

Ném `ApiException` (hàm static `badRequest`, `forbidden`, `notFound`, `conflict`, hoặc `new ApiException(status, code, message)`), message tiếng Việt hiển thị thẳng cho người dùng. Mã `code` PHẢI có trong bảng mã lỗi `docs/05` A.1 (mã mới thì thêm vào đó).

| Tình huống | HTTP · code | Ví dụ thật |
|---|---|---|
| Dữ liệu gửi lên sai: thiếu trường, sai định dạng, id tham chiếu không hợp lệ | `400 VALIDATION_ERROR` | điểm sai bước; `classId` không phải lớp của mình |
| Chưa đăng nhập / token sai | `401` | do `JwtAuthFilter` |
| Sai vai trò; hoặc tài nguyên **thấy được nhưng không được sửa** | `403 FORBIDDEN` | sửa mã lỗi của hệ thống |
| Không tồn tại, hoặc **thuộc người khác** | `404 NOT_FOUND` | giáo viên mở bài của giáo viên khác — KHÔNG dùng 403, để không lộ tài nguyên tồn tại |
| Sai trạng thái / xung đột | `409` `INVALID_STATE` · `VERSION_CONFLICT` · `ALREADY_SUBMITTED` · `DUPLICATE` | trả bài khi chưa chấm; nộp trùng |
| Đúng định dạng nhưng vi phạm quy tắc nghiệp vụ theo giá trị | `422` `DEADLINE_PASSED` · `REVISION_LIMIT` · `SCORE_LIMITED` | điểm vượt giới hạn checklist |
| Gọi quá nhiều lần trong thời gian ngắn | `429 RATE_LIMITED` | đăng nhập sai 6 lần / 5 phút; nhắc thủ công lần 4 / 10 phút |

- KHÔNG trả `null` / `Optional` rỗng để báo lỗi cho controller. KHÔNG bắt `Exception` chung trong service — để `GlobalExceptionHandler` lo (log + 500).
- Lỗi ràng buộc UNIQUE do đua request thì bắt `DataIntegrityViolationException` ở đúng chỗ lưu và đổi thành 409 (xem `StudentSubmissionService.saveNew`).

---

## 7. Bảo mật

- Phân quyền theo vai trò ở **class controller**: `@PreAuthorize("hasRole('TEACHER')")` (`/api/teacher/**`), tương tự ADMIN, STUDENT. API dùng chung mọi vai trò (vd `/api/notifications`) chỉ cần đăng nhập.
- Kiểm tra **sở hữu ở service**, bằng `me.id()` lấy từ token — KHÔNG tin id người dùng client gửi.
- Phía sinh viên, mọi truy cập bài tập PHẢI qua `StudentAssignmentAccess` / `StudentSubmissionAccess` (BR-04, BR-05). Sinh viên chỉ thấy điểm khi `grades.status = RETURNED` (BR-13) — kiểm tra ở service.
- Xác thực token dùng chung `AccessTokenVerifier` cho REST và WebSocket: kiểm tra chữ ký, hạn, **phiên chưa bị thu hồi**, tài khoản còn hoạt động. Tài khoản `mustChangePassword` chỉ gọi được `/api/auth/**` và không CONNECT WebSocket được.
- **Nhật ký (audit, bước 12):** thao tác quan trọng gọi `AuditService.record(actorId, AuditAction.X, targetType, targetId, detail)` — ghi **cùng transaction** (rollback thì không có nhật ký "ma"), IP lấy từ `RequestIp` (filter), service không đọc `HttpServletRequest`. Việc thất bại vẫn phải lưu (đăng nhập sai, bị chặn) hoặc ngoài request HTTP (TCP) dùng `recordAlone(…, ip, …)` (transaction riêng). `detail` chỉ chứa số liệu tóm tắt — KHÔNG mật khẩu, token, mã SV / tên trong file import, nội dung bài.
- **Giới hạn tần suất (bước 12):** dùng `common/SlidingWindowLimiter` (trong bộ nhớ, theo `Clock`), bọc trong một `@Component` riêng có hàm `clear()` (vd `LoginThrottle`, `RemindThrottle`); lớp nền test gọi `clear()` trước mỗi test. Vượt giới hạn → `429 RATE_LIMITED`, message ghi số phút phải chờ. Cấu hình ở `app.rate-limit`.
- Mật khẩu: BCrypt. Refresh token: chỉ lưu SHA-256. HTML người dùng nhập (đề bài) PHẢI qua `HtmlSanitizer`. Upload: nhận diện loại file theo byte đầu (`FileStorageService`).
- KHÔNG log mật khẩu, token, nội dung file import, nội dung bài làm. Log chỉ id.

---

## 8. Entity, JPA, truy vấn

- Entity kế thừa `BaseEntity` (id, `createdAt`, `updatedAt`) khi bảng có đủ 3 cột; bảng chỉ có `created_at` thì tự khai báo và **set giờ bằng `Clock`** (xem `Notification`, `InlineComment`). Bảng khoá kép dùng `@EmbeddedId` + `@MapsId` (xem `ClassMember`, `GradeCriterionScore`).
- Enum lưu `@Enumerated(EnumType.STRING)`, khớp `CHECK` trong DB. Nhóm enum của module gom vào `XxxEnums` (vd `AssignmentEnums`, `GradingEnums`).
- Quan hệ: `@ManyToOne(fetch = LAZY)` luôn luôn. `@OneToMany` chỉ khi cần đi cha → con.
- KHÔNG dùng `cascade = REMOVE`; để FK `ON DELETE CASCADE` của DB xoá. **Ngoại lệ aggregate** (con sống chết theo cha): `cascade = ALL, orphanRemoval = true` — bài tập → phần → tiêu chí → checklist; kết quả chấm → điểm / tick / nhận xét inline. Thay toàn bộ danh sách con có UNIQUE `order_no` thì PHẢI `clear()` rồi `saveAndFlush` trước khi thêm mới (Hibernate chạy INSERT trước DELETE).
- Lombok trên entity chỉ `@Getter`, `@Setter`, `@NoArgsConstructor`. KHÔNG `@Data`, `@ToString`, `@EqualsAndHashCode` (trừ class id nhúng).
- `SubmissionVersion` là `@Immutable`: chỉ INSERT, không bao giờ sửa (NFR-11) — offset nhận xét inline dựa vào điều này.
- **Tránh N+1:** danh sách dùng `JOIN FETCH` / truy vấn gộp theo danh sách id (`findByVersionIdIn`, `findLatest`) / projection. KHÔNG gọi repository trong vòng lặp qua từng dòng của một trang.
- **LIKE:** từ khoá luôn qua `Pageables.likePattern()` và query PHẢI viết `LIKE :keyword ESCAPE '!'`.
- **Tham số null kiểu thời gian** trong JPQL: viết `CAST(:after AS Instant) IS NULL OR …` (PostgreSQL không đoán được kiểu của tham số null).
- Native SQL trả nhiều cột: dùng interface projection có alias trùng tên getter (xem `ReminderLogRepository.Candidate`).

## 9. Database và Flyway

- Schema chỉ đổi qua Flyway. **KHÔNG sửa migration đã chạy** — tạo file mới, số thứ tự theo lộ trình `docs/04` mục 2 (V6 = Mức 2, V7 = Mức 3).
- `snake_case`; enum có `CHECK`; có FK; có index cho cột lọc / join thường xuyên; comment tiếng Việt cho ràng buộc khó hiểu. Dữ liệu mẫu của hệ thống seed trong migration (vd mã lỗi, nhận xét mẫu ở V5).
- Đổi schema thì cập nhật `docs/04` (lộ trình + mô tả cột) trong cùng bước. Entity mới cho bảng mới viết ngay trong bước đó để `validate` kiểm tra luôn.

---

## 10. Thời gian, đồng thời, sự kiện

**Thời gian**
- Lấy giờ qua bean `Clock` (`clock.instant()`). KHÔNG `Instant.now()`, `System.currentTimeMillis()` trong code nghiệp vụ.
- Bean `Clock` nhảy theo **micro giây** (bằng độ chính xác PostgreSQL); KHÔNG tự tạo `Instant` có nano giây rồi trả ra API.
- Hạn nộp, nộp muộn chỉ tính ở server (NFR-12) qua `DeadlineRules` (BR-07: hạn sửa → hạn phần → hạn bài). Hiển thị giờ trong thông báo / email theo `app.zone` (giờ Việt Nam), lưu luôn là UTC.

**Đồng thời — chọn đúng mẫu, KHÔNG "kiểm tra trước rồi mới làm":**

| Tình huống | Mẫu | Ví dụ |
|---|---|---|
| Nhiều request sửa cùng một dòng (nộp bài, lưu chấm, trả bài) | `@Lock(PESSIMISTIC_WRITE)` = `SELECT … FOR UPDATE`, **khoá trước rồi mới đọc**, + so `expectedVersion` | `SubmissionRepository.findForUpdate`, `TeacherSubmissionAccess.requireOwnedForUpdate` |
| Chỉ một lần chạy được làm việc X | `UPDATE … WHERE cờ IS NULL`, xem số dòng cập nhật | `AssignmentRepository.claimOpenNotification` |
| Ghi "đã làm" chống trùng | Khoá chính + `INSERT … ON CONFLICT DO NOTHING`, chỉ xử lý khi chèn được | `ReminderLogRepository.insertIfAbsent` |
| Lần tạo đầu tiên chạy song song | UNIQUE trong DB + bắt `DataIntegrityViolationException` → 409 | `StudentSubmissionService.saveNew` |
| Import nhiều dòng | Mỗi dòng một transaction (`TransactionTemplate` / `REQUIRES_NEW`) | `StudentImportService` |

**Sự kiện và thông báo** (SD-05, `notification/`)
1. Service nghiệp vụ chỉ `events.publishEvent(new XxxEvents.Yyy(…))`, sự kiện **mang sẵn dữ liệu hiển thị** (tên bài, tên phần, tên sinh viên) để module thông báo không phải đọc ngược.
2. `NotificationEventListener` nghe bằng `@TransactionalEventListener(phase = BEFORE_COMMIT)` → gọi `NotificationService.notify`: thông báo được **lưu trong cùng transaction** — rollback thì không có thông báo "ma".
3. `NotificationService` đăng ký `afterCommit` để **sau commit** mới đẩy WebSocket (`convertAndSendToUser(username, "/queue/notifications", dto)`) rồi gửi email `@Async`. Lỗi đẩy / email chỉ ghi log.
- KHÔNG ghi DB trong listener `AFTER_COMMIT` (transaction đã đóng, dễ mất dữ liệu).
- Kênh STOMP PHẢI đúng bảng `docs/05` phần B. Quyền SUBSCRIBE / SEND: `StompAuthInterceptor` hỏi các bean `StompAccessRule`. Kênh mới PHẢI có luật (một class `…ChannelRule` trong module của kênh, xem `realtime/AssignmentChannelRule`, `chat/ChatChannelRule`); không luật nào nhận → từ chối. Luật KHÔNG được phụ thuộc (trực tiếp hay gián tiếp) `SimpMessagingTemplate` — luật được tạo cùng cấu hình WebSocket nên sẽ vòng phụ thuộc bean; chỉ dùng service / truy vấn đọc.
- Việc chỉ đẩy tin (không ghi DB) sau một thao tác: `@TransactionalEventListener(phase = AFTER_COMMIT)` (xem `realtime/ProgressPushListener`, `SessionKickListener`). Trạng thái tạm (online, phần đang mở) giữ trong bộ nhớ (`PresenceService`), không lưu DB.
- Thu hồi nhiều phiên của một người PHẢI qua `user/SessionRevoker` (để thiết bị đang mở nhận `FORCE_LOGOUT`).
- Client mất kết nối lấy thông báo bị lỡ bằng `GET /api/notifications?after=` — không có cơ chế nào khác.

**Job định kỳ**
- Lớp lập lịch (`notification/ScheduledJobs`) **chỉ gọi service**, không chứa logic; bật / tắt bằng `app.reminders.enabled`. Logic job nằm trong service gọi được trực tiếp từ test với đồng hồ giả.
- Job PHẢI chạy đúng khi nhiều lượt chồng nhau (dùng mẫu chống trùng ở trên) và chia transaction theo nhóm nhỏ, để lỗi một nhóm không làm hỏng cả lượt.

**Server TCP** (`tcpgateway`, đã cài ở bước 11): giao thức đúng `docs/05` phần C; chỉ `ServerSocket` / `Socket` + `ExecutorService` cấu hình được (`app.tcp`); giới hạn số phiên bằng `Semaphore`, hết chỗ → `ERR 503` ngay; khởi động / dừng bằng `SmartLifecycle`; mỗi socket `setSoTimeout`; tái dùng `StudentImportService` (`requireImportableClass`, `importCsv`); phân tích lệnh (`TcpCommand`) và đọc khung tin (`FrameReader`) là class thuần để unit test. Socket phải còn mở khi gửi `ERR` trong khối `catch` — không bọc socket bằng try-with-resources ở chỗ đó.

---

## 11. Cấu hình, log, phong cách

- Hằng số cấu hình (thời hạn, giới hạn, cổng, chu kỳ) đặt trong `application.yml` → record `AppProperties`. Giá trị khác nhau theo môi trường đọc từ biến môi trường có mặc định (`${JWT_SECRET:…}`); profile `docker` (`application-docker.yml`) chỉ đổi tên host và bật `forward-headers-strategy` — thêm biến mới thì thêm cả vào `docker-compose.yml` và `.env.example` ở thư mục gốc. Thêm trường vào `AppProperties` thì sửa cả chỗ tạo tay trong `JwtServiceTest`. Profile test tắt những gì chạy nền (`app.reminders.enabled: false`).
- `@Slf4j`: `info` cho sự kiện nghiệp vụ (đăng nhập, phát hành, trả bài), `warn` cho bất thường xử lý được, `error` + stack trace cho lỗi không mong muốn. Log có id, không có dữ liệu nhạy cảm.
- Thụt lề **tab**. File ≤ 400 dòng (quá thì tách class quy tắc / mapper). Tên tiếng Anh; **comment và Javadoc tiếng Việt**, giải thích *vì sao*, không lặp lại code. KHÔNG để code chết, code comment, `System.out.println`, TODO không có mã việc trong PROGRESS.
- Viết giống code xung quanh: đặt tên, độ dày comment, cách chia section (`// ─── Tiêu đề ───`).

---

## 12. Kiểm thử

| Loại | Khi nào | Cách viết |
|---|---|---|
| Unit test | Mọi class quy tắc thuần | JUnit 5 + AssertJ, `@ParameterizedTest` cho bảng ca (xem `BandCalculatorTest`, `WordCounterTest`) |
| Integration test | Mỗi luồng API chính và **mỗi luật phân quyền** | Kế thừa `support/IntegrationTest` (MockMvc, DB `ielts_test`, `MutableClock`, `CapturingMailSender`) |
| WebSocket | Kênh và luật quyền STOMP | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `WebSocketStompClient` thật (xem `WebSocketIntegrationTest`) |
| Đồng thời | Mọi chỗ dùng mẫu chống trùng ở mục 10 | 2 luồng + `CountDownLatch` bắn cùng lúc, kiểm tra đúng 1 thành công (xem `StudentSubmissionIntegrationTest`, `ReminderJobIntegrationTest`) |

- Mỗi quy tắc `BR-xx` đã cài có ít nhất một test, ghi mã trong `@DisplayName("BR-08: …")`.
- **DB test dùng chung giữa các lần chạy**: mỗi test tự tạo dữ liệu riêng (`newUser(...)` sinh tên duy nhất); KHÔNG đếm tổng số bản ghi trong bảng; kiểm tra theo id / người dùng của chính test.
- Thời gian: dùng `clock.advance(...)`; sau khi tua giờ thì lấy lại access token (token cũ có thể đã hết hạn theo đồng hồ giả).
- Email chạy nền: kiểm tra bằng `mail.awaitMatching(...)`.
- Dữ liệu mẫu phải hợp quy tắc: bài ≤ 20 từ thì mọi tiêu chí tối đa Band 1 (BR-18) — cho điểm cao hơn sẽ bị 422.
- Ngoài test tự động, mỗi nhóm API có file `backend/http/<module>.http` để gọi thử bằng tay.
- `mvn test` PHẢI pass toàn bộ trước khi báo xong. Gặp `NoClassDefFoundError` / `ClassNotFoundException` với lớp của dự án → do IDE và Maven cùng ghi `target/`: chạy `mvn clean test`.

---

## 13. Lỗi đã gặp → quy tắc rút ra

| Đã gặp | Quy tắc |
|---|---|
| `savedAt` trả về lệch với giá trị đọc lại từ DB (nano vs micro giây) | Clock nhảy micro giây (mục 10) |
| Tìm kiếm có `_` / `%` trả kết quả sai | `likePattern` + `ESCAPE '!'` (mục 8) |
| Thiếu trường `boolean` trong body → 400 khó hiểu | Request dùng `Boolean` / `Integer` (mục 5) |
| Thay danh sách phần bài → vi phạm UNIQUE `order_no` | `clear()` + `saveAndFlush` trước khi thêm (mục 8) |
| `overall_score NUMERIC(3,1)` làm 6.25 thành 6.3 | Lưu band chưa làm tròn (`NUMERIC(8,6)`), chỉ làm tròn khi hiển thị |
| Email chậm ~5 giây mỗi thư trên Windows | Đặt `mail.smtp.localhost` trong `application.yml` |
| Kết quả chấm phía sinh viên đặt ở module bài nộp sẽ gây phụ thuộc vòng | Đặt ở module chấm bài, giữ đường dẫn API cũ (mục 3) |
| Ghi thông báo sau commit có thể mất thông báo | Lưu `BEFORE_COMMIT`, đẩy `afterCommit` (mục 10) |
| Đọc trạng thái rồi mới khoá → đọc phải dữ liệu cũ | Khoá trước rồi mới đọc (mục 10) |

---

## Checklist trước khi bàn giao (backend)

```
[ ] Endpoint, DTO, mã lỗi khớp docs/05 (đã cập nhật nếu đổi); có request trong backend/http/
[ ] Schema đổi qua migration MỚI; docs/04 đã cập nhật; entity validate được
[ ] Không phụ thuộc vòng giữa module; gọi module khác qua service "cửa" hoặc sự kiện
[ ] Controller mỏng; không trả entity; response bọc ApiResponse; mã HTTP theo bảng mục 6
[ ] Kiểm tra sở hữu ở service bằng id từ token; tài nguyên của người khác → 404
[ ] Chỗ có thể chạy đồng thời dùng mẫu chống trùng ở mục 10, có test đồng thời
[ ] Thời gian qua Clock; kiểu Instant; thông báo lưu trong transaction, đẩy sau commit
[ ] Không log mật khẩu / token / nội dung bài làm
[ ] Test cho từng BR-xx đã cài; mvn test pass toàn bộ (ghi số lượt vào PROGRESS)
[ ] PROGRESS.md đã có mục mới; báo cáo xong thì dừng chờ xác nhận
```
