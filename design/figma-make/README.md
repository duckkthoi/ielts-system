# Thiết kế giao diện — code xuất từ Figma Make

Bản xuất code (React + Vite + Tailwind) của thiết kế làm bằng Figma Make, **chỉ để tham khảo** khi dựng `frontend/`. Không chạy, không build, không import vào ứng dụng.

| File | Ứng với prompt (`design/figma-make-prompts.md`) |
|---|---|
| `src/components/DesignSystem.tsx` | Prompt 1 — màu, chữ, component cơ bản |
| `src/components/Prompt2Screens.tsx` | Prompt 2 — đăng nhập, khung app |
| `src/components/Prompt3Screens.tsx` | Prompt 3 — lớp học, import sinh viên |
| `src/components/Prompt4To10Screens.tsx` | Prompt 4 → 10 — bài tập, tiến độ, chấm bài, sinh viên làm bài, kết quả, chat, quản trị |

Cách dùng: trước mỗi bước F, đọc màn tương ứng rồi **viết lại** bằng Next.js + token Tailwind của dự án (không chép CSS, không chép dữ liệu mẫu). Màn nào chưa đẹp thì được thiết kế lại.

`FIGMA-MAKE-AGENTS.md` là hướng dẫn của Figma Make cho AI của nó, **không** phải quy tắc của dự án này (xem `CLAUDE.md` ở thư mục gốc).
