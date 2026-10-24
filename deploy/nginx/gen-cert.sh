#!/usr/bin/env sh
# Tạo chứng chỉ tự ký cho nginx (chỉ dùng khi dev / demo, trình duyệt sẽ cảnh báo "không an toàn").
# Cách dùng (Git Bash / Linux / macOS):  sh deploy/nginx/gen-cert.sh [tên-máy-chủ]
# Kết quả: deploy/nginx/certs/server.crt, server.key — thư mục certs/ không commit.
set -eu

HOST="${1:-localhost}"
DIR="$(cd "$(dirname "$0")" && pwd)/certs"
mkdir -p "$DIR"
cd "$DIR"

# MSYS_NO_PATHCONV: Git Bash trên Windows không đổi "/CN=…" thành đường dẫn (nên tên file dùng đường dẫn tương đối).
MSYS_NO_PATHCONV=1 openssl req -x509 -nodes -newkey rsa:2048 -days 825 \
  -keyout server.key -out server.crt \
  -subj "/CN=$HOST/O=LTMTU IELTS (tu ky)" \
  -addext "subjectAltName=DNS:$HOST,DNS:localhost,IP:127.0.0.1"

echo "Đã tạo $DIR/server.crt và server.key cho $HOST (hạn 825 ngày)"
