# 11. Hướng dẫn clone và triển khai

> Dành cho người muốn tự dựng hệ thống: lấy mã nguồn về, chạy thử trên máy, hoặc đưa lên một máy chủ Linux có tên miền.
> Cách dùng hệ thống sau khi chạy (tài khoản, thao tác theo vai trò): [10 Hướng dẫn sử dụng](10-huong-dan-su-dung.md).
> Cập nhật: 27/10/2026.

---

## 1. Chọn cách chạy

| Cách | Khi nào dùng | Cần gì |
|---|---|---|
| **A. Docker trên máy cá nhân** (mục 3) | Demo, chấm đồ án, chạy thử nhanh | Docker Desktop |
| **B. Docker trên máy chủ Linux có tên miền** (mục 4) | Cho người khác truy cập qua Internet bằng HTTPS thật | VPS Linux, tên miền, Docker |
| **C. Chạy từng phần khi phát triển** (mục 5) | Sửa code backend / frontend | JDK 21, Maven, Node 22, PostgreSQL 18 |

Cả A và B đều dựng cùng 5 container: **PostgreSQL 18, Mailpit, backend (Spring Boot), frontend (Next.js), nginx**. Chỉ nginx (80/443) và server TCP import (9090) mở ra ngoài; backend 8080 và PostgreSQL chỉ nằm trong mạng nội bộ Docker. Sơ đồ: [deploy/README.md](../deploy/README.md).

---

## 2. Clone mã nguồn

```
git clone https://github.com/duckkthoi/ielts-system.git
cd ielts-system
```

Repository riêng tư thì cần được chủ repo mời làm cộng tác viên, rồi đăng nhập GitHub (Git Credential Manager trên Windows tự hỏi; trên Linux dùng SSH key hoặc personal access token).

Sau khi clone, những thứ sau **không có sẵn** trong repo (cố ý không commit) và phải tự tạo:

| Thứ cần tạo | Tạo bằng | Ghi chú |
|---|---|---|
| `.env` | copy từ `.env.example` | Mật khẩu DB, khoá JWT, mật khẩu admin |
| `deploy/nginx/certs/server.crt`, `server.key` | `sh deploy/nginx/gen-cert.sh` (tự ký) hoặc Let's Encrypt (mục 4.4) | Thiếu thì nginx không khởi động |
| `tools/mailpit/` | tải Mailpit (chỉ cần cho cách C) | Docker đã có sẵn Mailpit |

Trên Windows nên để Git giữ nguyên ký tự xuống dòng của file `.sh` (repo dùng LF). Nếu `sh deploy/nginx/gen-cert.sh` báo lỗi lạ kiểu `$'\r': command not found` thì chạy `git config core.autocrlf input` rồi clone lại.

---

## 3. Cách A — Docker trên máy cá nhân

### 3.1. Chuẩn bị (một lần)

1. Cài **Docker Desktop** (Windows / macOS) hoặc Docker Engine + plugin compose (Linux), bật lên. Nên cấp cho Docker ít nhất 4 GB RAM vì bước build Maven và Next.js khá nặng.
2. Ở thư mục gốc dự án, mở **Git Bash** (Windows) hoặc terminal:
   ```
   cp .env.example .env
   ```
3. Sửa `.env`:

   | Biến | Đặt thành |
   |---|---|
   | `DB_PASSWORD` | Mật khẩu bất kỳ cho DB trong container |
   | `JWT_SECRET` | Chuỗi ngẫu nhiên ≥ 32 ký tự: `openssl rand -base64 48` |
   | `ADMIN_PASSWORD` | Mật khẩu lần đầu của tài khoản `admin` (nên dài, có chữ hoa, chữ thường, số, ký tự đặc biệt) |
   | `SPRING_PROFILES_ACTIVE` | `docker` · muốn có sẵn dữ liệu mẫu để demo thì `docker,demo` |
   | `PUBLIC_URL`, `PUBLIC_WS_URL` | Giữ `https://localhost`, `wss://localhost/ws` |

4. Tạo chứng chỉ tự ký:
   ```
   sh deploy/nginx/gen-cert.sh
   ```

### 3.2. Chạy

```
docker compose up --build -d      # lần đầu 5–10 phút (tải thư viện Maven / npm)
docker compose ps                 # chờ backend chuyển sang "healthy"
```

Mở https://localhost → trình duyệt cảnh báo chứng chỉ tự ký → chọn tiếp tục. Đăng nhập `admin` / `ADMIN_PASSWORD`, hệ thống bắt đổi mật khẩu ngay. Có profile `demo` thì dùng luôn các tài khoản mẫu (`gv.demo`, `DEMO001` …, mật khẩu `Demo@2026`) — danh sách đủ ở [docs/10](10-huong-dan-su-dung.md).

| Địa chỉ | Dùng để |
|---|---|
| https://localhost | Giao diện web; REST `/api/…`, WebSocket `wss://localhost/ws` |
| http://localhost:8025 | Mailpit — xem email hệ thống gửi (email không ra Internet) |
| `localhost:9090` | Server TCP cho `admin-cli` |

### 3.3. Cho máy khác trong cùng mạng LAN truy cập

1. Xem IP máy chạy Docker (vd `192.168.1.20`).
2. Trong `.env`: `PUBLIC_URL=https://192.168.1.20`, `PUBLIC_WS_URL=wss://192.168.1.20/ws`.
3. Tạo lại chứng chỉ có IP đó: sửa dòng `-addext` trong `gen-cert.sh` thêm `IP:192.168.1.20`, hoặc chấp nhận cảnh báo trên trình duyệt.
4. `docker compose up --build -d` (**phải build lại** vì `PUBLIC_WS_URL` được gắn vào frontend lúc build).
5. Mở cổng 443 (và 9090 nếu dùng `admin-cli` từ máy khác) trên tường lửa Windows.

---

## 4. Cách B — Máy chủ Linux có tên miền

Ví dụ dưới dùng Ubuntu 24.04 và tên miền `ielts.example.com`; thay bằng tên miền thật.

### 4.1. Máy chủ

| Mục | Tối thiểu | Khuyên dùng |
|---|---|---|
| CPU / RAM | 2 vCPU, 2 GB RAM + 2 GB swap | 2 vCPU, 4 GB RAM |
| Ổ đĩa | 20 GB | 40 GB (ảnh Docker, DB, file upload) |
| Cổng mở | 22, 80, 443 | + 9090 nếu import từ xa bằng `admin-cli` |

Máy 2 GB RAM build được nhưng chậm; nếu build bị "Killed" thì thêm swap hoặc build ảnh trên máy khác.

### 4.2. Cài Docker và trỏ tên miền

```
# Docker Engine + compose plugin (script chính thức của Docker)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER          # đăng xuất / đăng nhập lại để có hiệu lực
docker compose version                  # cần ≥ 2.24 (dùng cú pháp !override ở mục 4.5)
```

Ở nhà cung cấp tên miền, tạo bản ghi **A** `ielts.example.com` → IP của máy chủ. Kiểm tra: `ping ielts.example.com` ra đúng IP.

### 4.3. Lấy mã nguồn và cấu hình

```
git clone https://github.com/duckkthoi/ielts-system.git
cd ielts-system
cp .env.example .env
nano .env
```

| Biến | Giá trị |
|---|---|
| `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD` | Như mục 3.1, nhưng **bắt buộc** đổi khỏi giá trị mẫu |
| `PUBLIC_URL` | `https://ielts.example.com` |
| `PUBLIC_WS_URL` | `wss://ielts.example.com/ws` |
| `SPRING_PROFILES_ACTIVE` | `docker` — **không** bật `demo` cho hệ thống thật (tài khoản mẫu dùng chung một mật khẩu) |

`PUBLIC_URL` được dùng cho link trong email, danh sách CORS và kiểm tra `Origin` của WebSocket; sai giá trị này thì trang mở được nhưng đăng nhập / realtime bị chặn.

### 4.4. Chứng chỉ HTTPS thật (Let's Encrypt)

nginx đọc chứng chỉ ở `deploy/nginx/certs/server.crt` và `server.key`. Lấy chứng chỉ miễn phí bằng certbot ở chế độ standalone (cần cổng 80 còn trống, tức là **chạy trước khi** bật nginx):

```
sudo apt install -y certbot
sudo certbot certonly --standalone -d ielts.example.com -m ban@example.com --agree-tos -n

mkdir -p deploy/nginx/certs
sudo cp /etc/letsencrypt/live/ielts.example.com/fullchain.pem deploy/nginx/certs/server.crt
sudo cp /etc/letsencrypt/live/ielts.example.com/privkey.pem  deploy/nginx/certs/server.key
sudo chmod 644 deploy/nginx/certs/server.crt && sudo chmod 600 deploy/nginx/certs/server.key
```

Chứng chỉ hết hạn sau 90 ngày. Tự gia hạn: tạo 2 hook để certbot tạm dừng nginx trước khi gia hạn và chép lại chứng chỉ sau đó (thay `/home/ubuntu/ielts-system` bằng đường dẫn thật):

```
sudo tee /etc/letsencrypt/renewal-hooks/pre/ielts.sh >/dev/null <<'EOF'
#!/bin/sh
cd /home/ubuntu/ielts-system && docker compose stop nginx
EOF
sudo tee /etc/letsencrypt/renewal-hooks/post/ielts.sh >/dev/null <<'EOF'
#!/bin/sh
D=/home/ubuntu/ielts-system/deploy/nginx/certs
cp /etc/letsencrypt/live/ielts.example.com/fullchain.pem $D/server.crt
cp /etc/letsencrypt/live/ielts.example.com/privkey.pem  $D/server.key
cd /home/ubuntu/ielts-system && docker compose start nginx
EOF
sudo chmod +x /etc/letsencrypt/renewal-hooks/pre/ielts.sh /etc/letsencrypt/renewal-hooks/post/ielts.sh
sudo certbot renew --dry-run           # thử quy trình gia hạn
```

(Gói certbot trên Ubuntu đã có sẵn timer chạy `certbot renew` hai lần mỗi ngày.)

Không có tên miền thì vẫn chạy được bằng IP + chứng chỉ tự ký: `sh deploy/nginx/gen-cert.sh <IP>`, đặt `PUBLIC_URL=https://<IP>`.

### 4.5. Đóng các cổng không nên lộ ra Internet

`docker-compose.yml` mở cổng 8025 (Mailpit, **không có mật khẩu**, đọc được email chứa link đặt lại mật khẩu) và 9090 (TCP import, **không mã hoá**) ra mọi địa chỉ. Trên máy chủ thật, chỉ cho hai cổng này nghe ở `127.0.0.1` bằng file `docker-compose.override.yml` đặt cạnh `docker-compose.yml` (compose tự đọc file này):

```yaml
services:
  mailpit:
    ports: !override
      - "127.0.0.1:8025:8025"
  backend:
    ports: !override
      - "127.0.0.1:9090:9090"
```

Lưu ý: cổng do Docker mở **đi vòng qua `ufw`**, nên chặn bằng `ufw deny 8025` không có tác dụng — phải dùng file override như trên. Tường lửa vẫn nên bật cho các cổng khác:

```
sudo ufw allow OpenSSH && sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw enable
```

Muốn xem Mailpit hoặc dùng `admin-cli` từ máy mình thì đi qua đường hầm SSH:

```
ssh -L 8025:localhost:8025 -L 9090:localhost:9090 ubuntu@ielts.example.com
# rồi mở http://localhost:8025 trên máy mình, hoặc:
java -jar target/admin-cli.jar import --api https://ielts.example.com --host localhost --port 9090 \
     --user <giáo viên> --class <mã lớp> --file ds.csv
```

### 4.6. Gửi email thật (tuỳ chọn)

Mailpit chỉ giữ email lại để xem, không gửi ra ngoài. Muốn sinh viên nhận được email (tài khoản mới, nhắc hạn, đặt lại mật khẩu), thêm cấu hình SMTP thật vào `docker-compose.override.yml`, vd với Gmail (cần bật xác minh 2 bước và tạo *App password*):

```yaml
services:
  backend:
    environment:
      MAIL_HOST: smtp.gmail.com
      MAIL_PORT: "587"
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      MAIL_SMTP_AUTH: "true"
      MAIL_STARTTLS: "true"
      MAIL_FROM: "IELTS Writing <${MAIL_USERNAME}>"
```

và thêm `MAIL_USERNAME=…`, `MAIL_PASSWORD=…` vào `.env` (không ghi mật khẩu SMTP thẳng vào file yml).

### 4.7. Chạy và kiểm tra

```
docker compose up --build -d
docker compose ps                       # backend "healthy", các container khác "running"
docker compose logs -f backend          # Ctrl+C để thoát
```

Kiểm tra từ máy khác:

1. Mở https://ielts.example.com — ổ khoá xanh, không cảnh báo chứng chỉ.
2. Đăng nhập `admin` / `ADMIN_PASSWORD` → đổi mật khẩu ngay.
3. Biểu tượng kết nối realtime trên header hiện "đã kết nối" (WebSocket qua `wss://` hoạt động).
4. `http://ielts.example.com` tự chuyển sang `https://`.

Tạo giáo viên ở trang quản trị, rồi giáo viên tạo lớp và import sinh viên (giao diện web hoặc `admin-cli`).

---

## 5. Cách C — Chạy từng phần khi phát triển

Cần JDK 21+, Maven 3.9, Node 22, PostgreSQL 18. Tạo user và 2 DB (chạy bằng tài khoản superuser của PostgreSQL):

```sql
CREATE USER ielts WITH PASSWORD 'ielts';
CREATE DATABASE ielts OWNER ielts;
CREATE DATABASE ielts_test OWNER ielts;
```

| Việc | Lệnh | Địa chỉ |
|---|---|---|
| Email (tuỳ chọn) | tải [Mailpit](https://mailpit.axllent.org) vào `tools/mailpit/` → `tools\mailpit\mailpit.exe` | http://localhost:8025 |
| Backend | `cd backend` → `mvn spring-boot:run` (thêm `-Dspring-boot.run.profiles=dev,demo` để có dữ liệu mẫu) | http://localhost:8080, Swagger: `/swagger-ui.html` |
| Frontend | `cd frontend` → `npm install` → `npm run dev` | http://localhost:3000 |
| Test backend | `cd backend` → `mvn test` (dùng DB `ielts_test`) | |
| Kiểm tra frontend | `npm run lint` · `npm run build` · `npm run test:unit` | |

Bảng và dữ liệu khởi tạo do Flyway tự tạo ở lần chạy backend đầu tiên; không cần chạy file SQL bằng tay. Khi dev, mật khẩu `admin` mặc định là `Admin@123` (bắt đổi lần đầu).

---

## 6. Cập nhật lên phiên bản mới

```
cd ielts-system
git pull
docker compose up --build -d            # build lại ảnh nào có thay đổi, rồi khởi động lại
docker image prune -f                   # xoá ảnh cũ cho đỡ tốn ổ đĩa
```

Migration DB mới (Flyway `V…__*.sql`) tự chạy khi backend khởi động; dữ liệu cũ trong volume `pgdata` và `uploads` được giữ. Nên **sao lưu trước** khi cập nhật (mục 7). Trong lúc build lại có thể mất kết nối khoảng một phút; giao diện tự kết nối lại khi backend sẵn sàng.

---

## 7. Sao lưu và khôi phục

Dữ liệu nằm ở 2 volume: `ielts_pgdata` (DB) và `ielts_uploads` (ảnh đề, file đính kèm) — tiền tố `ielts_` là tên project trong `docker-compose.yml`.

```
# Sao lưu
mkdir -p backup
docker compose exec -T postgres pg_dump -U ielts -d ielts -Fc > backup/ielts-$(date +%F).dump
docker run --rm -v ielts_uploads:/data -v "$PWD/backup:/backup" alpine \
    tar czf /backup/uploads-$(date +%F).tar.gz -C /data .

# Khôi phục (DB sẽ bị ghi đè)
docker compose stop backend
docker compose exec -T postgres pg_restore -U ielts -d ielts --clean --if-exists < backup/ielts-2026-10-27.dump
docker run --rm -v ielts_uploads:/data -v "$PWD/backup:/backup" alpine \
    sh -c 'tar xzf /backup/uploads-2026-10-27.tar.gz -C /data && chown -R 1001 /data'
docker compose start backend
```

(`chown 1001`: backend chạy bằng user thường có uid 1001, cần quyền ghi vào thư mục upload.)

Muốn sao lưu tự động hằng ngày thì đưa 2 lệnh sao lưu vào `crontab -e`. File sao lưu chứa dữ liệu cá nhân của sinh viên và bài làm — cất ở nơi an toàn, không đưa vào Git.

---

## 8. Sự cố thường gặp khi triển khai

| Hiện tượng | Nguyên nhân / cách xử lý |
|---|---|
| `docker compose up` báo `Đặt DB_PASSWORD trong .env` | Chưa tạo `.env` hoặc thiếu biến — xem mục 3.1 |
| nginx khởi động rồi tắt, log báo không mở được `server.crt` | Chưa tạo chứng chỉ (mục 3.1 bước 4 hoặc 4.4) |
| nginx chờ mãi, backend không lên "healthy" | `docker compose logs backend`: thường là sai `DB_PASSWORD` sau khi đã tạo volume (Postgres chỉ đặt mật khẩu ở lần đầu). Đổi lại mật khẩu cũ, hoặc xoá volume `docker compose down -v` (mất dữ liệu) |
| Build bị `Killed` / `exit code 137` | Thiếu RAM — thêm swap hoặc tăng RAM cho Docker Desktop |
| Trang mở được nhưng đăng nhập báo lỗi CORS / 403, hoặc realtime không kết nối | `PUBLIC_URL` / `PUBLIC_WS_URL` không khớp địa chỉ đang gõ trên trình duyệt. Sửa `.env` rồi `docker compose up --build -d` (frontend phải build lại) |
| Đổi `ADMIN_PASSWORD` trong `.env` nhưng không đăng nhập được bằng mật khẩu mới | Biến này chỉ dùng ở lần tạo admin đầu tiên. Xem cách xử lý quên mật khẩu admin ở [docs/10](10-huong-dan-su-dung.md) mục 9 |
| `certbot` báo không kết nối được cổng 80 | nginx đang chiếm cổng 80 (`docker compose stop nginx`), hoặc tường lửa / nhà cung cấp VPS chưa mở 80, hoặc bản ghi DNS chưa trỏ đúng |
| `admin-cli` báo `Connection refused` cổng 9090 | Đã giới hạn 9090 về `127.0.0.1` (mục 4.5) — dùng đường hầm SSH; hoặc backend chưa chạy xong |
| Sinh viên không nhận được email | Đang dùng Mailpit (email chỉ nằm ở http://localhost:8025) — cấu hình SMTP thật ở mục 4.6 |
