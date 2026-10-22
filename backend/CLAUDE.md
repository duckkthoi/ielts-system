@AI-CODING-RULES.md

## Lệnh thường dùng

```bash
mvn spring-boot:run                 # chạy với profile dev (DB ielts trên localhost:5432)
mvn test                            # test, dùng DB ielts_test (profile test)
mvn -q compile                      # kiểm tra biên dịch nhanh
```

Swagger UI: http://localhost:8080/swagger-ui.html · Health: http://localhost:8080/actuator/health

Mail khi dev: chạy `..\tools\mailpit\mailpit.exe` (SMTP :1025, xem thư tại http://localhost:8025).
Tải về: release `mailpit-windows-amd64.zip` của https://github.com/axllent/mailpit (thư mục `tools/mailpit/` không commit).

Tài khoản dev: `admin` / `Admin@2026` (đã đổi từ mật khẩu mặc định `Admin@123` khi chạy thử B3b).
