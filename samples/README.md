# File mẫu import sinh viên

Dùng ở **Giáo viên → Lớp học → (mở một lớp) → Import sinh viên**, hoặc bằng `admin-cli`
(`java -jar admin-cli/target/admin-cli.jar import --class DEMO-A --file samples/import-sinh-vien-hop-le.csv --user gv.demo`).
File CSV UTF-8 có BOM (mở bằng Excel không lỗi dấu). Quy tắc cột: docs/05 mục A.4.

| File | Nội dung | Kết quả mong đợi khi import vào lớp mới |
|---|---|---|
| `import-sinh-vien-hop-le.csv` | 10 sinh viên mới `SV26001` → `SV26010` (2 người không có email) | Tạo mới 10 · Đã có 0 · Đã trong lớp 0 · Lỗi 0 |
| `import-sinh-vien-co-loi.csv` | Tiêu đề kiểu khác (`MSSV`, `Họ và tên`, `Email`) + 9 dòng có lỗi cố ý | Tạo mới 3 (`SV26011`, `SV26012`, `SV26017`) · Đã có tài khoản 1 (`DEMO001`) · Lỗi 5 |

Các dòng lỗi trong file thứ hai (số dòng tính như Excel, tiêu đề = dòng 1):

| Dòng | Mã SV | Lỗi |
|---|---|---|
| 5 | `21A` | Mã sinh viên dưới 4 ký tự |
| 6 | `SV-26014` | Mã chỉ được gồm chữ và số |
| 7 | `SV26015` | Thiếu họ tên |
| 8 | `SV26016` | Email sai định dạng |
| 9 | `SV26011` | Trùng mã với dòng 2 trong cùng file |

Import lần hai cùng một file vào cùng lớp: các sinh viên đã có chuyển sang "Đã trong lớp".
Mỗi lần import tạo **tài khoản thật** trong DB đang chạy — mật khẩu tạm nằm trong file tài khoản tải về (chỉ một lần, 15 phút).
