# 03. Sơ đồ tuần tự

> Mô tả cách các thành phần trao đổi với nhau trong những luồng chính. Tham chiếu use case `UC-xx` trong [02-use-case.md](02-use-case.md), quy tắc `BR-xx` và yêu cầu `NFR-xx` trong [01-tong-quan.md](01-tong-quan.md).
> Endpoint và kênh WebSocket ở đây khớp với đặc tả API (file 05).

**Quy ước tên thành phần**

| Tên trong sơ đồ | Là gì |
|---|---|
| Trình duyệt / Frontend | Ứng dụng Next.js chạy trên trình duyệt của người dùng |
| Backend | Ứng dụng Spring Boot (monolith). Khi cần, sơ đồ tách ra từng lớp bên trong: `Controller`, `Service`, `JwtService`… |
| STOMP broker | Simple broker của Spring nằm **trong** backend, quản lý các kết nối WebSocket |
| DB | PostgreSQL |
| SMTP | Máy chủ email (Mailpit khi phát triển) |

---

## Danh sách sơ đồ

| Mã | Luồng | Use case | Mức |
|---|---|---|---|
| SD-01 | Đăng nhập và mở kết nối WebSocket | UC-01 | 1 (một phiên: 2) |
| SD-02 | Tự làm mới access token khi hết hạn | UC-01 | 1 |
| SD-03 | Import sinh viên từ file (HTTP) | UC-11 | 1 |
| SD-04 | Import sinh viên qua TCP (admin-cli) | UC-12 | 2 |
| SD-05 | Phát hành bài và gửi thông báo | UC-14, UC-04 | 1 |
| SD-06 | Tự lưu nháp, mất mạng và xung đột | UC-23 | 1 (offline: 2) |
| SD-07 | Nộp bài một phần | UC-24 | 1 |
| SD-08 | Chấm bài, trả bài / yêu cầu sửa | UC-17, UC-18 | 1 |
| SD-09 | Vòng đời WebSocket: rớt kết nối, kết nối lại, lấy thông báo bị lỡ | UC-04 | 1 |
| SD-10 | Job nhắc hạn nộp | UC-30 | 1 |
| SD-11 | Đồng bộ giờ với server | UC-23 | 2 |
| SD-12 | Chat realtime | UC-29 | 2 |
| SD-13 | Chế độ thi và tự nộp khi hết giờ | UC-27, UC-31 | 3 |

---

## SD-01 · Đăng nhập và mở kết nối WebSocket

```mermaid
sequenceDiagram
    autonumber
    actor U as Người dùng
    participant FE as Frontend
    participant C as AuthController
    participant S as AuthService
    participant J as JwtService
    participant DB as DB
    participant WS as STOMP broker

    U->>FE: Nhập tên đăng nhập, mật khẩu
    FE->>C: POST /api/auth/login
    C->>S: login(username, password, ip)
    S->>S: LoginThrottle: IP hoặc tài khoản đã sai ≥ 5 lần / 5 phút?
    S->>DB: Tìm user theo username
    DB-->>S: User (password_hash, active, role)
    alt Đã sai quá nhiều lần (Mức 2)
        S->>DB: audit_logs LOGIN_BLOCKED (transaction riêng)
        S-->>FE: 429 "Bạn đã đăng nhập sai quá nhiều lần…"
    else Không tồn tại / sai mật khẩu
        S->>S: Tăng bộ đếm sai của IP và tài khoản
        S->>DB: audit_logs LOGIN_FAILED (transaction riêng)
        S-->>FE: 401 "Tên đăng nhập hoặc mật khẩu không đúng"
    else Tài khoản bị khoá
        S-->>FE: 403 "Tài khoản đã bị khoá"
    else Hợp lệ
        S->>J: Tạo access token (30 phút) + refresh token (7 ngày)
        S->>DB: Lưu refresh token (dạng hash), cập nhật last_login_at, audit_logs LOGIN
        opt Mức 2 — một tài khoản một phiên (bước 9a)
            S->>DB: SessionRevoker: thu hồi các phiên cũ (NEW_LOGIN)
            S->>WS: Sau commit: FORCE_LOGOUT tới từng kết nối của phiên cũ, rồi đóng kết nối đó
        end
        S-->>FE: 200 {accessToken, refreshToken, user}
        FE->>FE: Lưu token vào localStorage, lưu user vào store
        alt user.mustChangePassword
            FE->>U: Chuyển tới /doi-mat-khau
        else
            FE->>U: Chuyển tới trang chủ theo vai trò
        end
        FE->>WS: Mở WebSocket /ws, gửi CONNECT (Authorization: Bearer …)
        WS->>J: Kiểm tra token (StompAuthInterceptor)
        WS-->>FE: CONNECTED
        FE->>WS: SUBSCRIBE /user/queue/notifications và /user/queue/session
    end
```

**Ghi chú**
- Refresh token được lưu dưới dạng **hash** trong DB, để có thể thu hồi (đăng xuất, đổi mật khẩu, buộc đăng xuất).
- Ở bước xác thực STOMP, token gửi trong header của khung `CONNECT`. Trình duyệt không gửi được header tuỳ ý trong lúc bắt tay WebSocket (HTTP Upgrade), nên token không đi qua bước đó.

---

## SD-02 · Tự làm mới access token khi hết hạn

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend (axiosClient)
    participant B as Backend

    FE->>B: GET /api/... (access token đã hết hạn)
    B-->>FE: 401
    Note over FE: Nhiều request cùng nhận 401 thì<br/>chỉ chạy MỘT lần refresh, các request khác chờ
    FE->>B: POST /api/auth/refresh {refreshToken}
    alt Refresh token hợp lệ
        B->>B: Thu hồi refresh token cũ, cấp cặp token mới (xoay vòng)
        B-->>FE: 200 {accessToken, refreshToken}
        FE->>FE: Lưu token mới
        FE->>B: Gửi lại request ban đầu với token mới
        B-->>FE: 200 dữ liệu
    else Hết hạn / đã bị thu hồi
        B-->>FE: 401
        FE->>FE: Xoá phiên, chuyển tới /dang-nhap?returnUrl=…
    end
```

**Ghi chú:** refresh token **xoay vòng**: mỗi lần dùng thì bị thu hồi và đổi sang token mới. Khi một refresh token **đã bị xoay vòng** lại được gửi lên:
- **trong 30 giây** sau lần xoay vòng: thường là 2 tab của cùng trình duyệt refresh cùng lúc. Server trả `401 REFRESH_TOKEN_ROTATED` và **không thu hồi gì**. Client đọc token mới mà tab kia đã lưu trong localStorage rồi thử lại.
- **sau 30 giây**: có thể token đã bị đánh cắp. Backend thu hồi **toàn bộ** phiên của user đó (`revoke_reason = TOKEN_REUSE`).

---

## SD-03 · Import sinh viên từ file (HTTP)

```mermaid
sequenceDiagram
    autonumber
    actor GV as Giáo viên
    participant FE as Frontend
    participant C as ClassroomController
    participant I as StudentImportService
    participant DB as DB
    participant M as MailService (@Async)
    participant SMTP as SMTP

    GV->>FE: Chọn file .xlsx/.csv, tick "Gửi email"
    FE->>C: POST /api/teacher/classes/{id}/import (multipart)
    C->>I: importStudents(classId, file, sendEmail)
    I->>DB: Kiểm tra lớp thuộc giáo viên
    I->>I: Đọc file (Apache POI / Commons CSV), kiểm tra cột bắt buộc
    alt File sai định dạng / thiếu cột
        I-->>FE: 400 "File thiếu cột ma_sv"
    else Đọc được
        loop Mỗi dòng (mỗi dòng một giao dịch riêng — BR-03)
            I->>I: Kiểm tra mã SV, họ tên, email, trùng trong file
            alt Dòng lỗi
                I->>I: Ghi vào danh sách lỗi (số dòng, lý do)
            else Mã SV chưa có tài khoản
                I->>I: Sinh mật khẩu ngẫu nhiên 8 ký tự
                I->>DB: INSERT users (must_change_password = true)
                I->>DB: INSERT class_members
            else Đã có tài khoản, chưa trong lớp
                I->>DB: INSERT class_members
            else Đã là thành viên
                I->>I: Đếm "bỏ qua"
            end
        end
        I->>I: Lưu tạm danh sách tài khoản + mật khẩu (trong bộ nhớ, hết hạn sau 15 phút)
        opt sendEmail
            I-)M: Gửi email cho từng SV mới (chạy nền)
            M-)SMTP: SMTP: thông tin đăng nhập
        end
        I-->>FE: 200 {created, existing, skipped, errors[], credentialsToken}
        FE->>GV: Hiện báo cáo import
        GV->>FE: Bấm "Tải file tài khoản"
        FE->>C: GET /api/teacher/imports/{credentialsToken}/credentials
        C->>I: Lấy và XOÁ danh sách tạm (chỉ tải một lần)
        C-->>FE: File .xlsx (mã SV, họ tên, mật khẩu tạm)
    end
```

**Ghi chú:** mật khẩu tạm **không bao giờ lưu dạng rõ trong DB**, chỉ có bản hash. Danh sách rõ chỉ nằm trong bộ nhớ của backend cho tới khi giáo viên tải file, hoặc sau 15 phút thì tự huỷ.

---

## SD-04 · Import sinh viên qua TCP (admin-cli)

```mermaid
sequenceDiagram
    autonumber
    actor GV as Giáo viên / Admin
    participant CLI as admin-cli
    participant L as TcpImportServer (luồng accept)
    participant W as Luồng xử lý (thread pool)
    participant I as StudentImportService
    participant DB as DB

    GV->>CLI: admin-cli import --class L01 --file sv.csv --user gv
    CLI->>CLI: POST /api/auth/login → accessToken
    CLI->>L: Mở kết nối TCP tới cổng 9090
    alt Đủ số phiên đồng thời (Semaphore hết chỗ)
        L-->>CLI: ERR 503 Server bận\n
        L->>L: Đóng socket
    else Còn chỗ
        L->>W: Giao socket cho một luồng trong ExecutorService
        CLI->>W: AUTH <accessToken>\n
        alt Token sai / hết hạn
            W-->>CLI: ERR 401 Token sai hoặc hết hạn\n
            W->>W: Đóng socket
        else Hợp lệ (ADMIN / TEACHER)
            W-->>CLI: OK\n
            CLI->>W: IMPORT L01 <số byte> [EMAIL]\n
            W->>DB: Tìm lớp L01, kiểm tra quyền (trước khi nhận dữ liệu)
            alt Không có lớp / không có quyền / quá 5 MB
                W-->>CLI: ERR 404 / 403 / 413\n
            else
                CLI->>W: Nội dung CSV (đúng <số byte>)
                loop Mỗi dòng
                    W->>I: Xử lý 1 dòng (như SD-03)
                    I->>DB: INSERT ... (transaction riêng)
                    W-->>CLI: PROGRESS n/tổng\n
                    CLI->>CLI: Cập nhật thanh tiến trình
                end
                W-->>CLI: DONE created=.. existing=.. skipped=.. errors=..\n
                W-->>CLI: ERRLINE <số dòng> <lý do>\n (mỗi dòng lỗi)
                W-->>CLI: CRED <mã SV>|<họ tên>|<mật khẩu>\n (mỗi tài khoản mới)
                W-->>CLI: END\n
            end
            W->>W: Đóng socket, ghi audit log (IP client — bước 12)
            CLI->>GV: In báo cáo, lưu file tài khoản ra đĩa
        end
    end
```

**Ghi chú**
- Giao thức dạng text. Mỗi khung tin kết thúc bằng `\n`. Riêng phần nội dung CSV gửi đúng số byte đã khai báo, nên server biết chính xác chỗ dừng.
- Server có **timeout đọc** (ví dụ 30 giây). Client im lặng quá lâu thì server đóng kết nối, giải phóng luồng.
- Mỗi kết nối được một luồng trong thread pool xử lý. Số phiên đồng thời giới hạn bằng `Semaphore` (bằng số luồng, `app.tcp.threads`): hết chỗ thì trả ngay `ERR 503` thay vì để client chờ trong hàng đợi.
- Lớp và quyền được kiểm tra **ngay khi nhận dòng `IMPORT`**, trước khi đọc phần CSV, để từ chối sớm mà không phải nhận cả file.
- Đặc tả đầy đủ của giao thức nằm trong file 05.

---

## SD-05 · Phát hành bài và gửi thông báo

```mermaid
sequenceDiagram
    autonumber
    actor GV as Giáo viên
    participant FE as Frontend GV
    participant A as AssignmentService
    participant N as NotificationService
    participant DB as DB
    participant WS as STOMP broker
    participant M as MailService (@Async)
    participant SV as Frontend SV (đang online)

    GV->>FE: Chọn lớp / sinh viên, bấm "Phát hành"
    FE->>A: POST /api/teacher/assignments/{id}/publish {classIds, studentIds}
    A->>DB: Kiểm tra bài ở trạng thái DRAFT, đủ phần và tiêu chí
    A->>DB: INSERT assignment_targets, UPDATE status = PUBLISHED
    A->>DB: Tính danh sách SV nhận bài (lớp ∪ cá nhân, loại trùng)
    A->>N: notifyMany(students, ASSIGNMENT_PUBLISHED, …)
    loop Mỗi sinh viên
        N->>DB: INSERT notifications
    end
    Note over N: Lưu DB TRƯỚC, đẩy realtime SAU<br/>→ SV đang offline vẫn thấy khi vào lại
    N->>WS: convertAndSendToUser(sv, /queue/notifications, payload)
    WS-->>SV: MESSAGE (chuông +1, hiện toast)
    N-)M: Gửi email cho từng SV
    A-->>FE: 200 {recipientCount}
```

**Ghi chú**
- Nếu `open_at` ở tương lai (BR-05), thông báo chưa gửi ngay. Job định kỳ sẽ gửi khi tới thời điểm mở.
- Cài đặt: `PublishService` chỉ phát sự kiện `AssignmentEvents.Published`. `NotificationEventListener` nghe ở pha `BEFORE_COMMIT`, nên các dòng `notifications` được INSERT **trong cùng transaction** với việc phát hành; bước đẩy WebSocket và gửi email chạy sau khi commit.

---

## SD-06 · Tự lưu nháp, mất mạng và xung đột

```mermaid
sequenceDiagram
    autonumber
    actor SV as Sinh viên
    participant FE as Frontend
    participant IDB as IndexedDB (trình duyệt)
    participant B as Backend (DraftService)
    participant DB as DB

    SV->>FE: Gõ bài
    FE->>FE: Đếm số từ ngay khi gõ
    FE->>IDB: Lưu nháp cục bộ (mỗi lần thay đổi)
    Note over FE: Chờ 3 giây không gõ (debounce)
    FE->>B: PUT /api/student/parts/{partId}/draft {content, draftVersion: 7}
    B->>DB: SELECT … FOR UPDATE (khoá dòng bài làm)
    alt Online, draftVersion khớp DB
        B->>DB: UPDATE submissions SET draft_content, draft_version = 8, draft_saved_at
        B-->>FE: 200 {savedAt, wordCount, draftVersion: 8}
        FE->>SV: "Đã lưu lúc 14:32"
    else Mất mạng
        FE--xB: Lỗi mạng
        FE->>SV: "Đang ngoại tuyến – đã lưu trên máy"
        Note over FE: Nghe sự kiện "online" của trình duyệt
        FE->>B: Có mạng lại → gửi lại bản trong IndexedDB
    else Xung đột (Mức 2): tab/máy khác đã lưu bản mới hơn
        B-->>FE: 409 DRAFT_CONFLICT, data = {serverContent, draftVersion: 9, savedAt}
        FE->>SV: Hộp thoại: giữ bản trên máy này / dùng bản trên server
        SV->>FE: Chọn một bản
        FE->>B: PUT …/draft {content đã chọn, draftVersion: 9}
    end
```

**Ghi chú**
- `draftVersion` là **optimistic locking**. Mỗi lần lưu thành công, server tăng số này. Client gửi số mình đang có; lệch với DB nghĩa là có nơi khác đã lưu, nên server trả 409 thay vì ghi đè.
- Cột `draft_version` thêm ở migration V8 (bước 10). Server khoá dòng rồi mới so số, nên 2 lần lưu đồng thời cùng số chỉ 1 lần thành công. Client không gửi `draftVersion` thì server lưu đè như Mức 1.

---

## SD-07 · Nộp bài một phần

```mermaid
sequenceDiagram
    autonumber
    actor SV as Sinh viên
    participant FE as Frontend SV
    participant S as SubmissionService
    participant DB as DB
    participant N as NotificationService
    participant WS as STOMP broker
    participant GV as Frontend GV

    SV->>FE: Bấm "Nộp phần này"
    FE->>FE: Chưa đủ số từ tối thiểu → hỏi xác nhận
    FE->>S: POST /api/student/parts/{partId}/submit {content, expectedVersion: 1}
    S->>DB: SELECT submission ... FOR UPDATE (khoá dòng)
    S->>S: Kiểm tra SV được giao bài và bài đang mở (BR-04, BR-05)
    S->>S: Hạn áp dụng = hạn sửa ?? hạn phần ?? hạn bài (BR-07)<br/>so với GIỜ SERVER (NFR-12)
    alt Quá hạn và không cho nộp muộn
        S-->>FE: 422 "Đã quá hạn nộp"
    else Trạng thái không cho nộp (đã nộp, không bị yêu cầu sửa)
        S-->>FE: 409 "Phần này đã được nộp"
    else expectedVersion ≠ current_version (bấm nộp 2 lần / 2 tab)
        S-->>FE: 409 + version hiện tại (không tạo trùng)
    else Hợp lệ
        S->>DB: INSERT submission_versions (version_no = 2, word_count, is_late)
        S->>DB: UPDATE submissions SET status = SUBMITTED, current_version = 2, draft_content = NULL
        S-->>FE: 201 {versionNo: 2, submittedAt, late}
        FE->>SV: "Đã nộp lúc 23:58"
        S->>N: notify(giáo viên, SUBMISSION_RECEIVED)
        N->>DB: INSERT notifications
        N->>WS: /user/queue/notifications của giáo viên
        WS-->>GV: Thông báo "SV A vừa nộp Task 2"
        opt Mức 2 — tiến độ live
            S->>WS: /topic/assignments/{id}/progress {studentId, partId, status}
            WS-->>GV: Ô trong ma trận tiến độ đổi màu
        end
    end
```

**Ghi chú:** `SELECT … FOR UPDATE` cùng với `expectedVersion` đảm bảo hai request nộp đồng thời (bấm đúp, hai tab, mạng gửi lại) chỉ tạo đúng **một** version. Đây là chỗ xử lý đồng thời quan trọng khi nhiều sinh viên nộp sát hạn (NFR-01).

---

## SD-08 · Chấm bài, trả bài / yêu cầu sửa

```mermaid
sequenceDiagram
    autonumber
    actor GV as Giáo viên
    participant FE as Frontend GV
    participant G as GradingService
    participant BC as BandCalculator
    participant DB as DB
    participant N as NotificationService
    participant SV as Frontend SV

    GV->>FE: Mở bài nộp từ hàng đợi
    FE->>G: GET /api/teacher/submissions/{id}
    G->>DB: Bài làm (version mới nhất), tiêu chí, checklist, kết quả chấm nháp (nếu có)
    G-->>FE: 200 dữ liệu màn hình chấm
    loop Giáo viên tick checklist, cho điểm, nhận xét
        FE->>FE: Tính band tạm ở client để hiển thị ngay
    end
    GV->>FE: Bôi đen đoạn văn → thêm nhận xét inline
    FE->>G: PUT /api/teacher/submissions/{id}/grade {versionNo, scores[], checklist[], inlineComments[], generalComment, zeroReason}
    G->>DB: SELECT submission FOR UPDATE (version mới nhất, trạng thái SUBMITTED / GRADED)
    G->>G: Kiểm tra điểm: bước điểm (BR-11), giới hạn checklist (BR-17), ≤ 20 từ (BR-18), "Chấm 0" (BR-19)<br/>Kiểm tra nhận xét inline: vị trí, đoạn trích, mã lỗi (BR-21)
    G->>BC: Trung bình có trọng số (BR-12)
    BC-->>G: band phần (chưa làm tròn)
    G->>DB: UPSERT grades (status = DRAFT), grade_criterion_scores, grade_checklist_marks, inline_comments<br/>submissions.status = GRADED
    G-->>FE: 200 GradeDto (rawBand, band đã làm tròn, giới hạn từng tiêu chí)
    alt Trả bài
        GV->>FE: Bấm "Trả bài"
        FE->>G: POST /api/teacher/submissions/{id}/return
        G->>DB: SELECT submission FOR UPDATE, kiểm tra đã chấm đủ mọi tiêu chí
        G->>DB: grades.status = RETURNED, returned_at<br/>submissions.status = RETURNED
    else Yêu cầu sửa
        GV->>FE: Nhập lời nhắn + hạn sửa, bấm "Yêu cầu sửa"
        FE->>G: POST /api/teacher/submissions/{id}/request-revision {note, dueAt}
        G->>DB: SELECT submission FOR UPDATE, kiểm tra đã chấm đủ, số lần sửa còn lại (BR-10),<br/>dueAt ở tương lai, bài tập chưa đóng
        G->>DB: grades.status = RETURNED<br/>submissions.status = REVISION_REQUESTED, revision_due_at, revision_note
    end
    G-)N: Sau khi commit: GradingEvents.Returned / RevisionRequested
    N->>N: notify(sinh viên, GRADE_RETURNED / REVISION_REQUESTED)
    N-->>SV: WebSocket + email
```

**Ghi chú**
- Band hiển thị ở client chỉ để xem trước. **Giá trị chính thức do server tính** bằng `BandCalculator`, để không phụ thuộc vào client. Server lưu band chưa làm tròn, chỉ làm tròn .25 / .75 khi trả ra để hiển thị.
- Mọi thao tác ghi đều khoá dòng `submissions`, nên hai lần bấm "Trả bài" cùng lúc chỉ có một lần thành công, và không đua được với việc sinh viên nộp lại.

---

## SD-09 · Vòng đời WebSocket: rớt kết nối, kết nối lại, lấy thông báo bị lỡ

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant WS as STOMP broker (Backend)
    participant API as Backend REST
    participant DB as DB

    FE->>WS: CONNECT (token), heart-beat: 10000,10000
    WS-->>FE: CONNECTED
    FE->>WS: SUBSCRIBE /user/queue/notifications
    loop Mỗi 10 giây
        FE-->>WS: heartbeat
        WS-->>FE: heartbeat
    end
    Note over FE,WS: Mất mạng / backend khởi động lại
    FE->>FE: Không nhận heartbeat → coi như mất kết nối<br/>Hiện thanh "Mất kết nối… đang thử lại"
    loop Exponential backoff: 1s, 2s, 4s, 8s … tối đa 30s
        FE-xWS: Thử CONNECT
    end
    FE->>WS: CONNECT thành công
    WS-->>FE: CONNECTED
    FE->>WS: SUBSCRIBE lại các kênh
    FE->>API: GET /api/notifications?after=<createdAt của thông báo cuối đã nhận>
    API->>DB: SELECT notifications WHERE user_id = ? AND created_at > ?
    API-->>FE: Các thông báo bị lỡ
    FE->>FE: Gộp vào danh sách, cập nhật số chưa đọc, ẩn thanh cảnh báo
```

**Ghi chú:** vì thông báo luôn được **lưu DB trước khi đẩy** (SD-05), client mất kết nối bao lâu cũng không mất thông báo. Đây là kịch bản kiểm chứng NFR-04 và NFR-06.

---

## SD-10 · Job nhắc hạn nộp

```mermaid
sequenceDiagram
    autonumber
    participant SCH as Bộ lập lịch (@Scheduled 15 phút)
    participant R as DeadlineReminderJob
    participant DB as DB
    participant N as NotificationService

    SCH->>R: run()
    R->>DB: Tìm các phần đã phát hành có hạn áp dụng trong (now, now + 24h]
    DB-->>R: Danh sách (phần, hạn)
    loop Mỗi phần
        R->>R: Còn ≤ 2h → mốc H2, ngược lại → mốc H24
        R->>DB: SV được giao, CHƯA nộp, CHƯA có reminder_log ở mốc này
        DB-->>R: Danh sách sinh viên
        loop Mỗi sinh viên
            R->>DB: INSERT reminder_log (part, student, kind) ON CONFLICT DO NOTHING
            alt Chèn được (chưa nhắc lần nào)
                R->>N: notify(sv, DEADLINE_REMINDER, "Còn X giờ…")
            else Đã có (job khác vừa nhắc)
                R->>R: Bỏ qua
            end
        end
    end
    Note over R: Làm tương tự với hạn sửa bài<br/>(mốc REVISION_H24, REVISION_H2)
```

**Ghi chú**
- Dùng khoá chính của `reminder_log` cùng `ON CONFLICT DO NOTHING` để chống gửi trùng (BR-14), kể cả khi job chạy chồng nhau hoặc server khởi động lại giữa chừng.
- Cài đặt (B7b): `ReminderLogRepository.findCandidates` tìm mọi cặp (phần, sinh viên) cần nhắc bằng **một câu SQL** (hạn nộp + hạn sửa, đã loại người có `reminder_log` ở mốc đó). Mỗi nhóm (phần, mốc) chạy trong một transaction: chèn `reminder_log` cho từng người, rồi gọi `NotificationService.notify` cho những ai chèn được. Tạo thông báo lỗi thì bản ghi "đã nhắc" cũng bị huỷ, lượt sau nhắc lại.
- Hạn áp dụng giống BR-07: hạn riêng của phần, không có thì hạn bài; bài làm đang "Cần sửa" thì dùng hạn sửa. "Chưa nộp" = chưa có bài làm hoặc mới có nháp.
- Mỗi lần giáo viên **yêu cầu sửa**, các mốc `REVISION_H24` / `REVISION_H2` cũ của (phần, sinh viên) bị xoá, để vòng sửa mới vẫn được nhắc.
- Cùng bộ lập lịch còn chạy job **thông báo bài tới giờ mở**: mỗi bài được "giành" bằng `UPDATE … SET open_notified_at = now WHERE open_notified_at IS NULL`, nên chỉ thông báo một lần.
- Chu kỳ và hai mốc cấu hình trong `app.reminders` (`interval: PT15M`, `soon: PT24H`, `urgent: PT2H`).

---

## SD-11 · Đồng bộ giờ với server (Mức 2)

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant B as Backend

    FE->>FE: t0 = Date.now()
    FE->>B: GET /api/time
    B-->>FE: {serverTime: ts}
    FE->>FE: t3 = Date.now()<br/>rtt = t3 − t0<br/>offset = ts + rtt/2 − t3
    Note over FE: Lặp 3 lần, lấy kết quả có rtt nhỏ nhất<br/>Lặp lại mỗi 5 phút
    FE->>FE: Đồng hồ đếm ngược = hạn − (Date.now() + offset)
```

**Ghi chú:** cách làm giống NTP thu gọn. Kết quả chỉ dùng để **hiển thị** cho đúng; **quyết định nộp muộn luôn ở server** (SD-07). Nhờ vậy sinh viên chỉnh giờ máy cũng không có tác dụng.

---

## SD-12 · Chat realtime (Mức 2)

```mermaid
sequenceDiagram
    autonumber
    actor A as Giáo viên
    participant FA as Frontend GV
    participant WS as STOMP broker
    participant R as ChatChannelRule (interceptor)
    participant C as ChatSocketController / ChatService
    participant DB as DB
    participant FB as Frontend SV

    FA->>WS: SUBSCRIBE /topic/chat/{channelId}
    WS->>R: Kiểm tra là thành viên kênh
    FB->>WS: SUBSCRIBE /topic/chat/{channelId}
    FA->>C: GET /api/chat/{channelId}/messages (50 tin gần nhất + mốc "đã xem")
    A->>FA: Gõ tin nhắn
    FA->>WS: SEND /app/chat/{channelId}/typing
    WS->>C: @MessageMapping (bỏ bớt nếu dày hơn 2 giây)
    C-->>FB: TYPING "Giáo viên đang gõ…"
    A->>FA: Bấm gửi
    FA->>WS: SEND /app/chat/{channelId}/send {content, clientMsgId}
    WS->>R: Kiểm tra là thành viên kênh
    WS->>C: @MessageMapping
    alt Tin rỗng / quá 4000 ký tự
        C-->>FA: /user/queue/errors {code, message}
    else Hợp lệ
        C->>DB: INSERT chat_messages (UNIQUE sender + clientMsgId: gửi lại thì dùng tin đã lưu)
        C->>WS: Sau commit: phát MESSAGE tới /topic/chat/{channelId}
        WS-->>FA: Tin nhắn (khớp clientMsgId → "đã gửi")
        WS-->>FB: Tin nhắn mới
        opt Kênh bài nộp
            C-->>FB: Sau commit: /user/queue/chat (chưa có kênh trong danh sách → tải lại danh sách)
        end
    end
    FB->>WS: SEND /app/chat/{channelId}/read {lastMessageId}
    WS->>C: Upsert chat_read_markers (GREATEST)
    C-->>FA: Sau commit: READ → "Đã xem"
```

---

## SD-13 · Chế độ thi và tự nộp khi hết giờ (Mức 3)

```mermaid
sequenceDiagram
    autonumber
    actor SV as Sinh viên
    participant FE as Frontend
    participant E as ExamService
    participant WS as STOMP broker
    participant DB as DB
    participant SCH as Bộ lập lịch

    SV->>FE: Bấm "Bắt đầu"
    FE->>E: POST /api/student/parts/{partId}/exam/start
    E->>DB: INSERT exam_sessions (started_at = now, ends_at = now + 40 phút)
    E-->>FE: {endsAt}
    loop Trong lúc làm bài
        FE->>WS: SEND /app/exam/{sessionId}/event {TAB_HIDDEN | TAB_VISIBLE | PASTE}
        WS->>E: Ghi exam_events (kèm IP)
    end
    Note over WS,E: Mất kết nối WebSocket → server tự ghi DISCONNECT,<br/>kết nối lại → RECONNECT
    alt SV nộp trước giờ
        FE->>E: Nộp như SD-07
    else Hết giờ
        SCH->>E: Mỗi phút: tìm phiên quá ends_at mà chưa finished
        E->>DB: Nộp bản nháp mới nhất thành version mới, finished_at = now
        E->>WS: /user/queue/notifications "Đã hết giờ, bài đã được nộp"
    end
```
