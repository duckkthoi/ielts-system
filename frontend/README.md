# Frontend — AcaWrite (Next.js)

Next.js 16 (App Router) · React 19 · Ant Design 6 · Tailwind CSS 4 · Zustand 5 · axios · dayjs · lucide-react · STOMP.

## Chạy

```bash
cp .env.example .env.local   # sửa địa chỉ backend nếu cần
npm install
npm run dev                  # http://localhost:3000
npm run build && npm run lint
```

## Cấu trúc thư mục

```
app/
├─ layout.tsx · providers.tsx · globals.css
├─ dang-nhap/            # page.tsx (metadata) + index.tsx (client) + type.ts
├─ doi-mat-khau/         # đổi mật khẩu (bắt buộc ở lần đăng nhập đầu)
├─ (admin)/              # layout CÓ sidebar — dùng cho cả 3 vai trò, menu đổi theo vai trò
│   ├─ const.tsx         # MENU_BY_ROLE
│   ├─ quan-tri/...      # Admin
│   ├─ giao-vien/...     # Giáo viên
│   ├─ sinh-vien/...     # Sinh viên
│   └─ thong-bao/ · ca-nhan/   # dùng chung
└─ (main)/               # layout toàn màn hình (chỉ header)
    ├─ sinh-vien/lam-bai/[id]
    ├─ giao-vien/cham-bai/[id]
    └─ chat/
components/              # AppDrawer, AppModal, IconButton, AppHeaderActions…
lib/
├─ axiosClient.ts        # gắn JWT, tự refresh khi 401, toast lỗi, loading toàn cục
├─ api/<domain>.ts       # mỗi file một nhóm nghiệp vụ, chỉ export hàm async
├─ constants/ · types/ · utils/ (date.ts, hooks.ts)
├─ storage.ts · tokens.ts
stores/                  # zustand: useAuthStore, useAppStore
```

Mỗi trang nghiệp vụ gồm: `page.tsx`, `const.tsx` (cột bảng, form, map trạng thái), `type.ts`,
và `<Entity>DetailDrawer.tsx` nếu có drawer chi tiết.

Các trang hiện là khung tạm (`PagePlaceholder`); giao diện sẽ làm theo bản thiết kế Figma.
Quy tắc code chi tiết: [AI-CODING-RULES.md](AI-CODING-RULES.md).
