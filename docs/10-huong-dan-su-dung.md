# 10. Hướng dẫn sử dụng

> Dành cho người chạy thử, người demo và người chấm đồ án: mở hệ thống ở đâu, đăng nhập bằng tài khoản nào, từng vai trò làm được gì.
> Cập nhật: 28/10/2026, theo trạng thái giao diện F1 → F6 và Ngân hàng đề B17 (xem mục 1).

---

## 1. Trạng thái giao diện

| Phần | Giao diện | Ghi chú |
|---|---|---|
| Đăng nhập, đổi mật khẩu, thông tin cá nhân, khung ứng dụng, thông báo realtime | ✅ Dùng được | |
| Quản trị viên: tổng quan, giáo viên, người dùng, nhật ký | ✅ Dùng được | Trang "Phiên đăng nhập" chỉ hiện số phiên (danh sách chi tiết thuộc Mức 3) |
| Giáo viên: lớp học, import sinh viên, bài tập (soạn, giao, phát hành) | ✅ Dùng được | |
| Giáo viên: chấm bài (hàng đợi, chấm theo tiêu chí, đánh dấu trên bài kiểu Word, nhận xét mẫu, thống kê lỗi, trả bài, yêu cầu sửa, so sánh 2 lần nộp) | ✅ Dùng được | |
| Giáo viên: tiến độ realtime (ma trận, online, nhắc nhở, xuất Excel) | ✅ Dùng được | |
| Paper theo nhóm: soạn bài loại Paper (giai đoạn, khung mục, rubric thang 10), chia nhóm, sinh viên tự lập / vào nhóm | 🟡 Đang làm | Mức 4: đã có soạn bài + nhóm (B23a); trình soạn viết paper cùng nhóm ở B23b |
| Giáo viên: chế độ thi + giám sát làm bài (xem trực tiếp bài đang viết, góp ý ngay, cảnh báo rời trang / dán, quá thời gian) | ✅ Dùng được | Mức 3, bước 15 |
| Giáo viên: thống kê một bài, thống kê lớp + xếp hạng, kết quả học tập từng sinh viên | ✅ Dùng được | |
| Giáo viên: tổng quan (dashboard) | ✅ Dùng được | |
| Giáo viên: ngân hàng đề | ✅ Dùng được | Danh sách, tìm/lọc, xem rubric, thêm từ rubric mẫu, sửa, xoá, giao trực tiếp và lưu/nạp trong trình soạn |
| Sinh viên: bài được giao, chi tiết bài, lịch sử nộp, làm bài (cả khi mất mạng), nộp bài, kết quả từng phần + so sánh các lần nộp | ✅ Dùng được | |
| Sinh viên: kết quả học tập (band theo thời gian, theo tiêu chí, hồ sơ lỗi) | ✅ Dùng được | |
| Trao đổi (chat) theo lớp / theo bài nộp — giáo viên và sinh viên | ✅ Dùng được | |

Những phần "đang làm" vẫn gọi thử được qua Swagger UI (mục 2.2) hoặc file `backend/http/*.http`.

---

## 2. Mở hệ thống

### 2.1. Chạy bằng Docker (khuyên dùng khi demo)

Ở thư mục gốc dự án, dùng Git Bash:

```
cp .env.example .env               # sửa DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD
                                   # muốn có dữ liệu mẫu: SPRING_PROFILES_ACTIVE=docker,demo
sh deploy/nginx/gen-cert.sh        # tạo chứng chỉ tự ký cho HTTPS (một lần)
docker compose up --build -d       # lần đầu mất vài phút
```

| Địa chỉ | Dùng để |
|---|---|
| https://localhost | Trang web. Trình duyệt báo chứng chỉ không tin cậy → chọn "Nâng cao" → "Tiếp tục" (chứng chỉ tự ký) |
| http://localhost:8025 | Mailpit — xem các email hệ thống gửi (tài khoản mới, bài tập mới, nhắc hạn…) |
| `localhost:9090` | Cổng TCP cho công cụ `admin-cli` (import sinh viên từ dòng lệnh) |

### 2.2. Chạy trên máy (khi phát triển)

| Thành phần | Lệnh | Địa chỉ |
|---|---|---|
| Backend có dữ liệu mẫu | `cd backend` → `mvn spring-boot:run -Dspring-boot.run.profiles=dev,demo` | http://localhost:8080 · Swagger UI: http://localhost:8080/swagger-ui.html |
| Frontend | `cd frontend` → `npm install` → `npm run dev -- -H 0.0.0.0` | **http://localhost:3000** hoặc `http://<IP-LAN-máy-chạy>:3000` |
| Xem email | `tools\mailpit\mailpit.exe` | http://localhost:8025 |

Khi mở từ máy khác cùng mạng, dùng IP LAN của máy chạy frontend (ví dụ `http://172.11.250.63:3000`), không dùng `localhost`.
Profile `dev` đã cho phép CORS từ các dải IP LAN phổ biến. Nếu đã đặt `NEXT_PUBLIC_API_BASE_URL` hoặc `NEXT_PUBLIC_WS_URL`,
hãy trỏ chúng tới IP LAN của máy chạy backend; cách đơn giản hơn là để trống để Next.js proxy API cùng origin.

---

## 3. Danh sách tài khoản

Dữ liệu mẫu chỉ có khi bật profile **`demo`** (`dev,demo` khi chạy trên máy, `docker,demo` khi chạy Docker). Backend tạo dữ liệu ở lần khởi động đầu, các lần sau thấy đã có thì bỏ qua.

### 3.1. Quản trị viên

| Tên đăng nhập | Mật khẩu | Ghi chú |
|---|---|---|
| `admin` | Chạy trên máy: `Admin@123`. Docker: giá trị `ADMIN_PASSWORD` trong file `.env` | **Bắt đổi mật khẩu ở lần đăng nhập đầu** (mật khẩu mới ≥ 8 ký tự, có chữ và số). Ghi lại mật khẩu mới — hệ thống không có chức năng "quên mật khẩu" cho admin |

### 3.2. Giáo viên

| Tên đăng nhập | Mật khẩu | Ghi chú |
|---|---|---|
| `gv.demo` | `Demo@2026` | Giáo viên mẫu, **không** bị bắt đổi mật khẩu. Chủ lớp `DEMO-A`, `DEMO-B` và các bài mẫu |

Giáo viên khác do admin tạo ở **Quản trị → Giáo viên**; mật khẩu tạm hiện **một lần** khi tạo.

### 3.3. Sinh viên

Mật khẩu chung: **`Demo@2026`** (không bị bắt đổi). Đăng nhập bằng **mã sinh viên**, không phân biệt hoa thường.

| Mã SV | Họ tên | Lớp | | Mã SV | Họ tên | Lớp |
|---|---|---|---|---|---|---|
| `DEMO001` | Trần Văn Bình | DEMO-A | | `DEMO011` | Trần Thu Trang | DEMO-B |
| `DEMO002` | Lê Văn Chi | DEMO-A | | `DEMO012` | Lê Quốc Tuấn | DEMO-B |
| `DEMO003` | Phạm Thị Dũng | DEMO-A | | `DEMO013` | Phạm Quốc Vy | DEMO-B |
| `DEMO004` | Hoàng Thị Giang | DEMO-A | | `DEMO014` | Hoàng Quốc An | DEMO-B |
| `DEMO005` | Vũ Thị Hà | DEMO-A | | `DEMO015` | Vũ Ngọc Bình | DEMO-B |
| `DEMO006` | Đặng Minh Khánh | DEMO-A | | `DEMO016` | Đặng Ngọc Chi | DEMO-B |
| `DEMO007` | Bùi Minh Linh | DEMO-A | | `DEMO017` | Bùi Ngọc Dũng | DEMO-B |
| `DEMO008` | Đỗ Minh Nam | DEMO-A | | `DEMO018` | Đỗ Đức Giang | DEMO-B |
| `DEMO009` | Ngô Thu Phương | DEMO-A | | `DEMO019` | Ngô Đức Hà | DEMO-B |
| `DEMO010` | Nguyễn Thu Quân | DEMO-A | | `DEMO020` | Nguyễn Đức Khánh | DEMO-B |

Email của sinh viên mẫu có dạng `demo001@ielts.local` (xem ở Mailpit).

Sinh viên mới do giáo viên **import** từ file: mật khẩu tạm nằm trong **file tài khoản** tải về sau khi import (chỉ tải được một lần, trong 15 phút) và trong email gửi cho sinh viên; lần đầu đăng nhập phải đổi mật khẩu.

### 3.4. Dữ liệu mẫu đi kèm

| Dữ liệu | Trạng thái lúc khởi tạo |
|---|---|
| Lớp `DEMO-A` (DEMO001 → 010), `DEMO-B` (DEMO011 → 020) | Giáo viên `gv.demo` |
| Bài **"Tuần 1"** — Task 1 + Task 2, rubric IELTS, hạn sau 7 ngày | **Đang mở**, giao cho cả 2 lớp. `DEMO001` → `DEMO006` đã nộp Task 1 (có sẵn bài để chấm), 3 người đang viết nháp Task 2 |
| Bài **"Tuần 2"** — Task 2 | **Nháp** — để phát hành ngay trong buổi demo |
| Lớp `DEMO-LOAD` (sinh viên `DEMO-T0001`…) | Chỉ có khi chạy kiểm thử tải (`DEMO_LOAD_STUDENTS` > 0, docs/09) |

> Profile `demo` chỉ dùng để demo / kiểm thử. **Không** bật cho hệ thống thật vì mọi tài khoản mẫu dùng chung một mật khẩu.

---

## 4. Dùng chung cho mọi vai trò

| Việc | Cách làm |
|---|---|
| Đăng nhập | Nhập tên đăng nhập (sinh viên: mã SV) và mật khẩu. Sai quá **5 lần trong 5 phút** → bị chặn tạm, thông báo ghi số phút phải chờ |
| Đổi mật khẩu lần đầu | Tài khoản mới (admin, giáo viên do admin tạo, sinh viên import) bị chuyển thẳng tới trang đổi mật khẩu. Các điều kiện tick xanh khi đạt: ≥ 8 ký tự, có chữ và số, khác mật khẩu hiện tại |
| Đổi mật khẩu sau này | Bấm tên mình ở góc phải trên → **Đổi mật khẩu**. Các thiết bị khác đang đăng nhập sẽ bị đăng xuất |
| Thông báo | Chuông ở góc phải trên: số chưa đọc, 6 thông báo mới nhất, "Đánh dấu đã đọc tất cả". Thông báo mới hiện ngay ở góc màn hình; bấm vào để mở trang liên quan. "Xem tất cả" → trang Thông báo (lọc Chưa đọc, nhóm theo ngày) |
| Trạng thái kết nối | Chấm **xanh "Trực tuyến"** cạnh chuông. Mất mạng / máy chủ khởi động lại → chấm vàng và thanh vàng "Mất kết nối… đang thử lại (lần n)"; khi có mạng lại, hệ thống tự kết nối và tải các thông báo bị lỡ |
| Đăng nhập ở máy khác | Mỗi tài khoản chỉ một phiên: đăng nhập ở máy mới thì máy cũ hiện hộp thoại **"Tài khoản vừa đăng nhập ở nơi khác"** và về trang đăng nhập |
| Thông tin cá nhân | Bấm tên mình → **Thông tin cá nhân**: họ tên, tên đăng nhập, mã SV, email, vai trò (chỉ xem — muốn sửa họ tên / email thì báo giáo viên hoặc quản trị viên), nút **Đổi mật khẩu** và **Đăng xuất** |
| Thu gọn menu | Nút ở đầu thanh tiêu đề (máy tính); trên điện thoại bấm ☰ để mở menu |
| Đăng xuất | Bấm tên mình → **Đăng xuất** |

---

## 5. Quản trị viên

| Trang | Làm được gì |
|---|---|
| **Tổng quan** | Số người dùng (theo vai trò, số bị khoá), số lớp, bài tập theo trạng thái, bài nộp hôm nay, số phiên đang hoạt động; biểu đồ bài nộp 14 ngày (rê chuột vào cột để xem số) |
| **Giáo viên** | **Thêm giáo viên**: tên đăng nhập (chữ không dấu, số, `. _ -`), họ tên, email → hệ thống hiện **mật khẩu tạm một lần** (bấm "Sao chép" rồi gửi cho giáo viên). Nút "…" ở mỗi dòng: sửa thông tin, đặt lại mật khẩu, khoá / mở khoá |
| **Người dùng** | Tra cứu mọi tài khoản theo tên / mã SV / email, lọc vai trò và trạng thái. Bấm một dòng để xem chi tiết (số phiên đang hoạt động, lần đăng nhập gần nhất). Khoá tài khoản → người đó bị đăng xuất ngay trên mọi thiết bị. Đặt lại mật khẩu → mật khẩu tạm mới, bắt đổi khi đăng nhập |
| **Nhật ký hệ thống** | Các thao tác quan trọng kèm IP: đăng nhập (đúng / sai / bị chặn), tạo giáo viên, khoá, đặt lại mật khẩu, import, phát hành, trả bài… Lọc theo hành động, tiền tố IP (vd `192.168.`), khoảng ngày; bấm ⊕ ở đầu dòng để xem chi tiết |
| **Phiên đăng nhập** | Số phiên đang hoạt động. Muốn đăng xuất một người khỏi mọi thiết bị: khoá hoặc đặt lại mật khẩu tài khoản đó |

Admin không tự khoá được chính mình.

---

## 6. Giáo viên

**Tổng quan** (trang chủ của giáo viên): lời chào; 4 thẻ — **bài chờ chấm** (bấm để mở hàng đợi; kèm số bài đang chấm dở), **bài tập đang mở** (số bài hết hạn trong 48 giờ), **tỉ lệ nộp đủ** của các bài đang mở, **lớp học**; danh sách **bài đang mở** (hạn gần nhất trước, đồng hồ đếm ngược, thanh nộp đủ, số bài chờ chấm — bấm tên bài để xem tiến độ); **hoạt động gần đây** (thông báo mới nhất, tự cập nhật khi sinh viên nộp bài); các **lớp học** với lối tắt sang Thống kê lớp. Nút **Tạo bài tập** ở góc phải.

### 6.1. Lớp học và sinh viên

1. **Lớp học → Tạo lớp**: mã lớp (2–30 ký tự, không phân biệt hoa thường, vd `IELTS-K15A`), tên, mô tả. Tạo xong vào thẳng trang lớp.
2. **Import sinh viên** (trong trang lớp):
   - Bấm **Tải file mẫu: Excel / CSV**. Cột bắt buộc `ma_sv` (4–30 chữ và số), `ho_ten`; tuỳ chọn `email`. Tối đa 2000 dòng, 10 MB.
   - Có sẵn 2 file để thử trong thư mục [`samples/`](../samples/README.md): `import-sinh-vien-hop-le.csv` (10 sinh viên mới) và `import-sinh-vien-co-loi.csv` (có 5 dòng lỗi cố ý để xem bảng lỗi).
   - Kéo thả file vào hộp, chọn có **gửi email thông tin đăng nhập** cho sinh viên mới không, bấm **Import**.
   - Kết quả: số **tạo mới / đã có tài khoản / đã trong lớp / lỗi**, bảng lỗi ghi số dòng (như Excel) và lý do. Dòng lỗi bị bỏ qua, các dòng khác vẫn được xử lý; sinh viên đã có tài khoản chỉ được thêm vào lớp.
   - Bấm **Tải file tài khoản** để lấy mật khẩu tạm của sinh viên mới. **Chỉ tải được một lần, trong 15 phút** — đóng hộp thoại khi chưa tải, hệ thống sẽ hỏi lại. Lỡ mất file thì nhờ admin đặt lại mật khẩu từng sinh viên.
3. **Thêm sinh viên** (đã có tài khoản): nhập mã SV, mỗi dòng một mã; mã không tồn tại được liệt kê lại.
4. **Xoá khỏi lớp**: biểu tượng thùng rác ở cuối dòng — tài khoản sinh viên vẫn giữ nguyên.
5. **Bảng điểm lớp**: mở chi tiết lớp, chọn tab **Bảng điểm** để xem toàn bộ sinh viên theo từng bài đã giao cho lớp. Có thể tìm theo tên / mã SV; ô trống là bài chưa được trả đủ các phần. Bấm tên bài ở đầu cột để mở thống kê bài đã lọc theo lớp.

Import bằng dòng lệnh (qua TCP, cho người quen terminal): `java -jar admin-cli/target/admin-cli.jar import --class DEMO-A --file ds.csv --user gv.demo` (xem `admin-cli/README.md`).

### 6.2. Soạn bài tập

**Bài tập → Tạo bài tập** mở trình soạn 4 bước. Nút **Lưu nháp** dùng được ở mọi bước; bài nháp chưa ai thấy.

| Bước | Nội dung |
|---|---|
| 1. Thông tin chung | Tên bài, mô tả / lời dặn, **thời điểm mở** (trống = mở ngay khi phát hành), **hạn nộp**, cho phép nộp muộn, số lần yêu cầu sửa tối đa, hiện / ẩn gợi ý sửa khi yêu cầu sửa |
| 2. Các phần | Mặc định Task 1 + Task 2; thêm Task 1 / Task 2 / phần tự do, đổi thứ tự (↑ ↓), xoá. Mỗi phần: tiêu đề, loại, **đề bài** (để một dòng trống giữa các đoạn), **ảnh đề** (biểu đồ Task 1 — PNG/JPG/WEBP ≤ 5 MB), số từ tối thiểu, thời gian gợi ý, trọng số, hạn riêng, **Chế độ thi** + thời gian thi (để trống = theo thời gian gợi ý) |
| 3. Tiêu chí & checklist | Bấm **Nạp rubric mẫu** để lấy 4 tiêu chí IELTS (TA/TR, CC, LR, GRA) kèm checklist. Mục **Cần có** = đặc điểm bài nên có; mục **Giới hạn điểm** = lỗi nặng, kèm **band tối đa** (vd "Không có overview → tối đa 5"). Sửa, thêm, xoá tự do |
| 4. Giao bài | Tóm tắt và danh sách việc còn thiếu (chưa đặt hạn, phần chưa có đề / tiêu chí…). Đủ điều kiện → **Lưu & giao bài** → chọn lớp và / hoặc từng sinh viên (gõ mã / tên để tìm) → **Phát hành** |

Ở bước **Các phần**, mỗi phần có hai thao tác ngân hàng đề:

- **Lưu vào ngân hàng**: phần phải có đề bài; nhập tên và các tag rồi lưu. Đề, ảnh, số từ, thời gian gợi ý và rubric/checklist hiện tại được sao chép vào ngân hàng.
- **Chọn từ ngân hàng**: tìm theo tên/nội dung, lọc loại Task hoặc bấm tag, rồi chọn **Dùng đề này**. Hệ thống thay đề + rubric của phần nhưng giữ nguyên **hạn riêng** và **trọng số** đang đặt cho bài hiện tại.
- Bản trong ngân hàng và phần đã nạp là hai bản độc lập; sửa hoặc xoá một bên không làm đổi bên còn lại.

Menu **Ngân hàng đề** mở trang quản lý các đề riêng của giáo viên:

- Tìm theo tên/nội dung; lọc **Task 1** (biểu đồ, bảng, quy trình, bản đồ) hoặc **Task 2** (essay); bấm tag để lọc nhanh và sắp xếp theo ngày cập nhật hoặc tên.
- Bấm tên đề để xem nội dung, ảnh, rubric và checklist. **Thêm đề** cho phép chọn rubric mẫu tương ứng; **Sửa** cập nhật bản trong ngân hàng; **Xoá** không làm thay đổi những bài tập đã sao chép đề đó trước đây.
- **Giao bài** ở mỗi dòng hoặc trong chi tiết đề: đặt tên bài, thời điểm mở, hạn nộp và tuỳ chọn sửa; hệ thống tạo một bài gồm một phần từ đề + rubric này, sau đó chọn lớp / từng sinh viên để phát hành. Nếu đóng ở bước chọn người nhận, bài đã tạo vẫn được giữ dưới dạng nháp trong menu **Bài tập**.

Khi phát hành, sinh viên nhận thông báo + email ngay (bài hẹn giờ: tới giờ mở mới gửi).

**Bài loại Paper (viết theo nhóm, Mức 4):** ở bước 1 chọn **Loại bài → Paper theo nhóm** (hệ thống hỏi lại rồi thay các phần bằng 2 giai đoạn mẫu *Đề cương*, *Bản cuối*). Chọn **kiểu trích dẫn** (IEEE cho ngành IT, hoặc APA), **số người tối đa mỗi nhóm** và **hạn chốt nhóm** (để trống = lúc hạn nộp). Ở bước 2 mỗi giai đoạn có **khung mục** (Abstract, Introduction, Method…): đặt tên mục, *Bắt buộc / Tuỳ chọn*, số từ tối thiểu / tối đa, đổi thứ tự, thêm / xoá. Bài Paper không có Task 1/2, thời gian gợi ý hay chế độ thi. Ở bước 3 bấm **Nạp rubric mẫu paper IT** để có 6 tiêu chí thang 10 (bước 0,25) kèm trọng số; sửa tự do.

### 6.3. Quản lý bài tập

- **Nhóm (bài Paper)**: sau khi phát hành, nút **Nhóm** ở chi tiết bài (hoặc tab **Nhóm** trong hàng Chi tiết · Nhóm · Tiến độ · …). Bên trái là người **chưa có nhóm**, bên phải các nhóm (số người / tối đa, nhãn *SV tự lập*). **Chia ngẫu nhiên** chia người chưa có nhóm (cả bài hoặc theo lớp đã chọn) thành các nhóm đều nhau; **Tạo nhóm** / **Sửa** (bút chì) đặt tên và chọn thành viên — chọn người đang ở nhóm khác thì người đó được chuyển sang; **Xoá** trả thành viên về chưa có nhóm (nhóm đã có bài làm thì không xoá được). Giáo viên đổi nhóm được cả sau hạn chốt. Tới hạn chốt mà còn người chưa có nhóm, bạn nhận thông báo **Chốt nhóm**.
- **Danh sách bài tập**: lọc Nháp / Đang mở / Đã đóng, hạn nộp kèm đồng hồ đếm ngược, số sinh viên đã nộp đủ, số bài chờ chấm.
- **Trang chi tiết bài**: đề và rubric từng phần, thời gian và tuỳ chọn, danh sách đã giao.
  - **Cài đặt** (mọi trạng thái): đổi tên, mô tả, hạn nộp, hạn riêng từng phần, nộp muộn, số lần sửa, gợi ý sửa.
  - **Giao thêm** (bài đang mở): thêm lớp / sinh viên; không gỡ được người đã giao.
  - Menu "…": **Sửa nội dung** (chỉ bài nháp), **Nhân bản** (tạo bản nháp mới, không chép người nhận và hạn), **Đóng bài** (sinh viên không nộp được nữa, vẫn chấm được) / **Mở lại**, **Xoá** (chỉ bài nháp).
- Bài **đã phát hành không sửa được đề và rubric** (để không làm sai lệch bài đã nộp) — muốn đổi thì nhân bản rồi soạn bài mới.

### 6.4. Chấm bài

1. **Hàng đợi** (menu **Chấm bài**, hoặc nút "Chấm bài" ở danh sách / chi tiết bài tập): lọc theo bài tập, phần, lớp, mã SV / họ tên; tab **Chờ chấm / Đang chấm / Đã trả / Cần sửa**. Bài nộp trước xếp trước; nhãn `v2` = bài nộp lại sau khi yêu cầu sửa, nhãn "Nộp muộn" nếu nộp sau hạn. Bấm **Chấm**.
2. **Màn chấm**: bài làm bên trái (cảnh báo khi bài ≤ 20 từ hoặc dưới số từ tối thiểu; bấm "Đề bài" để xem đề), bảng chấm bên phải. Màn hình nhỏ: chuyển giữa tab **Bài làm** / **Chấm điểm**.
   - Bấm **ⓘ** cạnh tên tiêu chí IELTS để xem bảng mô tả band thu gọn (diễn giải tiếng Việt, mức đang chọn được tô xanh) và link **bản chính thức (ielts.org)**.
   - Mỗi tiêu chí: tick checklist, chọn điểm (bấm lại để bỏ chọn, hoặc dùng phím ← →), ghi nhận xét. Tick mục **"Giới hạn điểm"** (nhãn cam "Tối đa N") thì các mức cao hơn bị khoá kèm lý do, điểm đang chọn cao hơn sẽ tự hạ xuống. Bài ≤ 20 từ: mọi tiêu chí tối đa 1.
   - **Band của phần** hiện ngay khi chấm đủ (trung bình các tiêu chí, làm tròn kiểu IELTS .25 / .75).
   - **Chấm 0** (góc trên bảng chấm): chọn lý do Không làm bài / Không viết bằng tiếng Anh / Học thuộc hoàn toàn; bấm "Bỏ chấm 0" để chấm lại bình thường.
   - Kết quả **tự lưu nháp** 2 giây sau mỗi thay đổi (hoặc bấm **Lưu nháp**); sinh viên chưa thấy gì cho tới khi trả bài.
   - **Trả bài**: cần chấm đủ mọi tiêu chí. Sinh viên nhận thông báo và thấy band, nhận xét.
   - **Yêu cầu sửa**: nhập lời nhắn và hạn sửa; chỉ khi còn lượt sửa (theo cài đặt bài tập) và bài chưa đóng. Sinh viên nộp lại sẽ thành lần nộp mới (`v2`).
   - **Đánh dấu trên bài** (giống Word đã chấm): **bôi đen** chữ trong bài → thanh công cụ **Lỗi · Đánh dấu · Tốt · Nhận xét · Sửa thành… · Xoá · Thêm chữ**.
     - *Lỗi*: chọn 1 trong 7 nhóm (Ngữ pháp, Chính tả, Từ vựng, Collocation, Liên kết, Dấu câu, Khác) hoặc mã IELTS chi tiết (VT, SV, ART…), tuỳ chọn tiêu chí, nhận xét, cách sửa → chữ được **gạch chân gợn sóng** màu theo nhóm lỗi.
     - *Đánh dấu* → nền vàng; *Tốt* → nền xanh nhạt; *Nhận xét* → nền xám, nội dung hiện ở **lề phải** (bong bóng canh theo dòng).
     - *Sửa thành…* → chữ cũ gạch ngang đỏ, chữ mới gạch chân đỏ ngay cạnh; *Xoá* → chữ gạch ngang đỏ; *Thêm chữ* → chữ đỏ gạch chân chèn sau đoạn đã chọn. **Bài gốc của sinh viên không bị đổi**, đây chỉ là lớp hiển thị.
     - Nhiều kiểu được chồng lên cùng một đoạn. Lề trái có **thanh dọc** ở các dòng có đánh dấu. Hàng **Hiện:** bật / tắt từng lớp (Tô màu, Sửa, Lỗi, Nhận xét).
     - Bấm vào đoạn đã đánh dấu (hoặc bong bóng ở lề) → thẻ chi tiết với **Sửa / Xoá**. Ô nhận xét có nút **Nhận xét mẫu** để chèn nhận xét hay dùng hoặc lưu nhận xét đang viết.
     - Tab **Đánh dấu (N)** ở bảng bên phải: **thống kê số lỗi** theo nhóm và theo tiêu chí, danh sách mọi đánh dấu — bấm một mục để cuộn tới đoạn đó trên bài. Đánh dấu tự lưu cùng kết quả chấm.
   - Trả bài / yêu cầu sửa xong, màn tự chuyển sang **bài kế tiếp** trong hàng đợi. Nút **Trước / Sau** và số "3/12" đi theo bộ lọc của hàng đợi; ô chọn **Lần nộp** để xem lại các lần nộp cũ (chỉ xem).
   - **So sánh với lần trước** (hiện khi bài đã nộp lại): nút ở trên bài làm. Chọn lần cũ cần so; xem kiểu **Gộp** (chữ bị bỏ gạch ngang nền đỏ, chữ thêm nền xanh) hoặc **Song song** (bài cũ | bài mới). Bên dưới là **"Đã sửa X/Y lỗi của lần N"** theo nhóm lỗi và theo tiêu chí, rồi từng lỗi của lần cũ với kết quả **tự đoán** (Đã sửa / Đổi khác gợi ý / Chưa sửa). Bấm một lỗi → đoạn trích được viền trên bài cũ; đoán sai thì chọn lại, bấm **Về tự đoán** để bỏ. Đánh giá lưu ngay, làm được cả sau khi đã trả bài.

### 6.5. Tiến độ theo thời gian thực

Mở từ nút **Tiến độ** ở chi tiết bài / danh sách bài tập (bài đã phát hành). Hàng nút **Chi tiết · Tiến độ · Giám sát · Thống kê** ở góc phải để chuyển giữa các trang của một bài.

- **Bốn thẻ đầu trang**: tỉ lệ hoàn thành (sinh viên đã nộp đủ mọi phần), số sinh viên nộp muộn, số sinh viên quá hạn mà chưa nộp, số người **đang trực tuyến** và số người đang mở bài này.
- **Ma trận**: mỗi hàng một sinh viên, mỗi cột một phần (tiêu đề cột ghi số đã nộp, vd "Task 1 · 16/20"). Ô tô màu theo trạng thái (Chưa làm, Đang làm, Đã nộp, Nộp muộn, Đang chấm, Đã trả bài, Cần sửa); đã trả thì ô ghi band; đồng hồ = nộp muộn, chấm than đỏ = quá hạn chưa nộp, `v2` = lần nộp thứ 2. Rê chuột vào ô để xem chi tiết; **bấm ô đã nộp để mở màn chấm**.
- **Realtime**: sinh viên lưu nháp lần đầu, nộp bài, hoặc bạn trả bài / yêu cầu sửa → ô đổi ngay và nháy viền. Chấm xanh ở avatar = sinh viên đang trực tuyến; dòng "đang mở Task 2" và viền xanh quanh ô = sinh viên đang mở màn làm phần đó. Nhãn **Cập nhật trực tiếp** ở góc phải chuyển sang "Đang kết nối lại…" khi mất kết nối; có lại kết nối thì ma trận tự tải lại.
- **Lọc**: theo lớp, theo trạng thái (hàng có ít nhất một ô ở trạng thái đó), tìm theo mã SV / họ tên.
- **Nhắc nhở**: tick chọn sinh viên rồi bấm **Nhắc N người đã chọn**; không chọn ai thì nhắc mọi người còn phần chưa nộp / cần sửa (chọn "Nhắc về phần" để chỉ nhắc một phần). Sinh viên nhận thông báo + email. Mỗi bài nhắc tối đa 3 lần trong 10 phút; bài đã đóng không nhắc được.
- **Xuất Excel**: tải bảng điểm `.xlsx` theo lớp đang lọc (điểm từng tiêu chí, band từng phần, trạng thái, band cả bài).

### 6.5b. Giám sát làm bài và chế độ thi

**Bật chế độ thi**: ở bước **Các phần** của trình soạn, bật **Chế độ thi** cho phần cần thi và nhập **thời gian thi** (để trống = lấy thời gian gợi ý). Trang chi tiết bài ghi "Chế độ thi · N phút". Sinh viên phải bấm **Bắt đầu làm bài** mới viết được; hết giờ **không tự nộp** — sinh viên vẫn nộp được nhưng bị đánh dấu **quá thời gian**, bạn và sinh viên nhận thông báo "Quá thời gian thi".

Mở trang **Giám sát** (hàng nút **Chi tiết · Tiến độ · Giám sát · Thống kê**):

- **Bốn thẻ**: số người đang trực tuyến, đang thi (kèm số đã nộp), quá thời gian, có cảnh báo (đã rời trang hoặc dán văn bản).
- **Bảng**: mỗi hàng một sinh viên, mỗi cột một phần (cột ở chế độ thi ghi "· thi"). Ô ghi trạng thái bài; phần thi có thêm trạng thái thi (Đang thi / Quá thời gian / Đã nộp / Nộp quá giờ), **đồng hồ còn lại** (quá giờ hiện "+mm:ss" màu đỏ) và số lần **rời trang / dán / mất kết nối** (rê chuột để xem; "rời trang" gồm cả chuyển sang cửa sổ / ứng dụng khác). Mặc định chỉ hiện người **đang làm bài**; chọn **Tất cả** để xem mọi người nhận bài. Lọc theo **lớp** và tìm theo mã SV / họ tên.
- **Sự kiện mới** (cột phải): mở trang là có sẵn 50 sự kiện gần nhất; rời trang, rời cửa sổ, dán văn bản (kèm số ký tự), mất / có lại kết nối, bắt đầu, nộp, hết giờ — hiện ngay khi xảy ra, dòng tô vàng là cảnh báo. Dòng ghi **gửi bù** = xảy ra lúc máy sinh viên mất mạng, giờ hiển thị là lúc xảy ra. Rời trang và dán còn tạo thông báo ở chuông (tối đa một lần / 2 phút cho mỗi sinh viên).
- **Xem trực tiếp**: bấm **Xem** ở ô còn đang làm (hoặc bấm một sự kiện) → khung bên phải hiện **bài sinh viên đang viết**, cập nhật mỗi giây khi sinh viên gõ, kèm số từ và giờ cập nhật. Bên dưới là khung **Góp ý trực tiếp**: tin bạn gửi hiện ngay dưới ô soạn của sinh viên, sinh viên trả lời được (cũng là kênh "Theo bài nộp" ở trang Trao đổi). Phần thi có thêm tab **Dòng thời gian thi** liệt kê mọi sự kiện của phiên. Sinh viên nộp xong thì khung báo đã nộp — xem bài ở màn chấm.

### 6.6. Thống kê một bài

Nút **Thống kê** ở chi tiết bài (hoặc **Chi tiết · Tiến độ · Thống kê**). Chỉ tính các phần **đã trả bài**; trung bình giữ 2 chữ số, chưa làm tròn kiểu IELTS. Ô chọn lớp ở góc phải để chỉ xem một lớp; **Xuất Excel** tải bảng điểm theo lớp đang chọn.

- **Thẻ đầu trang**: band cả bài trung bình (của sinh viên đã được trả đủ mọi phần), số sinh viên nhận bài, tỉ lệ nộp đúng hạn (lần nộp đầu), số phần đã trả trên số phần đã nộp.
- **Từng phần**: đã nộp, nộp muộn, % đúng hạn, đã trả, band trung bình.
- **Phân tích theo phần** (chọn Cả bài / từng phần): **phân bố band** (biểu đồ cột, rê chuột để xem số bài) và **điểm trung bình theo tiêu chí**.
- **Lỗi hay gặp**: 10 mã lỗi đánh dấu nhiều nhất, số lần và số sinh viên mắc — dùng để chọn nội dung chữa chung trên lớp.

### 6.7. Thống kê lớp và xếp hạng

Menu **Thống kê**. Chọn lớp ở góc phải (nhớ trên đường dẫn `?lop=`).

- **Thẻ đầu trang**: band trung bình của lớp, số thành viên, số bài đã giao cho lớp, tỉ lệ hoàn thành trung bình.
- **Các bài đã giao cho lớp**: trạng thái, hạn nộp, số thành viên nộp đủ mọi phần (thanh tiến độ), số người đã có đủ kết quả, band trung bình. Bấm tên bài → thống kê của bài đó, lọc sẵn theo lớp.
- **Bảng xếp hạng**: theo band cả bài trung bình; bằng band thì cùng hạng (1, 1, 3), chưa có band đứng cuối. Kèm số bài đủ kết quả, số phần đã nộp, số phần nộp muộn. **Bấm một sinh viên** → ngăn bên phải hiện **kết quả học tập** của sinh viên đó trên các bài của bạn (giống trang của sinh viên ở mục 7.5).
- Muốn xem nhanh **danh sách điểm của toàn bộ học sinh theo từng bài**, vào **Lớp học → chọn lớp → Bảng điểm**; bảng này dùng cùng dữ liệu thống kê và cập nhật sau khi giáo viên trả bài.

### 6.8. Trao đổi (chat)

Menu **Trao đổi**, hoặc nút **Trao đổi** trên màn chấm bài (mở thẳng kênh của bài nộp đó). Trong kênh bài nộp có nút **Mở bài nộp** để quay lại màn chấm.

- Cột trái: các cuộc trò chuyện chia **Theo bài nộp**, **Theo lớp** và **Trao đổi riêng**. Một ô tìm kiếm vừa lọc cuộc trò chuyện, vừa gợi ý học sinh theo tên / mã để giáo viên chọn và mở chat riêng. Kênh theo bài nộp cũng mở từ màn chấm bài; học sinh thấy kênh riêng khi có tin đầu tiên.
- Cột phải: tin nhắn theo ngày ("Hôm nay", "Hôm qua"…), tin của mình bên phải. **Enter** để gửi, **Shift + Enter** xuống dòng (tối đa 4000 ký tự). Tin hiện ngay với chữ "Đang gửi…"; mất kết nối hoặc quá 10 giây chưa gửi được thì ghi **Chưa gửi được · Gửi lại** (gửi lại không tạo tin trùng).
- Dòng dưới tiêu đề hiện **"… đang gõ"** khi người kia gõ; dưới tin cuối của mình có **Đã xem** (kênh lớp ghi tên người xem). Kéo lên đầu và bấm **Tải tin cũ hơn** để xem lịch sử.
- Mất kết nối rồi có lại: tin bị lỡ được tải bù tự động. Trên điện thoại, danh sách và khung chat hiện lần lượt (nút ← để quay lại danh sách).

---

## 7. Sinh viên

### 7.1. Bài được giao

Trang chủ của sinh viên. Bài chia theo 5 tab, số bên cạnh tab là số bài thuộc tab đó (một bài có thể nằm ở nhiều tab):

| Tab | Gồm những bài |
|---|---|
| Cần làm | Bài đang mở, còn phần chưa làm / đang làm và chưa quá hạn |
| Sắp hết hạn | Bài "Cần làm" có phần hết hạn trong 24 giờ tới |
| Quá hạn | Còn phần chưa nộp mà đã quá hạn, hoặc bài đã đóng |
| Đã nộp | Đã nộp mọi phần (đang chờ chấm hoặc đã có kết quả) |
| Cần sửa | Giáo viên yêu cầu sửa ít nhất một phần |

Mỗi thẻ bài ghi tên giáo viên, trạng thái từng phần (Chưa làm / Đang làm / Đã nộp / Nộp muộn / Đã trả bài / Cần sửa), hạn gần nhất kèm đồng hồ đếm ngược **theo giờ máy chủ** (giờ máy tính sai cũng không ảnh hưởng), và một nút chính: **Sửa bài** (có phần cần sửa) → **Làm bài** (còn phần chưa nộp) → **Xem kết quả** (đã có phần được trả) → **Xem chi tiết**. Quá hạn mà bài không cho nộp muộn thì không còn nút "Làm bài".

### 7.2. Chi tiết bài

Bấm tên bài (hoặc bấm vào thông báo) để mở. Trang có:
- khung đỏ **"Giáo viên yêu cầu sửa …"** kèm hạn sửa, lời nhắn và nút **Sửa bài** (nếu có);
- từng phần trong một tab: trạng thái, loại đề, số từ tối thiểu, thời gian gợi ý, hạn nộp / hạn sửa, đề bài + hình, **tiêu chí chấm** (mở ra xem checklist, mục "Giới hạn điểm" ghi band tối đa); nút **Làm bài / Viết tiếp / Sửa bài**, hoặc **Xem bài đã nộp**;
- **Lịch sử nộp** của từng phần: mỗi lần nộp, giờ nộp, số từ, nộp muộn, band nếu đã được trả bài (đang chấm thì ghi "Chờ kết quả");
- khung **Kết quả**: band từng phần và band cả bài (chỉ có khi mọi phần đã được trả; Task 2 tính gấp đôi Task 1);
- khung **Thời gian & quy định**: hạn nộp, có cho nộp muộn không, số lần yêu cầu sửa tối đa.
- **Bài Paper (theo nhóm)**: khung **Nhóm của tôi** ở cột phải. Chưa có nhóm thì gõ tên rồi bấm **Lập** để lập nhóm mới, hoặc bấm **Vào nhóm** ở một nhóm còn chỗ (nhóm đủ người ghi *Đủ người*). Có nhóm rồi thì thấy tên các thành viên và nút **Rời nhóm** (nhóm đã bắt đầu viết thì không tự rời được). Sau hạn chốt nhóm chỉ xem được; chưa có nhóm thì liên hệ giáo viên. Cả nhóm dùng chung một bài cho mỗi giai đoạn.

### 7.3. Làm bài và nộp bài

Bấm **Làm bài / Viết tiếp / Sửa bài** để mở màn làm bài toàn màn hình. Bài nhiều phần thì chọn phần ở hàng tab trên cùng (tab ghi trạng thái từng phần).

- **Bên trái** là đề bài (trên điện thoại: bấm **Xem đề bài** để mở), **bên phải** là ô soạn. Số từ đếm ngay khi gõ (cùng cách đếm với máy chủ): màu cam khi chưa đủ tối thiểu, xanh khi đủ.
- **Tự lưu nháp**: ngừng gõ 3 giây, chuyển sang tab khác hoặc đổi phần là bài được lưu; dòng trạng thái ghi "Chưa lưu" → "Đang lưu…" → "Đã lưu lúc hh:mm". Lưu lỗi thì bấm "Lỗi lưu – thử lại". Đóng trang khi còn thay đổi chưa lưu, trình duyệt sẽ hỏi lại.
- **Mất mạng khi đang viết**: cứ viết tiếp, bài được giữ trên trình duyệt; dòng trạng thái ghi "Ngoại tuyến – đã lưu trên máy" và nút nộp tạm khoá ("Cần có mạng để nộp bài"). Có mạng lại, bài tự gửi lên máy chủ. Lỡ đóng trang khi chưa gửi được, mở lại màn làm bài trên **cùng trình duyệt** thì bài được khôi phục (nếu trong lúc đó bài đã được sửa ở nơi khác, hệ thống hỏi chọn bản).
- **Mở cùng bài ở 2 tab / 2 máy**: khi một nơi lưu đè, nơi kia hiện hộp thoại cho xem cả hai bản (kèm số từ) để chọn **Dùng bản trên máy này** hoặc **Dùng bản trên máy chủ**.
- **Nộp**: bấm **Nộp phần này** (đang "Cần sửa": **Nộp bản sửa**). Bài ít hơn số từ tối thiểu thì hệ thống hỏi lại ("Viết tiếp" / "Vẫn nộp"). Nộp xong ô soạn chuyển sang chỉ đọc kèm giờ nộp; nộp sau hạn (khi bài cho phép) được ghi là **nộp muộn**.
- **Đồng hồ đếm ngược** theo giờ máy chủ (tự đồng bộ 5 phút một lần). Quá hạn mà bài không cho nộp muộn, hoặc bài đã đóng, thì ô soạn khoá lại và ghi lý do.
- **Giáo viên xem trực tiếp**: trên ô soạn có dòng "Giáo viên có thể xem trực tiếp bài bạn đang viết". Bên dưới ô soạn là khung **Trao đổi với giáo viên**: góp ý của giáo viên hiện ngay ở đây và bạn trả lời được (cùng cuộc trò chuyện với kênh bài nộp ở trang Trao đổi). Khung có sẵn sau lần tự lưu đầu tiên.
- **Chế độ thi** (phần có ghi "Chế độ thi"): thay cho ô soạn là thẻ ghi thời gian thi và luật thi; bấm **Bắt đầu làm bài** → xác nhận thì đồng hồ thi bắt đầu chạy và **không dừng** khi tải lại trang hay mở tab khác. Đồng hồ "Còn mm:ss" chuyển cam khi còn dưới 5 phút; hết giờ chuyển đỏ "Quá thời gian +mm:ss" — bạn **vẫn viết và nộp được**, nhưng bài bị đánh dấu quá thời gian và bạn nhận thông báo. Rời khỏi trang làm bài (chuyển tab, thu nhỏ), chuyển sang cửa sổ / ứng dụng khác, dán văn bản, mất kết nối đều được ghi lại và báo cho giáo viên — kể cả khi đang mất mạng (máy báo lại khi có mạng).

- **Xem bài đã chấm**: phần đã được trả bài hiện bài viết **kèm đánh dấu của giáo viên** (cùng kiểu với màn chấm: gạch lỗi, chữ sửa, nền tô màu, nhận xét ở lề), chỉ đọc; bấm đoạn được đánh dấu để xem chi tiết, bật / tắt từng lớp ở hàng **Hiện:**. Khi bài đang **Cần sửa**, chữ sửa của giáo viên được ẩn — chỉ thấy loại lỗi và nhận xét để tự sửa.

### 7.4. Kết quả từng phần

Mở từ nút **Xem kết quả** ở chi tiết bài / màn làm bài, hoặc bấm một lần nộp trong **Lịch sử nộp**. Mặc định hiện lần nộp gần nhất đã được trả; ô chọn ở góc phải để xem lần khác (lần chưa trả ghi "chờ kết quả").

- **Đầu trang**: band của lần nộp và mức (vd 6.0 · Khá), giờ trả bài. Đang **Cần sửa** thì có khung đỏ (hạn sửa, lời nhắn, nút **Sửa bài**) và ghi chú "Gợi ý sửa sẽ hiện sau khi bạn nộp bản sửa".
- **Bên trái**: bài viết kèm đánh dấu của giáo viên (như mục 7.3) và bảng **Lỗi trong bài** (mã lỗi, số lần).
- **Bên phải**: từng tiêu chí với điểm, nhận xét, checklist (● đạt, ○ chưa đạt, ● cam = điểm yếu làm giới hạn điểm) và lý do bị giới hạn điểm nếu có; cuối cùng là nhận xét chung.
- **So sánh các lần nộp** (khi đã nộp từ 2 lần): chọn lần cũ, xem **Gộp** hoặc **Song song** — chữ bỏ đi gạch ngang nền đỏ, chữ thêm nền xanh. Bên dưới là **"Đã sửa X/Y lỗi của lần N"** và từng lỗi của lần cũ: đánh giá của giáo viên, hoặc "(tự đoán)" khi giáo viên chưa đánh giá. Bấm một lỗi để thấy đoạn đó trên bài cũ.

### 7.5. Kết quả học tập

Menu **Kết quả học tập**. Chỉ tính những phần giáo viên **đã trả bài** (bài đang chấm chưa hiện).

- **Ba thẻ đầu trang**: band trung bình (trung bình band cả bài của các bài đã có kết quả đủ mọi phần) kèm mức (Yếu → Xuất sắc); số phần đã nộp và số phần đã có kết quả; số phần nộp muộn (lần nộp đầu sau hạn).
- **Band theo thời gian**: biểu đồ đường theo ngày trả bài — Band cả bài, Task 1, Task 2. Rê chuột vào một điểm để xem tên bài, band, ngày trả.
- **Trung bình theo tiêu chí**: TA, TR, CC, LR, GRA gộp mọi bài (thang 0 – 9, 2 chữ số thập phân).
- **Hồ sơ lỗi**: tối đa 10 mã lỗi giáo viên đánh dấu nhiều nhất trong các bài của bạn — nên ôn những lỗi đứng đầu.
- **Kết quả từng bài**: bài trả gần nhất lên đầu, số phần đã trả, band cả bài (hoặc "Chưa đủ phần"). Bấm **+** để xem band từng phần và **Xem kết quả** chi tiết của phần đó; bấm tên bài để mở chi tiết bài.

### 7.6. Trao đổi (chat)

Menu **Trao đổi**: trò chuyện với giáo viên trong kênh chung của lớp, kênh riêng về một bài nộp, hoặc kênh riêng với giáo viên khi giáo viên nhắn cho bạn.

- Cột trái: các cuộc trò chuyện chia **Theo bài nộp**, **Theo lớp** và **Trao đổi riêng**; một ô tìm theo tên cuộc trò chuyện, kèm tin cuối, thời gian và số tin chưa đọc. Kênh lớp có sẵn; kênh riêng hiện khi giáo viên gửi tin đầu tiên.
- Cột phải: tin nhắn theo ngày ("Hôm nay", "Hôm qua"…), tin của mình bên phải. **Enter** để gửi, **Shift + Enter** xuống dòng (tối đa 4000 ký tự). Tin hiện ngay với chữ "Đang gửi…"; mất kết nối hoặc quá 10 giây chưa gửi được thì ghi **Chưa gửi được · Gửi lại** (gửi lại không tạo tin trùng).
- Dòng dưới tiêu đề hiện **"… đang gõ"** khi người kia gõ; dưới tin cuối của mình có **Đã xem** (kênh lớp ghi tên người xem). Kéo lên đầu và bấm **Tải tin cũ hơn** để xem lịch sử.
- Mất kết nối rồi có lại: tin bị lỡ được tải bù tự động. Trên điện thoại, danh sách và khung chat hiện lần lượt (nút ← để quay lại danh sách).

---

## 8. Kịch bản demo gợi ý (~10 phút)

Mở hai trình duyệt (hoặc một cửa sổ ẩn danh): máy A đăng nhập `gv.demo`, máy B đăng nhập `DEMO001`.

1. **Admin** (`admin`): Tổng quan → Giáo viên → thêm một giáo viên, xem mật khẩu tạm hiện một lần → Nhật ký thấy dòng "Tạo giáo viên".
2. **Giáo viên** (máy A): Lớp học → mở `DEMO-A` → Import file mẫu đã điền vài sinh viên → xem kết quả, tải file tài khoản.
3. Bài tập → mở **"Tuần 2"** (nháp) → **Giao bài & phát hành** cho lớp `DEMO-A` → máy B (`DEMO001`) thấy **thông báo bật lên ngay** và số trên chuông tăng.
4. Tắt mạng máy B vài giây (hoặc khởi động lại backend) → thanh vàng "Mất kết nối… đang thử lại" → bật lại → tự kết nối.
5. Đăng nhập `DEMO001` ở trình duyệt thứ ba → máy B hiện "Tài khoản vừa đăng nhập ở nơi khác".

Muốn làm lại từ đầu: xoá dữ liệu (Docker: `docker compose down -v`; trên máy: xoá và tạo lại DB `ielts`) rồi khởi động lại với profile `demo`.

---

## 9. Sự cố thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Quên mật khẩu (giáo viên, sinh viên) | Nhờ admin **Đặt lại mật khẩu** ở trang Người dùng / Giáo viên |
| Quên mật khẩu `admin` (môi trường thử) | Xoá tài khoản `admin` trong DB rồi khởi động lại backend → hệ thống tạo lại `admin` với mật khẩu cấu hình (`Admin@123` / `ADMIN_PASSWORD`), bắt đổi ở lần đăng nhập đầu |
| "Bạn đã đăng nhập sai quá nhiều lần" | Chờ số phút ghi trong thông báo (tối đa 5 phút) |
| Trình duyệt báo chứng chỉ không an toàn (Docker) | Bình thường với chứng chỉ tự ký — chọn tiếp tục truy cập |
| Không thấy email | Mở Mailpit (http://localhost:8025); khi chạy trên máy phải bật `tools\mailpit\mailpit.exe` trước |
| Thanh vàng "Mất kết nối" không tắt | Kiểm tra backend còn chạy (`http://localhost:8080/actuator/health` khi chạy trên máy); trang tự kết nối lại khi backend sẵn sàng |
| "Không tải được file tài khoản" | File chỉ tải được một lần trong 15 phút sau import. Hết hạn → đặt lại mật khẩu cho từng sinh viên mới |
| Trang giáo viên trống nội dung ("đang làm") | Xem mục 1 — phần đó chưa có giao diện |
