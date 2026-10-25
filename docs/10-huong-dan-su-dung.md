# 10. Hướng dẫn sử dụng

> Dành cho người chạy thử, người demo và người chấm đồ án: mở hệ thống ở đâu, đăng nhập bằng tài khoản nào, từng vai trò làm được gì.
> Cập nhật: 24/10/2026, theo trạng thái giao diện F1 → F3 (xem mục 1).

---

## 1. Trạng thái giao diện

| Phần | Giao diện | Ghi chú |
|---|---|---|
| Đăng nhập, đổi mật khẩu, khung ứng dụng, thông báo realtime | ✅ Dùng được | |
| Quản trị viên: tổng quan, giáo viên, người dùng, nhật ký | ✅ Dùng được | Trang "Phiên đăng nhập" chỉ hiện số phiên (danh sách chi tiết thuộc Mức 3) |
| Giáo viên: lớp học, import sinh viên, bài tập (soạn, giao, phát hành) | ✅ Dùng được | |
| Giáo viên: tiến độ, chấm bài, thống kê, trao đổi | ⏳ Đang làm | Backend đã có đủ API; giao diện là khung trống |
| Sinh viên: bài được giao, chi tiết bài, lịch sử nộp, làm bài (cả khi mất mạng), nộp bài | ✅ Dùng được | |
| Sinh viên: xem kết quả chi tiết, kết quả học tập, trao đổi | ⏳ Đang làm | |

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
| Frontend | `cd frontend` → `npm install` → `npm run dev` | **http://localhost:3000** |
| Xem email | `tools\mailpit\mailpit.exe` | http://localhost:8025 |

Frontend cần file `frontend/.env.local` (chép từ `frontend/.env.example`) để biết địa chỉ backend.

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

Import bằng dòng lệnh (qua TCP, cho người quen terminal): `java -jar admin-cli/target/admin-cli.jar import --class DEMO-A --file ds.csv --user gv.demo` (xem `admin-cli/README.md`).

### 6.2. Soạn bài tập

**Bài tập → Tạo bài tập** mở trình soạn 4 bước. Nút **Lưu nháp** dùng được ở mọi bước; bài nháp chưa ai thấy.

| Bước | Nội dung |
|---|---|
| 1. Thông tin chung | Tên bài, mô tả / lời dặn, **thời điểm mở** (trống = mở ngay khi phát hành), **hạn nộp**, cho phép nộp muộn, số lần yêu cầu sửa tối đa, hiện / ẩn gợi ý sửa khi yêu cầu sửa |
| 2. Các phần | Mặc định Task 1 + Task 2; thêm Task 1 / Task 2 / phần tự do, đổi thứ tự (↑ ↓), xoá. Mỗi phần: tiêu đề, loại, **đề bài** (để một dòng trống giữa các đoạn), **ảnh đề** (biểu đồ Task 1 — PNG/JPG/WEBP ≤ 5 MB), số từ tối thiểu, thời gian gợi ý, trọng số, hạn riêng |
| 3. Tiêu chí & checklist | Bấm **Nạp rubric mẫu** để lấy 4 tiêu chí IELTS (TA/TR, CC, LR, GRA) kèm checklist. Mục **Cần có** = đặc điểm bài nên có; mục **Giới hạn điểm** = lỗi nặng, kèm **band tối đa** (vd "Không có overview → tối đa 5"). Sửa, thêm, xoá tự do |
| 4. Giao bài | Tóm tắt và danh sách việc còn thiếu (chưa đặt hạn, phần chưa có đề / tiêu chí…). Đủ điều kiện → **Lưu & giao bài** → chọn lớp và / hoặc từng sinh viên (gõ mã / tên để tìm) → **Phát hành** |

Khi phát hành, sinh viên nhận thông báo + email ngay (bài hẹn giờ: tới giờ mở mới gửi).

### 6.3. Quản lý bài tập

- **Danh sách bài tập**: lọc Nháp / Đang mở / Đã đóng, hạn nộp kèm đồng hồ đếm ngược, số sinh viên đã nộp đủ, số bài chờ chấm.
- **Trang chi tiết bài**: đề và rubric từng phần, thời gian và tuỳ chọn, danh sách đã giao.
  - **Cài đặt** (mọi trạng thái): đổi tên, mô tả, hạn nộp, hạn riêng từng phần, nộp muộn, số lần sửa, gợi ý sửa.
  - **Giao thêm** (bài đang mở): thêm lớp / sinh viên; không gỡ được người đã giao.
  - Menu "…": **Sửa nội dung** (chỉ bài nháp), **Nhân bản** (tạo bản nháp mới, không chép người nhận và hạn), **Đóng bài** (sinh viên không nộp được nữa, vẫn chấm được) / **Mở lại**, **Xoá** (chỉ bài nháp).
- Bài **đã phát hành không sửa được đề và rubric** (để không làm sai lệch bài đã nộp) — muốn đổi thì nhân bản rồi soạn bài mới.

### 6.4. Đang làm

Tiến độ realtime của lớp, chấm bài (checklist, band, nhận xét inline, trả bài / yêu cầu sửa), thống kê, xuất Excel và trao đổi (chat) đã có ở backend; giao diện sẽ có ở các bước tiếp theo.

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

### 7.3. Làm bài và nộp bài

Bấm **Làm bài / Viết tiếp / Sửa bài** để mở màn làm bài toàn màn hình. Bài nhiều phần thì chọn phần ở hàng tab trên cùng (tab ghi trạng thái từng phần).

- **Bên trái** là đề bài (trên điện thoại: bấm **Xem đề bài** để mở), **bên phải** là ô soạn. Số từ đếm ngay khi gõ (cùng cách đếm với máy chủ): màu cam khi chưa đủ tối thiểu, xanh khi đủ.
- **Tự lưu nháp**: ngừng gõ 3 giây, chuyển sang tab khác hoặc đổi phần là bài được lưu; dòng trạng thái ghi "Chưa lưu" → "Đang lưu…" → "Đã lưu lúc hh:mm". Lưu lỗi thì bấm "Lỗi lưu – thử lại". Đóng trang khi còn thay đổi chưa lưu, trình duyệt sẽ hỏi lại.
- **Mất mạng khi đang viết**: cứ viết tiếp, bài được giữ trên trình duyệt; dòng trạng thái ghi "Ngoại tuyến – đã lưu trên máy" và nút nộp tạm khoá ("Cần có mạng để nộp bài"). Có mạng lại, bài tự gửi lên máy chủ. Lỡ đóng trang khi chưa gửi được, mở lại màn làm bài trên **cùng trình duyệt** thì bài được khôi phục (nếu trong lúc đó bài đã được sửa ở nơi khác, hệ thống hỏi chọn bản).
- **Mở cùng bài ở 2 tab / 2 máy**: khi một nơi lưu đè, nơi kia hiện hộp thoại cho xem cả hai bản (kèm số từ) để chọn **Dùng bản trên máy này** hoặc **Dùng bản trên máy chủ**.
- **Nộp**: bấm **Nộp phần này** (đang "Cần sửa": **Nộp bản sửa**). Bài ít hơn số từ tối thiểu thì hệ thống hỏi lại ("Viết tiếp" / "Vẫn nộp"). Nộp xong ô soạn chuyển sang chỉ đọc kèm giờ nộp; nộp sau hạn (khi bài cho phép) được ghi là **nộp muộn**.
- **Đồng hồ đếm ngược** theo giờ máy chủ (tự đồng bộ 5 phút một lần). Quá hạn mà bài không cho nộp muộn, hoặc bài đã đóng, thì ô soạn khoá lại và ghi lý do.

### 7.4. Đang làm

**Xem kết quả chi tiết** (nhận xét trên bài, mã lỗi, so sánh các lần nộp), **kết quả học tập** và **trao đổi** sẽ có ở các bước tiếp theo.

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
