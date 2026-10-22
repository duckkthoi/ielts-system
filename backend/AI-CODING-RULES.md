# Quy tắc code backend (`backend/`) — bắt buộc cho AI agent và người viết code

> Đọc file này **trước khi** viết hoặc sửa code trong `backend/`. Cuối file có checklist để tự kiểm tra trước khi bàn giao.

---

## 0. Bối cảnh và nguồn sự thật

- Dự án: hệ thống giao bài và chấm bài IELTS Writing, đồ án môn **Lập trình mạng**. Kiến trúc **monolith**: một ứng dụng Spring Boot chứa REST, WebSocket (STOMP), server TCP và các job định kỳ. **Không** tách microservice, **không** dùng Redis.
- **Tài liệu trong `docs/` là bản chuẩn.** Code phải khớp với tài liệu; tài liệu đổi thì code đổi theo, và ngược lại.

  | Cần gì | Xem |
  |---|---|
  | Chức năng `FR-xx`, quy tắc nghiệp vụ `BR-xx`, yêu cầu phi chức năng `NFR-xx` | `docs/01-tong-quan.md` |
  | Luồng nghiệp vụ, ngoại lệ | `docs/02-use-case.md` |
  | Thứ tự gọi giữa các thành phần | `docs/03-sequence.md` |
  | Bảng, cột, ràng buộc, lộ trình migration | `docs/04-erd.md` |
  | Endpoint, DTO, mã lỗi, kênh WebSocket, giao thức TCP | `docs/05-api.md` |
  | Trạng thái và điều kiện chuyển | `docs/06-trang-thai.md` |

- `PLAN.md` chỉ là kế hoạch ban đầu. Chỗ nào lệch với `docs/` thì **theo `docs/`**.
- Làm xong mỗi bước thì **thêm một mục vào `PROGRESS.md`** ở thư mục gốc: đã làm gì, file nào, chỗ nào khác so với tài liệu, kiểm tra bằng cách nào.

---

## 1. Công nghệ

| Thành phần | Phiên bản / ghi chú |
|---|---|
| Java | 21 (biên dịch `release 21`; máy dev có thể chạy JDK mới hơn) |
| Spring Boot | **4.0.x**: Web MVC, Security, Data JPA, WebSocket, Mail, Validation, Actuator |
| JSON | Boot 4 dùng **Jackson 3** (package `tools.jackson.*`). Annotation vẫn ở `com.fasterxml.jackson.annotation.*` |
| DB | PostgreSQL 16+ (máy dev đang dùng 18), Flyway |
| Thư viện khác | jjwt 0.12, Apache POI, Commons CSV, springdoc-openapi 3, MapStruct, Lombok |
| Build | Maven (`mvn` hoặc `./mvnw`) |

Không thêm thư viện mới khi chưa ghi lý do vào `PROGRESS.md`.

---

## 2. Cấu trúc package

Chia theo **module nghiệp vụ** (feature-first), không chia theo tầng kỹ thuật ở cấp cao nhất.

```
com.ltmtu.ielts
├─ config/          cấu hình Spring (Security, WebSocket, Async, Jpa, Clock, AppProperties)
├─ security/        JwtService, JwtAuthFilter, CurrentUser, StompAuthInterceptor
├─ common/          BaseEntity, ApiResponse, ErrorResponse, ApiException, GlobalExceptionHandler, PageResponse
├─ user/            tài khoản, phiên đăng nhập, xác thực, admin quản lý người dùng
├─ classroom/       lớp học, thành viên, import sinh viên
├─ assignment/      bài tập, phần, tiêu chí, checklist, rubric mẫu, giao bài
├─ submission/      bản nháp, nộp bài, version
├─ grading/         chấm điểm, nhận xét inline, BandCalculator
├─ notification/    thông báo, email, job nhắc hạn
├─ stats/           tiến độ, thống kê, xuất Excel
├─ realtime/        online, chat, đồng bộ giờ (Mức 2)
├─ tcpgateway/      server TCP import (Mức 2)
├─ exam/ · plagiarism/   (Mức 3)
```

Bên trong một module, các file đặt phẳng; chỉ tách thư mục con `dto/` khi có từ 5 DTO trở lên:
```
classroom/
├─ Classroom.java              entity
├─ ClassMember.java            entity
├─ ClassroomRepository.java
├─ ClassroomService.java
├─ StudentImportService.java
├─ TeacherClassroomController.java
└─ dto/  ClassroomRequest.java, ClassroomResponse.java, ImportReport.java …
```

- Module này **không** gọi thẳng repository của module khác. Cần dữ liệu thì gọi **service** của module đó.
- Không tạo các package chung chung kiểu `utils/` hay `helpers/` ở cấp cao nhất. Hàm tiện ích đặt trong module dùng nó, hoặc trong `common/` nếu từ 3 module trở lên cùng dùng.

---

## 3. Phân tầng

| Tầng | Được làm | Không được làm |
|---|---|---|
| **Controller** | Nhận request, `@Valid`, lấy user hiện tại, gọi **một** hàm service, bọc kết quả vào `ApiResponse` | Chứa logic nghiệp vụ, gọi repository, mở transaction, trả entity |
| **Service** | Toàn bộ logic nghiệp vụ, kiểm tra quyền sở hữu, `@Transactional`, phát sự kiện | Đọc `HttpServletRequest`, trả `ResponseEntity` |
| **Repository** | Spring Data JPA; `@Query` dùng JPQL; native SQL chỉ khi JPQL không làm được (vd `num_nonnulls`, `ON CONFLICT`) | Logic nghiệp vụ |
| **Entity** | Ánh xạ bảng, có thể có method nhỏ chỉ đụng tới chính trạng thái của nó (vd `isLocked()`) | Gọi service hay repository |

- `@Transactional` đặt ở **method của service**. Truy vấn chỉ đọc dùng `@Transactional(readOnly = true)`.
- Inject **qua constructor** (`@RequiredArgsConstructor` + field `private final`). Cấm `@Autowired` trên field.

---

## 4. API và DTO

- Đường dẫn, method, quyền và dạng dữ liệu **đúng như `docs/05-api.md`**. Thêm endpoint mới thì cập nhật file đó trong cùng bước.
- Mọi response bọc trong `ApiResponse`:
  ```java
  return ApiResponse.ok(classroomService.create(currentUser.id(), request));
  return ApiResponse.page(classroomService.search(currentUser.id(), keyword, pageable));
  ```
  Tạo mới trả `201` (`@ResponseStatus(HttpStatus.CREATED)`), còn lại trả `200`.
- DTO là **Java `record`**, hậu tố `Request` / `Response` / `Dto`. **Không bao giờ trả entity ra API.** Ánh xạ bằng MapStruct (`@Mapper`) hoặc hàm static `XxxResponse.from(entity)`.
- Validate bằng Jakarta Validation, **message tiếng Việt**:
  ```java
  public record ClassroomRequest(
      @NotBlank(message = "Vui lòng nhập mã lớp") @Size(max = 30) String code,
      @NotBlank(message = "Vui lòng nhập tên lớp") @Size(max = 150) String name,
      String description) {}
  ```
- Phân trang: nhận `Pageable` (`page` bắt đầu từ 0, `size` tối đa 100, đã cấu hình chung). Trả `ApiResponse.page(...)`.
- Thời gian trong DTO dùng kiểu `Instant`, serialize ra ISO 8601 có `Z`.
- **Request DTO không dùng kiểu nguyên thuỷ** (`boolean`, `int`…) cho trường client có thể bỏ trống. Jackson 3 trả lỗi 400 khi thiếu trường nguyên thuỷ trong record. Dùng `Boolean` / `Integer` và xử lý `null` (vd `Boolean.TRUE.equals(x)`).

---

## 5. Lỗi

- Lỗi nghiệp vụ: ném `ApiException` qua các hàm static, kèm **mã lỗi và message tiếng Việt theo bảng trong `docs/05-api.md`**:
  ```java
  throw ApiException.conflict("ALREADY_SUBMITTED", "Phần này đã được nộp");
  throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DEADLINE_PASSED", "Đã quá hạn nộp");
  ```
- **Không** trả `null`, `Optional` rỗng hay mã lỗi tự chế để báo lỗi cho controller.
- Tài nguyên không thuộc về người gọi (vd giáo viên mở lớp của giáo viên khác) → **404 `NOT_FOUND`**, không phải 403.
- Không bắt `Exception` chung chung trong service. Lỗi không mong muốn để `GlobalExceptionHandler` xử lý (log + trả 500).

---

## 6. Bảo mật và phân quyền

- Phân quyền theo vai trò ở **cấp class controller**:
  `@PreAuthorize("hasRole('TEACHER')")` cho `/api/teacher/**`, tương tự với `ADMIN` và `STUDENT`.
- Kiểm tra **quyền sở hữu ở service**, dựa trên id lấy từ token (`CurrentUser`), **không bao giờ** tin id người dùng do client gửi lên.
  Với sinh viên, mọi truy vấn bài tập đi qua `AssignmentRepository.findVisibleForStudent` (BR-04, BR-05).
- Sinh viên chỉ thấy điểm khi `grades.status = RETURNED` (BR-13). Kiểm tra trong service, không dựa vào frontend.
- Mật khẩu: BCrypt. Refresh token: chỉ lưu **SHA-256** (bảng `user_sessions`). Mật khẩu tạm khi import: chỉ ở trong bộ nhớ, **không ghi DB, không ghi log**.
- **Cấm log** mật khẩu, token, nội dung file import, nội dung bài làm.
- HTML do người dùng nhập (đề bài) phải được lọc (sanitize) trước khi lưu.

---

## 7. Entity và JPA

- Entity kế thừa `BaseEntity` (id, `createdAt`, `updatedAt`), trừ bảng nối có khoá kép (dùng `@EmbeddedId`).
- Enum lưu `@Enumerated(EnumType.STRING)`, khớp với `CHECK` trong DB.
- Thời gian dùng `Instant`. **Không** dùng `LocalDateTime` hay `Date`.
- Quan hệ:
  - `@ManyToOne(fetch = LAZY)` luôn luôn;
  - chỉ khai báo `@OneToMany` khi thực sự cần đi từ cha sang con (vd phần → tiêu chí);
  - **không dùng `cascade = REMOVE` của JPA**, để FK `ON DELETE CASCADE` của DB lo phần xoá.
  - **Ngoại lệ — aggregate:** với các bảng con mà vòng đời hoàn toàn thuộc về bảng cha, được dùng `cascade = ALL` + `orphanRemoval = true`. Ví dụ: bài tập → phần → tiêu chí → mục checklist; rubric mẫu → tiêu chí → mục. Khi thay toàn bộ danh sách con phải `clear()` rồi **flush** trước khi thêm mới, vì Hibernate chạy INSERT trước DELETE, sẽ vi phạm UNIQUE `order_no`.
- Lombok trên entity: chỉ `@Getter`, `@Setter`, `@NoArgsConstructor`. **Cấm `@Data`, `@EqualsAndHashCode`, `@ToString` mặc định** (gây lazy-load và lặp vô hạn).
- `SubmissionVersion` đánh dấu `@Immutable`: chỉ `INSERT` (NFR-11).
- Tránh N+1: dùng `@EntityGraph` hoặc `JOIN FETCH` cho danh sách; trang thống kê dùng query projection.
- **Tìm kiếm LIKE:**
  - từ khoá luôn qua `Pageables.likePattern()`;
  - mọi `LIKE` phải ghi **`LIKE :keyword ESCAPE '!'`**. Thiếu `ESCAPE` thì `_` và `%` trong từ khoá thành ký tự đại diện, tìm sai.
- **Sắp xếp:** luôn qua `Pageables.sanitize()` với whitelist trường (tránh 500 và lộ tên trường nội bộ).

---

## 8. Database và Flyway

- `spring.jpa.hibernate.ddl-auto = validate`. Schema **chỉ** thay đổi qua Flyway.
- **Không sửa migration đã chạy.** Muốn đổi thì tạo file mới `V{n}__mo_ta.sql`. Số thứ tự theo lộ trình trong `docs/04-erd.md` mục 2.
- Quy ước: `snake_case`, có `CHECK` cho enum, có FK, có index cho cột lọc hoặc join thường xuyên. Comment tiếng Việt cho các ràng buộc khó hiểu.
- Đổi schema thì cập nhật `docs/04-erd.md` trong cùng bước.

---

## 9. Thời gian

- **Luôn lấy giờ qua bean `Clock`**: `Instant.now(clock)`. Cấm gọi `Instant.now()` hay `System.currentTimeMillis()` trong code nghiệp vụ, để test hạn nộp, nộp muộn và nhắc hạn bằng `Clock.fixed(...)`.
- Hạn nộp và nộp muộn **chỉ tính ở server** (NFR-12). Hạn áp dụng theo BR-07: hạn sửa → hạn phần → hạn bài, cài trong `submission/DeadlineRules`.
- Bean `Clock` nhảy theo **micro giây**, bằng độ chính xác của PostgreSQL. Không tự tạo `Instant` có nano giây (vd `Instant.now()`) rồi trả ra API: giá trị sẽ lệch khi đọc lại từ DB.

---

## 10. Đồng thời và toàn vẹn

- Nộp bài: khoá dòng `submissions` bằng `@Lock(LockModeType.PESSIMISTIC_WRITE)` (tức `SELECT … FOR UPDATE`) và so `expectedVersion` với `current_version` (SD-07). Lệch thì trả 409 `VERSION_CONFLICT`.
- Lưu nháp (Mức 2): so `draftVersion`, lệch thì trả 409 `DRAFT_CONFLICT`.
- Chống gửi trùng: ràng buộc `UNIQUE` trong DB + `INSERT … ON CONFLICT DO NOTHING` (vd `reminder_log`). Không dựa vào việc "kiểm tra trước rồi mới chèn".
- Import: **mỗi dòng một transaction riêng** (BR-03). Dùng `TransactionTemplate`, hoặc service con có `@Transactional(propagation = REQUIRES_NEW)`.

---

## 11. Thông báo, email, WebSocket

- Tạo thông báo qua `NotificationService.notify` duy nhất. Thứ tự: **lưu DB trong transaction của nghiệp vụ → sau khi commit → đẩy WebSocket → gửi email**.
  - Module nghiệp vụ chỉ **phát sự kiện** (`AssignmentEvents`, `SubmissionEvents`, `GradingEvents`), mang sẵn dữ liệu hiển thị (tên bài, tên phần…), không gọi thẳng module thông báo.
  - `NotificationEventListener` nghe bằng `@TransactionalEventListener(phase = BEFORE_COMMIT)`: thông báo được lưu cùng transaction với nghiệp vụ, rollback thì không có thông báo "ma".
  - Đẩy WebSocket và email được `NotificationService` đăng ký chạy `afterCommit`. **Không** dùng `AFTER_COMMIT` để ghi DB: lúc đó transaction đã kết thúc, ghi thêm phải mở transaction mới và có thể mất thông báo nếu lỗi.
- Email gửi `@Async`. Lỗi email chỉ ghi log, không làm hỏng nghiệp vụ.
- Destination STOMP **đúng bảng trong `docs/05-api.md` phần B**. Gửi riêng từng người dùng `convertAndSendToUser(username, "/queue/…", payload)`.
- Quyền SUBSCRIBE / SEND kiểm tra trong `ChannelInterceptor`, không kiểm tra ở frontend.
- Thông báo lỡ khi mất kết nối: `GET /api/notifications?after=` (SD-09). Không có cơ chế nào khác.

---

## 12. Server TCP (`tcpgateway`)

- Giao thức **đúng `docs/05-api.md` phần C**: lệnh, phản hồi, mã lỗi, giới hạn, timeout.
- Chỉ dùng `java.net.ServerSocket` / `Socket` và `ExecutorService` cấu hình được. Khởi động và dừng theo `SmartLifecycle`. Mỗi socket `setSoTimeout`.
- Tái sử dụng `StudentImportService`, không viết lại logic import.
- Phần phân tích khung tin tách thành class thuần (không phụ thuộc Spring) để unit test.

---

## 13. Log

- Dùng `@Slf4j`. Mức log: `info` cho sự kiện nghiệp vụ quan trọng (đăng nhập, import, phát hành), `warn` cho tình huống bất thường xử lý được, `error` kèm stack trace cho lỗi không mong muốn.
- Có ngữ cảnh (id), không có dữ liệu nhạy cảm (xem mục 6). Ví dụ: `log.info("Import lớp {}: tạo {}, lỗi {}", classId, created, errors)`.

---

## 14. Kiểm thử

- **Unit test** (JUnit 5 + Mockito + AssertJ) cho logic thuần: `BandCalculator`, đọc file import, tính hạn áp dụng, phân tích khung TCP, logic chọn người cần nhắc.
- **Integration test** (`@SpringBootTest` + MockMvc) cho từng luồng API chính và **từng quy tắc phân quyền**. Chạy trên DB `ielts_test` (profile `test`); khi có Docker thì có thể chuyển sang Testcontainers.
- Mỗi quy tắc `BR-xx` được cài đặt phải có ít nhất **một test**, và ghi mã quy tắc trong `@DisplayName`:
  ```java
  @Test
  @DisplayName("BR-08: nộp sau hạn khi bài không cho nộp muộn → 422 DEADLINE_PASSED")
  void submitAfterDeadline_notAllowed_returns422() { … }
  ```
- Dùng `Clock` cố định trong test có liên quan tới thời gian.
- Ngoài test tự động, mỗi nhóm API có file `backend/http/<module>.http` để gọi thử bằng tay trong VS Code / IntelliJ.

---

## 15. Phong cách code

- Thụt lề bằng **tab** (giống code Spring Initializr sinh ra). Một file không quá **400 dòng**; quá thì tách.
- Tên tiếng Anh cho class, method, biến. **Comment và Javadoc tiếng Việt**, ngắn gọn, giải thích *vì sao* chứ không lặp lại code. Method service public có Javadoc ghi mã quy tắc liên quan (`BR-xx`).
- Không để code chết, code comment, `System.out.println`, hay TODO không có mã việc trong `PROGRESS.md`.
- Hằng số cấu hình (thời hạn, giới hạn, cổng) đặt trong `application.yml` → `AppProperties`, không hard-code.

---

## Checklist trước khi bàn giao

```
[ ] Endpoint, DTO, mã lỗi khớp docs/05-api.md (đã cập nhật docs nếu có thay đổi)
[ ] Schema đổi qua migration mới; docs/04-erd.md đã cập nhật
[ ] Controller mỏng; không trả entity; response bọc ApiResponse
[ ] Kiểm tra quyền sở hữu ở service, dùng id từ token; tài nguyên của người khác → 404
[ ] Không log mật khẩu / token / nội dung bài làm
[ ] Thời gian lấy qua Clock; kiểu Instant
[ ] Thông báo: lưu DB rồi đẩy sau khi commit
[ ] Có test cho từng BR-xx đã cài; mvn test chạy qua
[ ] Có file .http cho endpoint mới
[ ] Đã thêm mục vào PROGRESS.md
```
