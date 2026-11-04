# IELTS Writing — AcaWrite

AcaWrite là hệ thống web hỗ trợ luyện viết IELTS Writing và viết paper học thuật theo nhóm. Giáo viên tạo bài, giao bài, theo dõi tiến độ, xem sinh viên đang viết, nhận xét/chỉ lỗi và chấm bài. Sinh viên làm bài trên trình duyệt, tự lưu bản nháp, nộp nhiều lần và xem phản hồi.

Dự án được xây dựng cho bài tập lớn môn **Lập trình mạng**, tập trung vào REST, WebSocket realtime, TCP client/server, xác thực, phân quyền và kiểm thử tải.

## Tính năng chính

### Giáo viên

- Tạo/quản lý bài IELTS Writing nhiều phần và Paper theo nhóm.
- Cấu hình section, số từ, citation style và rubric Paper thang 10.
- Giao bài theo lớp hoặc từng sinh viên; quản lý nhóm Paper.
- Theo dõi tiến độ, online, section đang mở, mất kết nối, rời trang và dán văn bản.
- Xem trực tiếp bài đang viết; nhận xét/chỉ lỗi Paper realtime.
- Chấm IELTS theo rubric, checklist, mã lỗi, nhận xét gắn đoạn văn và so sánh phiên bản.
- Chấm Paper thang 10, mức đạt và điều chỉnh điểm riêng thành viên.
- Quản lý ngân hàng đề, feedback, lớp học và thống kê.

### Sinh viên

- Xem bài được giao, làm IELTS cá nhân hoặc Paper theo nhóm.
- Tự lưu bản nháp, xử lý xung đột phiên bản và lưu offline.
- Paper editor hỗ trợ section, bảng, ảnh, code, công thức, citation IEEE/APA và bibliography.
- Đồng bộ Paper bằng Yjs qua WebSocket.
- Nhận nhận xét trực tiếp của giáo viên; nộp bài và xem lịch sử/kết quả.
- Chat và nhận thông báo realtime.

### Quản trị viên

- Quản lý giáo viên, người dùng, lớp học và phiên đăng nhập.
- Import sinh viên qua web hoặc `admin-cli` TCP.
- Xem audit log và dashboard tổng quan.

## Kiến trúc

```text
Browser ── HTTPS/WSS :443 ── nginx ── frontend (Next.js 16)
                                      │
                                      └── backend (Spring Boot 4 monolith)
                                             ├── REST /api
                                             ├── STOMP /ws
                                             ├── TCP import :9090
                                             ├── scheduled jobs
                                             └── PostgreSQL

admin-cli ── TCP :9090 ──► backend       Mailpit/SMTP ◄── backend
```

Backend là monolith, không dùng microservice và không dùng Redis. Module nghiệp vụ chính: `user`, `security`, `classroom`, `assignment`, `submission`, `grading`, `paper`, `exam`, `questionbank`, `notification`, `stats`, `realtime`, `chat`, `tcpgateway`, `audit`, `demo`.

Frontend dùng Next.js 16, React 19, TypeScript, Ant Design, TipTap và Yjs. Khi chạy local, Next proxy `/api`, `/uploads` và `/ws` về backend qua `next.config.ts`.

## Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring MVC, JPA, Spring Security |
| Database | PostgreSQL 18, Flyway |
| Realtime | WebSocket STOMP, Yjs |
| Frontend | Next.js 16, React 19, TypeScript, Ant Design 6 |
| Editor | TipTap, citation-js, citeproc |
| Email | SMTP/Mailpit |
| TCP | Java TCP server + `admin-cli` |
| Deploy | Docker Compose, nginx, HTTPS/WSS |
| Test | JUnit, integration test, Node unit test, ESLint, TypeScript, k6 |

## Cấu trúc thư mục

```text
backend/                 Spring Boot API, WebSocket, TCP server, test
frontend/                Next.js app, editor và UI
admin-cli/               client TCP import sinh viên
docs/                    tài liệu phân tích/thiết kế chuẩn
docx/                    bản Word sinh từ docs/
deploy/                  Docker/nginx/cert
loadtest/                k6 scripts
shared/                  dữ liệu dùng chung
samples/                 file mẫu import
CLAUDE.md                quy trình và bản đồ repo
AGENTS.md                hướng dẫn cho AI agent
PLAN.md                  kế hoạch
PROGRESS.md              tiến độ thực tế
```

## Yêu cầu môi trường

- Windows 10/11 hoặc Linux.
- JDK 21+, Maven 3.9+, Node.js 22+, npm.
- PostgreSQL 18 hoặc tương thích.
- Docker Desktop nếu chạy bằng Docker.
- `psql` nếu cần kiểm tra database.

## Cấu hình môi trường

Không commit `.env`. Tạo từ file mẫu:

```bash
cp .env.example .env
```

Các biến quan trọng:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=Ielts
DB_USER=postgres
DB_PASSWORD=<mat-khau-postgresql>
JWT_SECRET=<chuoi-bi-mat-dai>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<mat-khau-admin>
```

Tên database phải trùng database thật. Không ghi mật khẩu thật vào README, `launch.json`, Git hoặc log.

## Chạy local bằng Git Bash

### Kiểm tra PostgreSQL

```bash
psql -h localhost -p 5432 -U postgres -d Ielts -W
```

Thoát psql bằng `\q`.

### Backend

```bash
cd /d/Desktop/Ielts-system/backend
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=Ielts
export DB_USER=postgres
export DB_PASSWORD='<mat-khau-postgresql>'
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend: `http://localhost:8080`.

### Frontend

Mở Git Bash thứ hai:

```bash
cd /d/Desktop/Ielts-system/frontend
npm install
npm run dev -- --webpack
```

Frontend: `http://localhost:3000`.

`--webpack` được dùng khi Turbopack bị treo lúc compile trên Windows. Nếu máy chạy Turbopack ổn, có thể dùng `npm run dev`.

### VS Code

`.vscode/launch.json` có compound `Chạy toàn bộ — Backend + Frontend`. Đảm bảo `.env` ở thư mục gốc có `DB_PASSWORD`; không ghi password trực tiếp vào `launch.json`.

## Chạy bằng Docker

```bash
cp .env.example .env
sh deploy/nginx/gen-cert.sh
docker compose up --build -d
```

| Địa chỉ | Mục đích |
|---|---|
| `https://localhost` | ứng dụng web qua nginx |
| `http://localhost:8025` | Mailpit |
| `localhost:9090` | TCP server cho `admin-cli` |

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose down
```

Chấp nhận cảnh báo chứng chỉ tự ký khi truy cập HTTPS.

## Dữ liệu demo

Chỉ bật profile demo khi thử nghiệm:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev,demo
```

Docker dùng `SPRING_PROFILES_ACTIVE=docker,demo`. Profile demo tạo giáo viên, lớp, sinh viên, bài IELTS, Paper và bài nộp mẫu. Mật khẩu lấy từ biến `DEMO_PASSWORD`; không dùng demo cho production.

## Database và migration

Schema do Flyway quản lý tại `backend/src/main/resources/db/migration/`.

- V1–V12: schema lõi, rubric, user session, grading feedback, chat và draft version.
- V13–V15: question bank và exam mode.
- V16–V23: Paper theo nhóm, Paper document/Yjs, references, submission content, rubric Paper và live comments.

Không sửa schema trực tiếp bằng `ddl-auto`. Khi thay đổi DB, tạo migration mới và cập nhật [docs/04-erd.md](docs/04-erd.md).

## Realtime và quyền

| Kênh | Mục đích |
|---|---|
| `/user/queue/notifications` | thông báo riêng |
| `/topic/assignments/{id}/progress` | tiến độ bài |
| `/topic/assignments/{id}/presence` | online/mở bài |
| `/topic/live/parts/{partId}/students/{studentId}` | xem IELTS đang viết |
| `/topic/paper/{documentId}` | update Yjs Paper |
| `/topic/paper/{documentId}/awareness` | con trỏ/trạng thái Yjs |
| `/topic/paper/{documentId}/comments` | nhận xét Paper realtime |
| `/topic/chat/{channel}` | trao đổi |

Quyền subscribe/send phải được kiểm tra ở backend, không chỉ ở frontend.

## API

REST, WebSocket, TCP và mã lỗi được mô tả tại [docs/05-api.md](docs/05-api.md). Request mẫu nằm trong `backend/http/`.

```text
POST /api/auth/login
POST /api/auth/refresh
GET  /api/teacher/assignments/{id}/progress
GET  /api/papers/docs/{id}
PUT  /api/papers/docs/{id}/snapshot
GET  /api/papers/docs/{id}/comments
POST /api/teacher/papers/docs/{id}/comments
```

## Kiểm thử

```bash
# backend
cd backend
mvn test

# frontend
cd ../frontend
npm run lint
npx tsc --noEmit
npm run test:unit
npm run build

# admin-cli
cd ../admin-cli
mvn test
mvn package
```

Kiểm thử tải k6 xem tại [docs/09-kiem-thu-tai.md](docs/09-kiem-thu-tai.md).

## Dừng ứng dụng

Trong terminal đang chạy, nhấn `Ctrl+C`. Hoặc từ Git Bash dừng riêng các cổng của project:

```bash
powershell.exe -NoProfile -Command 'Get-NetTCPConnection -LocalPort 3000,8080,9090 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force }'
```

Lệnh trên không dừng PostgreSQL.

## Tài liệu kiến trúc

Đọc theo thứ tự:

1. [CLAUDE.md](CLAUDE.md) — bản đồ repo và quy tắc làm việc.
2. [docs/01-tong-quan.md](docs/01-tong-quan.md) — yêu cầu và kiến trúc.
3. [docs/02-use-case.md](docs/02-use-case.md) — chức năng theo vai trò.
4. [docs/03-sequence.md](docs/03-sequence.md) — luồng xử lý.
5. [docs/04-erd.md](docs/04-erd.md) — database và migration.
6. [docs/05-api.md](docs/05-api.md) — REST/WebSocket/TCP API.
7. [docs/06-trang-thai.md](docs/06-trang-thai.md) — trạng thái nghiệp vụ.
8. [docs/08-nghiep-vu-cham-ielts.md](docs/08-nghiep-vu-cham-ielts.md) — nghiệp vụ chấm IELTS.
9. [docs/12-viet-paper.md](docs/12-viet-paper.md) — Paper theo nhóm.
10. [PROGRESS.md](PROGRESS.md) — trạng thái triển khai hiện tại.

Prompt hoàn thiện chức năng: [PROMPT-HOAN-THIEN-HE-THONG.md](PROMPT-HOAN-THIEN-HE-THONG.md).

## Quy tắc đóng góp

- Đọc `AGENTS.md` và `CLAUDE.md` trước khi sửa.
- Không commit `.env`, `uploads/`, `node_modules/`, `target/`, `.next/` hoặc secret.
- Không commit `cbs-fe.zip`/`cbs-fe/`.
- Không dùng `git push --force` trên `main`.
- Trước khi push, đồng bộ remote bằng `git pull --rebase origin main` và xử lý conflict nếu có.
- Mỗi thay đổi lớn phải có test và cập nhật tài liệu tương ứng.

## Tiến độ

Xem [PROGRESS.md](PROGRESS.md) để biết phần đã hoàn thành, phần đang làm và tồn đọng. Kế hoạch chi tiết nằm trong [PLAN.md](PLAN.md).
