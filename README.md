# Hệ thống giao bài & chấm bài IELTS Writing trực tuyến

Đồ án môn **Lập trình mạng**. Giáo viên tạo bài tập IELTS Writing nhiều phần, giao theo lớp hoặc từng sinh viên, chấm theo rubric IELTS (checklist, mã lỗi, nhận xét gắn vào đoạn văn) và theo dõi tiến độ. Sinh viên làm bài trên web, tự lưu nháp, nộp từng phần và xem kết quả.

Phần mạng là trọng tâm: **REST qua HTTPS**, **WebSocket STOMP qua WSS** (thông báo realtime, ai đang online, tiến độ live, chat, một tài khoản một phiên), một **server TCP tự viết** để import sinh viên bằng client dòng lệnh `admin-cli`, rate limit, nhật ký hệ thống, Docker + nginx, và **kiểm thử tải bằng k6**.

| Phần | Trạng thái |
|---|---|
| Backend — Mức 1 (toàn bộ đề gốc) và Mức 2 (trọng tâm mạng) | ✅ 468 test tự động, đạt các yêu cầu hiệu năng NFR-01/02/03 ([docs/09](docs/09-kiem-thu-tai.md)) |
| `admin-cli` (client TCP) | ✅ |
| Frontend (Next.js) | 🟡 mới có khung dự án, giao diện làm theo thiết kế Figma (chưa có) |
| Mức 3 (chế độ thi, đạo văn, ngân hàng đề, gợi ý AI, TCP qua TLS) | ⬜ mở rộng, làm khi còn thời gian |

Tiến độ chi tiết: [PROGRESS.md](PROGRESS.md) · Kế hoạch: [PLAN.md](PLAN.md).

---

## Kiến trúc

```
                ┌──────────────────────── Docker Compose ─────────────────────────┐
trình duyệt ──HTTPS / WSS 443──▶ nginx ──▶ frontend (Next.js 16)                  │
                │                    └──▶ backend (Spring Boot 4, monolith) ──▶ PostgreSQL 18
admin-cli ──TCP 9090─────────────────────▶   ├─ REST /api      ├─ job nhắc hạn    │
                │                             ├─ STOMP /ws      └─ server TCP      ├──▶ Mailpit (SMTP)
                └─────────────────────────────────────────────────────────────────┘
```

**Monolith:** một ứng dụng Spring Boot chứa REST, WebSocket, server TCP và job định kỳ. Không dùng microservice, không dùng Redis. Bên trong chia module theo nghiệp vụ (`user`, `classroom`, `assignment`, `submission`, `grading`, `notification`, `stats`, `realtime`, `chat`, `tcpgateway`, `audit`, `demo`), phụ thuộc chỉ đi một chiều. Chi tiết xem [docs/01](docs/01-tong-quan.md) mục 7 và [backend/AI-CODING-RULES.md](backend/AI-CODING-RULES.md).

## Thư mục

| Thư mục | Nội dung |
|---|---|
| [`backend/`](backend/) | Spring Boot 4 (Java 21), Flyway migration V1 → V8, test JUnit; file gọi thử API ở `backend/http/` |
| [`frontend/`](frontend/) | Next.js 16 + Ant Design 6 (khung) |
| [`admin-cli/`](admin-cli/) | Client TCP import sinh viên, Java thuần — xem [README](admin-cli/README.md) |
| [`loadtest/`](loadtest/) | Script k6: nộp bài sát hạn, 500 kết nối WebSocket |
| [`deploy/`](deploy/) | Hướng dẫn chạy bằng Docker, cấu hình nginx HTTPS/WSS |
| [`docs/`](docs/) | Tài liệu phân tích thiết kế (Markdown + Mermaid) — **bản chuẩn** |
| [`docx/`](docx/) | Bản Word sinh từ `docs/`; `00-tai-lieu-phan-tich-thiet-ke.docx` là file gộp để nộp |

---

## Chạy nhanh bằng Docker (khuyên dùng khi demo)

Cần Docker Desktop. Chạy ở thư mục gốc (Git Bash trên Windows):

```
cp .env.example .env              # sửa DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD
                                  # muốn có dữ liệu mẫu: SPRING_PROFILES_ACTIVE=docker,demo
sh deploy/nginx/gen-cert.sh       # chứng chỉ tự ký cho HTTPS
docker compose up --build -d      # lần đầu mất vài phút
```

| Địa chỉ | Dùng để |
|---|---|
| https://localhost | Web (chấp nhận cảnh báo chứng chỉ tự ký) — REST ở `/api/…`, WebSocket ở `wss://localhost/ws` |
| http://localhost:8025 | Mailpit — xem email hệ thống gửi |
| `localhost:9090` | Server TCP cho `admin-cli` |

Lần đầu đăng nhập bằng `admin` / `ADMIN_PASSWORD`, hệ thống bắt đổi mật khẩu ngay. Thao tác khác (dừng, xoá dữ liệu, xem log, vào psql): [deploy/README.md](deploy/README.md). Clone từ đầu hoặc đưa lên máy chủ có tên miền: [docs/11](docs/11-huong-dan-trien-khai.md).

### Dữ liệu mẫu (profile `demo`)

Đặt `SPRING_PROFILES_ACTIVE=docker,demo` trong `.env` (khi chạy trên máy thì dùng `dev,demo`). Lần khởi động đầu, backend tạo sẵn các dữ liệu dưới đây; các lần sau thấy dữ liệu đã có thì bỏ qua:

| Tài khoản / dữ liệu | Ghi chú |
|---|---|
| Giáo viên `gv.demo` | Chủ của mọi lớp và bài mẫu |
| Lớp `DEMO-A`: sinh viên `DEMO001` … `DEMO010` · Lớp `DEMO-B`: `DEMO011` … `DEMO020` | Tên tiếng Việt, email `<mã>@ielts.local` |
| Bài "Tuần 1" (Task 1 + Task 2, rubric IELTS, hạn sau 7 ngày) | Đã giao cho cả 2 lớp; `DEMO001` … `DEMO006` đã nộp Task 1 (có sẵn bài để chấm), 3 người đang viết nháp Task 2 |
| Bài "Tuần 2" (Task 2) | Còn ở trạng thái nháp, để phát hành ngay trong buổi demo |

Mọi tài khoản mẫu dùng chung mật khẩu `DEMO_PASSWORD` (mặc định `Demo@2026`) và **không** bị bắt đổi mật khẩu. Chỉ bật profile này khi demo hoặc kiểm thử, không bật cho hệ thống thật.

---

## Chạy khi phát triển

Cần JDK 21+, Maven, Node 22, PostgreSQL 18 (DB `ielts` và `ielts_test`, user `ielts` / `ielts`).

| Việc | Lệnh |
|---|---|
| Email khi dev | `tools\mailpit\mailpit.exe` → http://localhost:8025 |
| Backend | `cd backend` → `mvn spring-boot:run` → http://localhost:8080, Swagger UI: http://localhost:8080/swagger-ui.html |
| Backend có dữ liệu mẫu | `mvn spring-boot:run -Dspring-boot.run.profiles=dev,demo` |
| Frontend | `cd frontend` → `npm install` → `npm run dev` → http://localhost:3000 |
| admin-cli | `cd admin-cli` → `mvn package` → `java -jar target/admin-cli.jar import --class DEMO-A --file ds.csv --user gv.demo` |

Frontend gọi backend **qua proxy của Next** (`next.config.ts` → `rewrites` cho `/api`, `/uploads`, `/ws`): trình duyệt chỉ nói chuyện với cổng 3000, không dính CORS khi chạy trên máy.

**Mở ra ngoài qua một tunnel** (cho người khác thử từ máy khác): chỉ cần tunnel **cổng 3000**, ví dụ `cloudflared tunnel --url http://localhost:3000` hoặc `ngrok http 3000`. Domain tunnel phải nằm trong `allowedDevOrigins` của `next.config.ts` (đã có Cloudflare, ngrok, localtunnel, Dev Tunnels) — thiếu thì trang hiện ra nhưng JavaScript không chạy, bấm nút không có tác dụng. Backend phải cho phép domain của tunnel (cả REST lẫn WebSocket đều kiểm tra `Origin`), nhận được mẫu có `*`:

```powershell
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000,https://*.trycloudflare.com,https://*.ngrok-free.app"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev,demo"
```

Link trong email vẫn trỏ `APP_URL` (mặc định http://localhost:3000) — muốn link trỏ về tunnel thì đặt thêm `APP_URL` và `APP_LOGIN_URL`. Tunnel chỉ để thử nhanh; demo chính thức dùng Docker + HTTPS (mục trên).

## Kiểm thử

| Loại | Lệnh | Kết quả gần nhất |
|---|---|---|
| Backend: unit + integration + WebSocket thật + đồng thời | `cd backend` → `mvn test` (DB `ielts_test`) | 468 test, tất cả đạt |
| admin-cli | `cd admin-cli` → `mvn package` | 6 test đạt |
| Kiểm thử tải k6 (chạy trong Docker) | xem [docs/09](docs/09-kiem-thu-tai.md) mục 6 | 200 sinh viên nộp sát hạn: p95 9,9 ms, 0% lỗi · 500 kết nối WebSocket: 500 / 500, thông báo p95 228 ms |
| Gọi thử API bằng tay | file `backend/http/*.http` (IntelliJ / VS Code REST Client) | — |

### Ảnh Wireshark cho báo cáo (HTTP và HTTPS)

Bộ ảnh này cho thấy vì sao phải dùng HTTPS: qua HTTP, mật khẩu hiện rõ trong gói tin; qua HTTPS thì chỉ thấy dữ liệu đã mã hoá.

1. Cài Wireshark kèm **Npcap** (nhớ tick "Support loopback traffic").
2. **HTTP (lộ mật khẩu):** chạy backend khi dev (`mvn spring-boot:run`, cổng 8080). Bắt gói trên card *Adapter for loopback traffic capture*, lọc `http.request.method == "POST" && tcp.port == 8080`, rồi gửi request đăng nhập trong `backend/http/auth.http`. Mở gói `POST /api/auth/login` → *JavaScript Object Notation*: thấy nguyên `username` / `password`.
3. **HTTPS (không lộ):** chạy bằng Docker, đăng nhập qua `https://localhost/api/auth/login`. Lọc `tcp.port == 443`: chỉ thấy `Client Hello` / `Server Hello` rồi `Application Data` đã mã hoá, không đọc được nội dung.
4. **TCP 9090 (`admin-cli`):** lọc `tcp.port == 9090` → *Follow TCP Stream*. Ảnh này cho thấy giao thức text tự định nghĩa (`AUTH`, `IMPORT`, `PROGRESS`, `DONE` — [docs/05](docs/05-api.md) phần C) và cũng cho thấy kênh này **chưa mã hoá**: token trong lệnh `AUTH` và mật khẩu tạm ở các dòng `CRED` đều đọc được. Vì vậy chỉ dùng `admin-cli` trong mạng tin cậy; TCP qua TLS là bước 19 (Mức 3).

---

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [01 Tổng quan](docs/01-tong-quan.md) | Yêu cầu chức năng (FR), phi chức năng (NFR), quy tắc nghiệp vụ BR-01 → BR-22, kiến trúc |
| [02 Use case](docs/02-use-case.md) · [03 Sequence](docs/03-sequence.md) | 32 use case, sơ đồ tuần tự SD-01 → SD-13 |
| [04 ERD](docs/04-erd.md) · [05 API](docs/05-api.md) · [06 Trạng thái](docs/06-trang-thai.md) | Bảng và migration; REST, WebSocket, giao thức TCP, mã lỗi; vòng đời đối tượng |
| [07 Design system](docs/07-design-system.md) · [08 Nghiệp vụ chấm IELTS](docs/08-nghiep-vu-cham-ielts.md) | Giao diện; cách chấm và tính band |
| [09 Kiểm thử tải](docs/09-kiem-thu-tai.md) | Môi trường, kịch bản và kết quả k6 |
| [10 Hướng dẫn sử dụng](docs/10-huong-dan-su-dung.md) | Cách mở hệ thống, **danh sách tài khoản**, thao tác theo vai trò, kịch bản demo, sự cố thường gặp |
| [11 Clone và triển khai](docs/11-huong-dan-trien-khai.md) | Clone mã nguồn, chạy bằng Docker trên máy / LAN, đưa lên máy chủ Linux có tên miền (Let's Encrypt, đóng cổng, SMTP thật), cập nhật, sao lưu |

Bản Word: `python tools/docs-export/export_docx.py --merge` (chạy bằng PowerShell) → `docx/00-tai-lieu-phan-tich-thiet-ke.docx`.

Bảng mô tả band IELTS trong hệ thống là **bản diễn giải tiếng Việt thu gọn**, không chép nguyên văn; bản chính thức xem tại [ielts.org](https://ielts.org).
