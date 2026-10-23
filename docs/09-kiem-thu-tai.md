# 09. Kiểm thử tải

> Kiểm tra các yêu cầu hiệu năng và realtime trong [01-tong-quan.md](01-tong-quan.md) mục NFR: **NFR-01** (200 sinh viên nộp bài trong 5 phút cuối, p95 < 500 ms, lỗi < 1%), **NFR-02** (500 kết nối WebSocket đồng thời), **NFR-03** (thông báo tới người nhận dưới 1 giây).
> Script ở thư mục [`loadtest/`](../loadtest/), chạy bằng **k6** trong Docker. Đo ngày 23/10/2026 (bước 14).

---

## 1. Kết quả tóm tắt

| Yêu cầu | Ngưỡng | Kết quả đo | Đạt |
|---|---|---|---|
| NFR-01 — thời gian phản hồi API khi 200 sinh viên nộp sát hạn | p95 < 500 ms | **p95 = 9,9 ms** (p99 = 71,7 ms, lớn nhất 148 ms) trên 9 031 request | ✅ |
| NFR-01 — tỉ lệ lỗi | < 1% | **0%** (0 / 9 031 request); 200 / 200 bài nộp thành công, không bài nào bị tính muộn | ✅ |
| NFR-02 — kết nối WebSocket đồng thời | 500 | **500 / 500** kết nối STOMP mở cùng lúc, giữ từ 1 phút 40 giây đến 2 phút 39 giây, 0 khung `ERROR` | ✅ |
| NFR-03 — độ trễ thông báo | < 1 s | Một lần nhắc 500 sinh viên: **500 / 500** kết nối nhận được, **p95 = 228 ms** (lớn nhất 228 ms) tính từ lúc thông báo được tạo | ✅ |

Ở mức tải yêu cầu, hệ thống còn dư nhiều: lúc cao điểm backend dùng khoảng **7% CPU** (trên 12 luồng) và **840 MB RAM**, PostgreSQL khoảng 3% CPU.

---

## 2. Môi trường đo

| Thành phần | Cấu hình |
|---|---|
| Máy | Laptop Intel Core i5-12450HX (12 luồng), RAM 16 GB; Docker Desktop 29.9 (WSL 2) được cấp 12 CPU, 7,6 GB RAM |
| Hệ thống | `docker-compose.yml` của bước 13 (postgres 18, mailpit, backend, frontend, nginx 1.27) + [`loadtest/compose.loadtest.yml`](../loadtest/compose.loadtest.yml): backend chạy profile `docker,demo`, lớp kiểm thử tải 500 sinh viên |
| Công cụ | k6 0.54 (image `grafana/k6:0.54.0`) chạy **trong cùng mạng Docker**, gọi `https://nginx` và `wss://nginx/ws` — đi qua nginx, TLS và reverse proxy giống trình duyệt thật |
| Cấu hình backend | Mặc định của Spring Boot (Tomcat 200 luồng, HikariCP 10 kết nối DB), simple broker STOMP trong bộ nhớ, một instance (monolith) |

**Giới hạn của phép đo:** k6 và hệ thống chạy trên **cùng một máy**, nên không có độ trễ mạng thật giữa client và server, và k6 dùng chung CPU với server. Mọi tài khoản dùng chung một IP (container k6), nên phép đo này không kiểm tra giới hạn đăng nhập sai theo IP (NFR-09 — đã có test tự động riêng).

---

## 3. Dữ liệu

Backend bật profile `demo` với `DEMO_LOAD_STUDENTS=500`: lúc khởi động, `DemoSeeder` tạo giáo viên `gv.demo`, lớp `DEMO-LOAD` gồm 500 sinh viên `DEMO-T0001` … `DEMO-T0500`. Các tài khoản này dùng chung mật khẩu `DEMO_PASSWORD` và không bị bắt đổi mật khẩu (xem [README](../README.md) mục "Dữ liệu mẫu"). Ở `setup()`, mỗi kịch bản cho giáo viên tạo một **bài mới** gồm một phần Task 2 với rubric IELTS mẫu, rồi giao cho lớp `DEMO-LOAD` (tạo thông báo và email cho 500 người). Nhờ vậy, lần chạy nào sinh viên cũng còn phần chưa nộp.

---

## 4. Kịch bản

### 4.1 `submit-rush.js` — nộp bài sát hạn (NFR-01)

Bài được tạo với **hạn sau 6 phút**, nên mọi lượt nộp đều rơi vào 5 phút cuối. Có 200 VU, mỗi VU là một sinh viên:

1. Đăng nhập vào một thời điểm ngẫu nhiên trong 30 giây đầu (BCrypt là thao tác tốn CPU nhất của luồng).
2. Mở danh sách bài (tab "Cần làm"), mở chi tiết bài, nạp bản nháp.
3. Vừa "gõ" vừa **tự lưu nháp mỗi 3–5 giây** (FR-32), mỗi lần gửi kèm `draftVersion` (khoá lạc quan, SD-06).
4. **Nộp bài** vào một thời điểm ngẫu nhiên trong khoảng từ phút 1 đến phút 4,8 (SD-07), rồi xem lịch sử nộp.

Cùng lúc đó, một giáo viên mở **ma trận tiến độ** của bài mỗi 5 giây. Đây là truy vấn tổng hợp nặng nhất trong luồng này.

| Loại request | Số lượng (xấp xỉ) | p95 |
|---|---|---|
| Lưu nháp `PUT /api/student/parts/{id}/draft` | 8 000 | 8,4 ms |
| Nộp bài `POST …/submit` | 200 | 10,5 ms |
| Toàn bộ request của sinh viên (kể cả đăng nhập) | 9 000 | 9,9 ms |

Tổng cộng 9 031 request trong 5 phút (trung bình 30 request/giây), 100% check đạt (9 227 / 9 227).

### 4.2 `ws-connections.js` — 500 kết nối WebSocket + độ trễ thông báo (NFR-02, NFR-03)

Có 500 VU, mỗi VU là một sinh viên. Các kết nối mở rải đều trong 60 giây. Mỗi VU:

1. Đăng nhập, mở `wss://…/ws`, gửi STOMP `CONNECT` kèm `Authorization: Bearer …` và `heart-beat: 10000,10000`.
2. `SUBSCRIBE /user/queue/notifications`, gửi heart-beat phía client mỗi 8 giây.
3. Giữ kết nối tới mốc chung (giây 160), sau đó gửi `DISCONNECT`.

Ở **giây 100**, khi cả 500 kết nối đang mở, giáo viên bấm **"Nhắc nhở"** cho cả 500 sinh viên bằng một request `POST /api/teacher/assignments/{id}/remind` (mất 243 ms). Request này tạo 500 thông báo `MANUAL_REMINDER` trong một transaction và đẩy qua WebSocket sau commit. Độ trễ được tính bằng giờ nhận ở client trừ `createdAt` của thông báo. Client và server dùng chung đồng hồ của máy chạy Docker.

| Chỉ số | Kết quả |
|---|---|
| Bắt tay WebSocket (`101`) | 500 / 500, p95 5,5 ms |
| STOMP `CONNECTED` (tính từ lúc mở socket) | 500 / 500, p95 10 ms |
| Khung `ERROR` / lỗi socket | 0 |
| Thông báo nhận được | 500 / 500 |
| Độ trễ thông báo | trung bình 216 ms, p95 228 ms, lớn nhất 228 ms |
| Đăng nhập (500 lượt trong 60 giây) | p95 84 ms |

Độ trễ thông báo gần như không đổi giữa người nhận đầu và người nhận cuối (193 → 228 ms). Phần lớn thời gian nằm ở transaction ghi 500 thông báo, vì thông báo chỉ được đẩy **sau commit** (quy tắc "lưu trước, đẩy sau" ở [03-sequence.md](03-sequence.md) SD-09); việc đẩy qua 500 kết nối chỉ mất vài chục mili giây.

---

## 5. Nhận xét

- **Đồng thời khi nộp:** 200 lượt nộp và khoảng 8 000 lượt lưu nháp chạy chồng nhau mà không có lỗi `409` ngoài ý muốn, không bài nào bị tính muộn. Khoá dòng `FOR UPDATE` trên `submissions` (SD-07) không gây nghẽn, vì mỗi sinh viên chỉ khoá dòng của chính mình.
- **Kết nối dài:** 500 kết nối STOMP qua nginx dùng rất ít tài nguyên. Simple broker trong bộ nhớ đủ cho quy mô một lớp / một khoa, chưa cần broker ngoài (đúng quyết định monolith, không Redis).
- **Điểm tốn tài nguyên nhất là đăng nhập** (BCrypt): 500 lượt đăng nhập trong 60 giây có p95 84 ms, cao hơn hẳn các API khác (dưới 10 ms). Nếu cần chịu đợt đăng nhập lớn hơn (vd cả trường vào thi cùng lúc), đây là chỗ cần xem trước, chưa phải DB hay WebSocket.
- Mức tải theo yêu cầu mới dùng một phần nhỏ tài nguyên. Muốn tìm giới hạn thật thì tăng `STUDENTS` / `CONNECTIONS` (biến môi trường của k6) và chạy k6 từ một máy khác trong mạng LAN.

---

## 6. Cách chạy lại

```
# 1. Dựng hệ thống ở chế độ kiểm thử tải (profile demo + 500 sinh viên tải). Cần .env và chứng chỉ như bước 13.
docker compose -f docker-compose.yml -f loadtest/compose.loadtest.yml up --build -d

# 2. Chạy từng kịch bản (khoảng 5 phút và 3 phút). k6 in bảng kết quả; ngưỡng nào không đạt thì lệnh trả mã lỗi.
docker compose -f docker-compose.yml -f loadtest/compose.loadtest.yml run --rm k6 run submit-rush.js
docker compose -f docker-compose.yml -f loadtest/compose.loadtest.yml run --rm k6 run ws-connections.js

# Tuỳ chọn: đổi quy mô
docker compose -f docker-compose.yml -f loadtest/compose.loadtest.yml run --rm -e CONNECTIONS=300 k6 run ws-connections.js
```

Ngưỡng đạt / không đạt được khai báo trong `options.thresholds` của từng script, khớp với NFR ở mục 1. Số liệu ở trên được đo bằng một project Docker riêng (`docker compose -p ielts-lt …`) để dữ liệu thử không ghi vào volume của hệ thống đang dùng; đo xong xoá bằng `down -v`.
