# admin-cli — import sinh viên qua TCP

Chương trình dòng lệnh (Java 21 thuần, không thư viện ngoài) gửi file danh sách sinh viên lên **server TCP** của hệ thống, theo giao thức trong [`docs/05-api.md` phần C](../docs/05-api.md). Kết quả giống hệt import qua giao diện web.

## Build

```
cd admin-cli
mvn package          # tạo target/admin-cli.jar, chạy kèm unit test
```

## Dùng

```
# Kiểm tra kết nối (đăng nhập qua HTTP để lấy token, rồi AUTH + PING qua TCP)
java -jar target/admin-cli.jar ping --user gv.hanh

# Import vào lớp K15-A, gửi email tài khoản cho sinh viên mới có email
java -jar target/admin-cli.jar import --class K15-A --file danh-sach.csv --user gv.hanh --email
```

| Tuỳ chọn | Mặc định | Ý nghĩa |
|---|---|---|
| `--user` / `--password` | — | Đăng nhập qua `POST /api/auth/login`. Bỏ `--password` thì nhập ẩn trên console |
| `--token` | — | Dùng access token có sẵn thay cho `--user` |
| `--host`, `--port` | `localhost`, `9090` | Server TCP |
| `--api` | `http://localhost:8080` | Địa chỉ HTTP để đăng nhập |
| `--class`, `--file` | — | Mã lớp, file `.csv` UTF-8 (dòng đầu là tiêu đề: `ma_sv,ho_ten[,email]`) |
| `--email` | tắt | Gửi email tài khoản cho sinh viên mới |
| `--out` | `tai-khoan-<mã lớp>.csv` | File lưu tài khoản mới + **mật khẩu tạm** |
| `--timeout` | `60` | Số giây chờ phản hồi |
| `--insecure` | tắt | Chấp nhận chứng chỉ HTTPS tự ký khi đăng nhập (chạy sau nginx trong Docker: `--api https://localhost --insecure`) |

Mã thoát: `0` thành công · `1` lỗi kết nối hoặc server trả `ERR` · `2` sai tham số.

Ví dụ một lần chạy:
```
Đã xác thực. Gửi 96 byte tới lớp demo-36d7…
[##############################] 3/3
Xong: tạo mới 2, đã có tài khoản 0, đã trong lớp 0, lỗi 1
  Dòng 4: Mã sinh viên không hợp lệ (4–30 ký tự, chỉ chữ và số)
Đã lưu 2 tài khoản mới vào …\tai-khoan-demo-36d7.csv
Lưu ý: file chứa mật khẩu tạm — giữ kín và xoá sau khi đã phát cho sinh viên.
```

> Kết nối TCP là **text không mã hoá** (mật khẩu tạm đi trên dây): chỉ dùng trong mạng LAN tin cậy. Bản có TLS là Mức 3.
> Không commit các file `tai-khoan-*.csv` (đã có trong `.gitignore`).
