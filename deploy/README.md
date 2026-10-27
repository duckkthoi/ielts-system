# Chạy toàn bộ hệ thống bằng Docker (bước 13)

> Hướng dẫn đầy đủ từ lúc clone, đưa lên máy chủ có tên miền (Let's Encrypt, đóng cổng 8025 / 9090, email thật), cập nhật và sao lưu: [docs/11](../docs/11-huong-dan-trien-khai.md).

Một lệnh dựng đủ 5 container: **PostgreSQL 18, Mailpit, backend (Spring Boot), frontend (Next.js), nginx**. nginx là cổng vào duy nhất cho web (HTTPS / WSS); server TCP import mở thẳng cổng 9090 cho `admin-cli`.

```
                 ┌──────────── máy chủ ─────────────────────────────────────────────┐
trình duyệt ──443 HTTPS / WSS──▶ nginx ──▶ frontend:3000                            │
                 │                    └──▶ backend:8080 (/api, /ws, /uploads) ──▶ postgres:5432
admin-cli ──9090 TCP────────────────────▶ backend:9090                    └──▶ mailpit:1025 (SMTP)
người dùng ──8025──▶ mailpit (xem email)                                             │
                 └──────────────────────────────────────────────────────────────────┘
```

## Chuẩn bị (một lần)

1. Cài Docker Desktop và bật lên.
2. Ở thư mục gốc dự án:
   ```
   cp .env.example .env          # rồi sửa DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD
   sh deploy/nginx/gen-cert.sh   # tạo chứng chỉ tự ký vào deploy/nginx/certs/ (Git Bash trên Windows)
   ```
   `JWT_SECRET` nên sinh ngẫu nhiên: `openssl rand -base64 48`. File `.env` và thư mục `certs/` không commit.

## Chạy

```
docker compose up --build -d      # lần đầu build mất vài phút (tải thư viện Maven / npm)
docker compose ps                 # backend "healthy" là sẵn sàng
docker compose logs -f backend    # xem log backend
```

| Địa chỉ | Dùng để |
|---|---|
| https://localhost | Giao diện web (trình duyệt cảnh báo chứng chỉ tự ký → chấp nhận để vào) |
| https://localhost/api/… | REST API qua nginx |
| wss://localhost/ws | WebSocket STOMP qua nginx |
| http://localhost:8025 | Mailpit — xem email hệ thống gửi |
| localhost:9090 | Server TCP import — dùng `admin-cli` |

Đăng nhập lần đầu bằng `admin` / mật khẩu `ADMIN_PASSWORD` trong `.env`; hệ thống **bắt đổi mật khẩu ngay** (BR-02). `ADMIN_PASSWORD` chỉ dùng khi tạo admin ở lần chạy đầu — đổi biến này sau đó không đổi mật khẩu đã lưu.

Import qua TCP vào hệ thống đang chạy trong Docker:
```
cd admin-cli && mvn -q package
java -jar target/admin-cli.jar import --api https://localhost --insecure --user <giáo viên> --class <mã lớp> --file ds.csv
```
(`--insecure` chỉ để chấp nhận chứng chỉ tự ký khi đăng nhập qua HTTPS; kết nối TCP 9090 là text thuần, chỉ dùng trong mạng tin cậy.)

## Dữ liệu mẫu và kiểm thử tải

- Đặt `SPRING_PROFILES_ACTIVE=docker,demo` trong `.env` rồi `docker compose up -d` → backend tạo sẵn giáo viên `gv.demo`, lớp `DEMO-A` / `DEMO-B`, 20 sinh viên, 2 bài (danh sách tài khoản: [README gốc](../README.md) mục "Dữ liệu mẫu"). Đã có dữ liệu mẫu thì lần sau bỏ qua.
- Kiểm thử tải k6 (bước 14): `docker compose -f docker-compose.yml -f loadtest/compose.loadtest.yml up --build -d`, sau đó `… run --rm k6 run submit-rush.js` và `… run --rm k6 run ws-connections.js`. Kết quả và cách đọc: [docs/09](../docs/09-kiem-thu-tai.md). Muốn không ghi dữ liệu thử vào volume đang dùng thì thêm `-p ielts-lt` cho mọi lệnh và xoá bằng `down -v` sau khi đo.

## Các thao tác hay dùng

| Việc | Lệnh |
|---|---|
| Dừng (giữ dữ liệu) | `docker compose down` |
| Xoá cả dữ liệu DB và file upload | `docker compose down -v` |
| Build lại sau khi sửa code backend | `docker compose up --build -d backend` |
| Khởi động lại backend (kiểm tra client tự kết nối lại — NFR-04) | `docker compose restart backend` |
| Vào psql | `docker compose exec postgres psql -U ielts -d ielts` |

## Ghi chú cấu hình

- Backend chạy profile `docker` (`application-docker.yml`): host DB / mail là tên service; `server.forward-headers-strategy: framework` để lấy IP thật từ `X-Forwarded-For` (chỉ nginx đặt được vì cổng 8080 không mở ra ngoài).
- DB, email, upload nằm trong volume `pgdata`, `uploads`.
- Biến `PUBLIC_URL` / `PUBLIC_WS_URL` trong `.env` đổi khi chạy bằng tên miền khác `localhost` (dùng cho link trong email, CORS, địa chỉ WebSocket của frontend — biến của frontend gắn lúc build, đổi thì phải build lại).
