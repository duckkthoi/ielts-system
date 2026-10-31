# 07. Design system

> Bộ quy chuẩn giao diện chung cho **thiết kế Figma** và **code frontend**.
> - Code: token nằm trong `frontend/lib/theme/` và `frontend/app/globals.css`. Component dùng **Ant Design 6** làm nền, theme hoá bằng token ở đây; layout và khoảng cách dùng **Tailwind CSS 4**.
> - Figma: tạo *Variables* và *Styles* đúng theo tên token trong file này, để thiết kế và code dùng cùng một bộ tên.

---

## 1. Nguyên tắc thiết kế

| Nguyên tắc | Áp dụng cụ thể |
|---|---|
| **Tập trung vào bài viết** | Màn hình làm bài và chấm bài ưu tiên vùng văn bản: chữ lớn, nhiều khoảng trắng, ít màu, công cụ đặt ở lề |
| **Trạng thái nhìn là biết** | Mỗi trạng thái bài nộp và mỗi mức band có **một màu cố định** trên toàn hệ thống, kèm chữ (không dùng chỉ màu) |
| **Realtime nhưng không làm phiền** | Cập nhật realtime hiện bằng hiệu ứng nhẹ (ô đổi màu, chấm xanh online, toast góc phải) thay cho popup chặn màn hình |
| **Không mất dữ liệu** | Luôn hiện trạng thái lưu ("Đã lưu lúc…", "Ngoại tuyến – đã lưu trên máy"). Thao tác không hoàn tác được thì phải xác nhận |
| **Tiếng Việt trước** | Mọi nhãn tiếng Việt. Thuật ngữ IELTS giữ tiếng Anh (Task Response, Band…). Font phải hỗ trợ đủ dấu tiếng Việt |
| **Dùng được trên điện thoại** | Sinh viên làm bài được trên điện thoại. Giáo viên chấm bài tối ưu cho màn hình ≥ 1280px |
| **Tiếp cận (a11y)** | Tương phản chữ ≥ 4.5:1 (WCAG AA), có viền focus rõ, thao tác được bằng bàn phím, icon-only button luôn có tooltip và `aria-label` |

### 1.1 Tên và logo
- **Tên:** **AcaWrite** (viết tắt *Academic Writing*), dùng chung cho bài luận IELTS và paper học thuật. Viết liền, chữ `A` và `W` hoa. Tiêu đề tab trình duyệt: `<Tên trang> — AcaWrite`. Câu giới thiệu: "Luyện viết IELTS và paper học thuật trực tuyến".
- **Logo:** ngòi bút hướng lên (đồng thời là chữ "A") trên một dòng kẻ, nét trắng dày 2 / 24, bo đầu nét, đặt trong ô vuông nền `brand-600` bo góc. Trên nền brand thì ô trong suốt, viền trắng 25%.
- **Code:** component `BrandMark` (`md` 36px dùng ở sidebar / đầu thẻ đăng nhập, `lg` 52px dùng ở panel trang đăng nhập); favicon `app/icon.svg` cùng hình.
- Nhãn loại bài "Bài luận IELTS", "IELTS Writing Task 1 / 2" giữ nguyên vì nói về loại bài, không phải tên hệ thống.

---

## 2. Màu sắc

### 2.1 Màu thương hiệu
Chủ đạo **xanh dương học thuật**: đáng tin, dịu mắt khi đọc lâu. Điểm nhấn là **vàng hổ phách**, dùng cho band và thành tích.

| Token | Giá trị | Dùng cho |
|---|---|---|
| `brand-50` | `#EFF4FF` | Nền mục menu đang chọn, nền nhấn nhẹ |
| `brand-100` | `#DBE6FE` | Nền hover |
| `brand-200` | `#BFD3FE` | Viền nhấn |
| `brand-500` | `#3B6FF6` | Icon nhấn, biểu đồ |
| **`brand-600`** | **`#2457E6`** | **Màu chính (primary)**: nút chính, link, focus |
| `brand-700` | `#1C44C2` | Hover / active của nút chính |
| `brand-900` | `#172C6E` | Chữ tiêu đề trên nền brand nhạt |
| `accent-500` | `#F59E0B` | Điểm nhấn: band cao, huy hiệu, đánh dấu sao |
| `accent-50` | `#FFFBEB` | Nền điểm nhấn |

### 2.2 Màu trung tính
| Token | Giá trị | Dùng cho |
|---|---|---|
| `neutral-0` | `#FFFFFF` | Nền card, nền nội dung |
| `neutral-25` | `#F8FAFC` | **Nền trang** |
| `neutral-50` | `#F1F5F9` | Nền khối phụ, header bảng |
| `neutral-100` | `#E2E8F0` | **Viền mặc định**, đường kẻ |
| `neutral-300` | `#CBD5E1` | Viền ô nhập |
| `neutral-400` | `#94A3B8` | Placeholder, icon mờ |
| `neutral-500` | `#64748B` | Chữ phụ, chú thích |
| `neutral-700` | `#334155` | Chữ thường |
| `neutral-900` | `#0F172A` | **Chữ tiêu đề** |

### 2.3 Màu ngữ nghĩa
| Token | Màu chính | Nền | Viền | Dùng cho |
|---|---|---|---|---|
| `success` | `#16A34A` | `#F0FDF4` | `#BBF7D0` | Thành công, đã trả bài |
| `warning` | `#D97706` | `#FFFBEB` | `#FDE68A` | Cảnh báo, sắp hết hạn, mất kết nối |
| `error` | `#DC2626` | `#FEF2F2` | `#FECACA` | Lỗi, quá hạn, cần sửa |
| `info` | `#0284C7` | `#F0F9FF` | `#BAE6FD` | Thông tin, gợi ý |

### 2.4 Màu trạng thái bài nộp
Dùng **thống nhất** cho tag trạng thái, ô ma trận tiến độ, chấm màu trong danh sách. Mỗi trạng thái có bộ ba `text / bg / border` cho tag, và `solid` cho ô ma trận.

| Trạng thái | Nhãn hiển thị | text | bg | border | solid |
|---|---|---|---|---|---|
| `NOT_STARTED` | Chưa làm | `#475569` | `#F1F5F9` | `#E2E8F0` | `#CBD5E1` |
| `DRAFT` | Đang làm | `#0369A1` | `#F0F9FF` | `#BAE6FD` | `#7DD3FC` |
| `SUBMITTED` | Đã nộp | `#1C44C2` | `#EFF4FF` | `#BFD3FE` | `#3B6FF6` |
| `LATE` ¹ | Nộp muộn | `#C2410C` | `#FFF7ED` | `#FED7AA` | `#FB923C` |
| `GRADED` | Đang chấm | `#6D28D9` | `#F5F3FF` | `#DDD6FE` | `#A78BFA` |
| `RETURNED` | Đã trả bài | `#15803D` | `#F0FDF4` | `#BBF7D0` | `#22C55E` |
| `REVISION_REQUESTED` | Cần sửa | `#B91C1C` | `#FEF2F2` | `#FECACA` | `#EF4444` |

¹ `LATE` là **cách hiển thị**, không phải trạng thái trong DB: bài `SUBMITTED` có `is_late = true` thì hiện màu này.
Phía sinh viên, `GRADED` hiển thị như `SUBMITTED` ("Đã nộp – chờ kết quả"), vì điểm chưa được công bố.

### 2.5 Màu trạng thái bài tập
| Trạng thái | Nhãn | text | bg | border |
|---|---|---|---|---|
| `DRAFT` | Nháp | `#475569` | `#F1F5F9` | `#E2E8F0` |
| `PUBLISHED` | Đang mở | `#1C44C2` | `#EFF4FF` | `#BFD3FE` |
| `CLOSED` | Đã đóng | `#334155` | `#E2E8F0` | `#CBD5E1` |

### 2.6 Thang màu band IELTS
| Khoảng band | Mức | text | bg | solid (biểu đồ) |
|---|---|---|---|---|
| 0 – 4.5 | Yếu | `#B91C1C` | `#FEF2F2` | `#EF4444` |
| 5.0 – 5.5 | Trung bình | `#C2410C` | `#FFF7ED` | `#F97316` |
| 6.0 – 6.5 | Khá | `#A16207` | `#FEFCE8` | `#EAB308` |
| 7.0 – 7.5 | Tốt | `#15803D` | `#F0FDF4` | `#22C55E` |
| 8.0 – 9.0 | Xuất sắc | `#0F766E` | `#F0FDFA` | `#14B8A6` |

Biểu đồ phân bố band dùng màu `solid` theo đúng khoảng của từng cột.

### 2.7 Màu cho văn bản bài làm
| Token | Giá trị | Dùng cho |
|---|---|---|
| `highlight-comment` | nền `#FEF08A`, gạch chân `#CA8A04` | Đoạn có nhận xét inline |
| `highlight-comment-active` | nền `#FDE047` | Nhận xét inline đang được chọn |
| `highlight-similar` | nền `#FCE7F3` | Đoạn giống bài khác (đạo văn, Mức 3) |
| `diff-added` | nền `#DCFCE7`, chữ `#166534` | So sánh version: phần thêm |
| `diff-removed` | nền `#FEE2E2`, chữ `#991B1B`, gạch ngang | So sánh version: phần bỏ |

### 2.8 Màu realtime
| Token | Giá trị | Dùng cho |
|---|---|---|
| `presence-online` | `#22C55E` (viền trắng 2px) | Chấm xanh "đang online" |
| `presence-away` | `#F59E0B` | Online nhưng không mở tab làm bài |
| `presence-offline` | `#CBD5E1` | Ngoại tuyến |
| `pulse` | `brand-200` | Hiệu ứng nhấp nháy 1 lần khi ô ma trận vừa đổi trạng thái |

---

## 3. Chữ (Typography)

### 3.1 Font
| Vai trò | Font | Ghi chú |
|---|---|---|
| Giao diện (UI) | **Roboto** 400 / 500 / 600 / 700 | Google Fonts, subset `vietnamese` |
| **Vùng bài làm** (soạn và đọc bài viết) | **Noto Serif** 400 / 700 | Chữ có chân, đọc lâu không mỏi. Gần với cảm giác bài thi giấy |
| Mã (mã SV, mã lớp, IP, token) | **JetBrains Mono** 500 | |

### 3.2 Thang cỡ chữ
| Token | Cỡ / dòng | Độ đậm | Dùng cho |
|---|---|---|---|
| `display` | 30 / 38 | 700 | Số liệu lớn trên dashboard (band trung bình…) |
| `h1` | 24 / 32 | 600 | Tiêu đề trang |
| `h2` | 20 / 28 | 600 | Tiêu đề khối, tiêu đề drawer |
| `h3` | 16 / 24 | 600 | Tiêu đề card, tên tiêu chí |
| `body` | 14 / 22 | 400 | **Chữ mặc định** của UI (giống AntD) |
| `body-strong` | 14 / 22 | 500 | Nhãn, tên trong bảng |
| `small` | 13 / 20 | 400 | Nội dung bảng dày, mô tả phụ |
| `caption` | 12 / 18 | 400 | Chú thích, thời gian, "Đã lưu lúc…" |
| `essay` | **17 / 30** | 400 | Văn bản bài làm (Noto Serif). Trên điện thoại: 16 / 28 |
| `prompt` | 15 / 24 | 400 | Đề bài |

Độ rộng dòng của vùng bài làm tối đa **72 ký tự** (khoảng 720px), để dễ đọc.

---

## 4. Khoảng cách, bo góc, đổ bóng

### 4.1 Khoảng cách (lưới 4px)
| Token | px | Dùng điển hình |
|---|---|---|
| `space-1` | 4 | Khoảng giữa icon và chữ |
| `space-2` | 8 | Giữa các nút trong nhóm |
| `space-3` | 12 | Padding ô bảng, giữa các field |
| `space-4` | 16 | Padding card trên điện thoại, lề trang trên điện thoại |
| `space-5` | 20 | |
| `space-6` | 24 | **Padding card, lề trang trên desktop** |
| `space-8` | 32 | Giữa các khối lớn |
| `space-10` | 40 | |
| `space-12` | 48 | Khoảng trên dưới của trang trống (empty state) |

### 4.2 Bo góc
| Token | px | Dùng cho |
|---|---|---|
| `radius-sm` | 4 | Tag, checkbox |
| `radius-md` | 8 | **Nút, ô nhập, mặc định** |
| `radius-lg` | 12 | Card, drawer, modal |
| `radius-full` | 9999 | Avatar, chấm trạng thái, badge band |

### 4.3 Đổ bóng
| Token | Giá trị | Dùng cho |
|---|---|---|
| `shadow-sm` | `0 1px 2px rgba(15,23,42,.06)` | Card mặc định |
| `shadow-md` | `0 4px 12px rgba(15,23,42,.08)` | Card hover, dropdown |
| `shadow-lg` | `0 12px 32px rgba(15,23,42,.12)` | Modal, drawer, toast |

---

## 5. Bố cục

### 5.1 Breakpoint (theo Ant Design)
| Tên | Từ | Thiết bị | Thay đổi chính |
|---|---|---|---|
| `xs` | 0 | Điện thoại | Sidebar thành drawer (nút ☰). Bảng thành danh sách card. Lề 16px |
| `sm` | 576 | Điện thoại ngang | |
| `md` | 768 | Máy tính bảng | Màn hình chấm: 2 cột xếp dọc thành tab "Bài làm / Chấm điểm" |
| `lg` | 992 | Laptop nhỏ | Sidebar thu gọn còn icon (72px) |
| `xl` | 1200 | Laptop | Sidebar đầy đủ (240px) |
| `xxl` | 1600 | Màn lớn | Nội dung tối đa 1440px, căn giữa |

### 5.2 Khung trang
- **Layout có sidebar** (`(admin)` trong code):
  - Sidebar: nền trắng, rộng 240px (thu gọn còn 72px), logo cao 64px, menu theo vai trò.
  - Header: cao 64px, nền trắng, viền dưới `neutral-100`. Bên trái là breadcrumb; bên phải là chỉ báo kết nối, chuông thông báo, menu tài khoản.
  - Nội dung: nền `neutral-25`, lề 24px (16px trên điện thoại).
  - Đầu mỗi trang có **Page header**: tiêu đề `h1`, mô tả, các nút hành động ở bên phải.
- **Layout toàn màn hình** (`(main)`): chỉ có header 64px (nút Quay lại, tiêu đề bài, các hành động). Phần còn lại dành cho nội dung.

### 5.3 Bố cục các màn hình đặc thù
| Màn hình | Bố cục |
|---|---|
| **Làm bài** | Header: tên bài, đồng hồ đếm ngược, chỉ báo lưu, nút "Nộp phần này". Dưới header là tab các phần. Desktop chia 2 cột: **đề bài 40%** (cuộn riêng, ảnh biểu đồ phóng to được) và **vùng soạn 60%**. Thanh dưới vùng soạn: số từ / tối thiểu, thời gian gợi ý. Điện thoại: đề bài thu gọn ở trên, bấm để mở |
| **Chấm bài** | Header: tên sinh viên, phần, version, nhãn muộn, nút Trước/Sau trong hàng đợi. **Bài làm 58%** (font essay, bôi đen để nhận xét) và **bảng chấm 42%** (cuộn riêng): danh sách tiêu chí → band tổng → nhận xét chung → nút "Lưu nháp", "Yêu cầu sửa", "Trả bài" (dính đáy) |
| **Ma trận tiến độ** | Cột đầu cố định: mã SV, họ tên, chấm online. Mỗi cột là một phần. Ô màu `solid` theo trạng thái, có điểm nếu đã trả. Hàng tổng ở trên: phần trăm hoàn thành từng phần |
| **Dashboard** | Hàng thẻ số liệu (4 thẻ), rồi lưới 2 cột: danh sách việc cần làm + biểu đồ |

---

## 6. Icon
- Bộ icon: **lucide-react**, nét 1.75px.
- Kích thước: 16px trong nút và menu, 14px trong bảng, 20px trên header, 24px cho empty state.
- Icon có ý nghĩa cố định:

| Ý nghĩa | Icon |
|---|---|
| Bài tập | `FileText` |
| Lớp học | `Users` |
| Chấm bài | `ClipboardCheck` |
| Nộp bài | `Send` |
| Hạn nộp | `CalendarClock` |
| Đồng hồ | `Timer` |
| Đã lưu | `CloudCheck` |
| Đang lưu | `Loader2` (xoay) |
| Ngoại tuyến | `CloudOff` |
| Thông báo | `Bell` |
| Trao đổi | `MessageSquare` |
| Import | `Upload` |
| Xuất Excel | `FileSpreadsheet` |
| Lịch sử | `History` |
| Nhận xét | `MessageSquareQuote` |
| Cần sửa | `PencilLine` |
| Mất kết nối | `WifiOff` |
| Gợi ý AI | `Sparkles` |

---

## 7. Component

Mọi component dựa trên Ant Design, theme hoá bằng token. Cột **Nguồn** cho biết component lấy thẳng từ AntD hay tự viết trong `frontend/components/`.

### 7.1 Component nền

| Component | Nguồn | Quy cách |
|---|---|---|
| **Button** | AntD `Button` | Cao 36px (`middle`), 32px (`small`), 40px (`large`, dùng cho nút "Nộp"). Kiểu: `primary` (hành động chính, tối đa 1 nút mỗi khu vực), `default` (phụ), `text` (trong bảng), `danger` (xoá, khoá). **Không dùng thẻ `<button>` HTML** |
| **IconButton** | `components/IconButton` | Nút chỉ có icon, luôn bọc `Tooltip` và có `aria-label` |
| **Input / Select / DatePicker** | AntD | Cao 36px, `radius-md`, viền `neutral-300`, focus viền `brand-600` + vòng sáng `brand-100` 3px. Nhãn ở trên, lỗi hiện dưới ô màu `error` |
| **Table** | AntD `Table` | Header nền `neutral-50`, chữ `small` 600. Hàng cao 48px, hover `neutral-25`. Cột hành động dính phải. Phân trang ở dưới bên phải. Điện thoại: chuyển thành danh sách card |
| **Card** | AntD `Card` | Nền trắng, viền `neutral-100`, `radius-lg`, `shadow-sm`, padding 24 (16 trên điện thoại) |
| **AppDrawer** | `components/AppDrawer` | Mở từ phải, rộng 600px (720 cho form lớn, 100% trên điện thoại). Footer dính đáy chứa nút. Dùng cho **xem chi tiết và form phụ** |
| **AppModal** | `components/AppModal` | Chỉ dùng cho **xác nhận** và nội dung ngắn. Xác nhận hành động nguy hiểm thì nút `danger` ở bên phải |
| **Toast** | AntD `App.useApp().message` | Góc trên giữa, tự ẩn sau 3 giây. Lỗi giữ 5 giây |
| **Notification** | AntD `notification` | Góc trên phải, dùng cho **thông báo realtime** (có tiêu đề + nội dung + bấm để mở) |
| **Empty** | AntD `Empty` | Icon 24px + câu giải thích + nút hành động (vd "Chưa có lớp nào — Tạo lớp") |
| **Skeleton** | AntD `Skeleton` | Dùng khi tải trang lần đầu. **Không** dùng spinner toàn màn hình cho dữ liệu của trang |
| **Tabs** | AntD `Tabs` | Tab trạng thái của sinh viên có badge đếm số |

### 7.2 Component nghiệp vụ (tự viết)

| Component | Mô tả | Biến thể / trạng thái |
|---|---|---|
| **StatusTag** | Tag hình viên thuốc, có chấm màu bên trái và nhãn theo bảng 2.4 / 2.5 | `size`: sm / md. Có chấm hoặc không |
| **BandBadge** | Hiển thị band ("6.5") trong vòng tròn hoặc viên thuốc, màu theo bảng 2.6 | `size`: sm 24px / md 32px / lg 56px (màn kết quả). `null` → "—" |
| **ScorePicker** | Chọn điểm một tiêu chí: dải nút 0 → 9 bước 0.5 (segmented), phím ← → để đổi. Quá 21 mức (thang 10 bước nhỏ) → thanh trượt + ô nhập số theo đúng bước, dòng phụ ghi khoảng / bước / trần | Đang chọn: nền `brand-600` chữ trắng. Có tooltip mô tả band |
| **LevelsEditor** (Mức 4, trình soạn bài) | Khung "Mức đạt" dưới checklist của tiêu chí bài Paper: mỗi mức là thẻ nền `neutral-25` gồm tên, hai ô điểm "Từ – Đến", nút xoá và ô mô tả tự giãn; lỗi chung (chồng khoảng, thiếu mức…) hiện dưới danh sách. Nút "Thêm mức" (tối đa 6), "Dùng 4 mức mặc định" khi chưa có mức | Bài luận IELTS không có khung này |
| **CriterionCard** | Một tiêu chí trên màn chấm: tên + mô tả rút gọn, checklist (Checkbox), ScorePicker, ô nhận xét | Thu gọn / mở rộng. Viền trái `brand-600` khi đã chấm đủ |
| **ChecklistItem** | Checkbox + nội dung. Đã tick: chữ `neutral-500` | |
| **EssayEditor** | Vùng soạn bài: font `essay`, không viền, nền trắng, padding 32. Thanh trạng thái dưới: WordCounter + AutosaveIndicator | Chế độ chỉ đọc (đã nộp / quá hạn). Chế độ thi: chặn menu chuột phải (tuỳ chọn) |
| **EssayAnnotator** (thay EssayViewer, F5b) | Bài đã nộp kiểu "Word đã chấm": lỗi = gạch chân gợn sóng màu theo nhóm (`annot-grammar` / `-spelling` / `-vocabulary` / `-coherence` / `-punctuation` / `-other`); sửa = chữ cũ gạch ngang + chữ mới gạch chân màu `annot-edit`; lưu ý nền `highlight-comment`, dùng hay nền `annot-good`, có nhận xét nền `annot-comment` (đang chọn / hover: `annot-comment-active`). Lề trái: thanh dọc `brand-500` ở dòng có đánh dấu; lề phải 180px: bong bóng nhận xét canh theo dòng, tự dàn không chồng | Giáo viên: bôi đen → thanh công cụ 7 nút. Sinh viên: chỉ xem. Hàng "Hiện:" bật / tắt lớp Tô màu / Sửa / Lỗi / Nhận xét. Không dùng innerHTML — tách đoạn theo offset |
| **InlineCommentPopover** | Popover cạnh đoạn bôi đen: trích dẫn + ô nhận xét + Lưu / Xoá | |
| **WordCounter** | "248 / 250 từ". Chưa đủ tối thiểu: màu `warning`; đủ: `success` | |
| **PaperEditor** (Mức 4) | Trình soạn paper (TipTap): tờ giấy trắng rộng tối đa 960, chữ bài font `essay`. Tài liệu gồm các **mục cố định** do giáo viên đặt — tiêu đề mục `h2` (sans, gạch dưới `neutral-100`) không sửa / xoá / đổi thứ tự được. Thanh công cụ dính trên cùng: hoàn tác, đoạn / tiêu đề nhỏ / tiêu đề con, đậm / nghiêng / gạch chân, danh sách, trích khối, khối code (tô màu cú pháp theo token), bảng, hình (upload ≤ 5 MB), công thức LaTeX (KaTeX, có xem trước). Nút đang bật: `primary` ghost | Chỉ đọc: ẩn thanh công cụ (nhóm đã nộp / bài đóng). Thanh trạng thái dưới: số từ + PaperSyncIndicator |
| **PaperOutline** (Mức 4) | Mục lục của paper ở cột trái (trên đề bài; điện thoại: mục thu gọn "Mục lục & số từ"). Mỗi mục: icon + tên (dấu `*` màu `error` nếu bắt buộc) + dòng `caption` "120 từ / 150–600 từ". Màu theo `PAPER_WORD_HINT_META`: đạt `success`, còn thiếu / vượt `warning`, mục bắt buộc chưa viết `error`, mục không bắt buộc chưa viết `neutral-500`. Dưới cùng: tổng số từ cả bài | Bấm một mục → cuộn tới mục đó (chừa header + thanh công cụ). Chỉ nhắc, không chặn nộp (BR-29) |
| **Trích dẫn trong PaperEditor** (Mức 4) | Nút **Chèn trích dẫn** (icon sách) cuối thanh công cụ mở **CitationPicker** (Modal 680px: ô tìm, danh sách tài liệu đã định dạng có Checkbox — chọn nhiều, thứ tự theo lúc bấm; nút "Thêm tài liệu vào thư viện"; sửa thì có "Bỏ trích dẫn"). Trích dẫn trong bài: chữ `brand-700`, nền `brand-50` khi hover, viền `brand-500` khi chọn; nhãn `[1]`, `[1]–[3]` (IEEE) hoặc `(LeCun et al., 2015)` (APA) tự đánh lại khi đổi thứ tự / thư viện; tài liệu đã xoá → `[?]`. Cuối tờ giấy: **References** (h2) + chú thích `caption` "Tự sinh từ các trích dẫn…", mục font `serif` 15/26, APA thụt dòng treo 32px | Bấm vào trích dẫn → sửa (chỉ khi soạn được). Chưa trích dẫn → "Chưa có trích dẫn nào trong bài." |
| **ReferenceLibrary** (Mức 4) | Drawer 640px "Tài liệu tham khảo của nhóm" (phụ đề: kiểu trích dẫn + số tài liệu). Trên cùng: ô **Tra DOI** (Input.Search), nút **Nhập tay**, **Nhập BibTeX**. Mỗi mục là một thẻ viền `neutral-100`: dòng trích dẫn theo IEEE / APA (font `serif`, chữ thường — không HTML), dưới là Tag `warning` "Thiếu tác giả, năm…" (BR-30) + "Thêm bởi …"; bên phải nút sửa / xoá (Popconfirm). Form nhập tay / sửa và hộp nhập BibTeX (dán chữ hoặc chọn file `.bib`, xem trước danh sách đã định dạng) là Modal | Chỉ xem (giáo viên, tài liệu chỉ đọc): ẩn ô tra và nút sửa. Rỗng: Empty kèm hướng dẫn 3 cách thêm |
| **PaperSyncIndicator** | Như AutosaveIndicator cho tài liệu nhóm: "Đã lưu" · "Đang lưu…" · "Mất kết nối – sẽ gửi khi có mạng" (`warning`) · "Lỗi đồng bộ – mở lại" (`error`, bấm để mở lại) | Không có nút lưu — mọi thay đổi gửi sau ~0,3 giây |
| **AutosaveIndicator** | Icon + chữ `caption`: "Đã lưu lúc 14:32" · "Đang lưu…" · "Ngoại tuyến – đã lưu trên máy" (`warning`) · "Lỗi lưu – thử lại" (`error`) | 4 trạng thái theo sơ đồ 06 mục 7 |
| **CountdownTimer** | Thời gian còn lại tới hạn (theo giờ server). > 24h: "Còn 2 ngày"; < 24h: "Còn 05:12:30"; < 2h: màu `warning`; < 15 phút: màu `error` + nhấp nháy nhẹ mỗi phút; quá hạn: "Quá hạn 1 giờ" | Chế độ thi: to hơn, cố định trên header |
| **ConnectionBanner** | Thanh mỏng 32px dưới header: "Mất kết nối… đang thử lại (lần 3)" nền `warning` bg | Ẩn khi đã kết nối. Hiện "Đã kết nối lại" màu `success` 2 giây rồi ẩn |
| **NotificationBell** | Icon chuông + badge số chưa đọc (tối đa "99+"). Bấm mở popover 8 thông báo mới nhất + "Xem tất cả" | Có thông báo mới: chuông rung nhẹ 1 lần |
| **PresenceDot** | Chấm 8px màu theo bảng 2.8, đặt góc dưới phải avatar hoặc trước tên | online / away / offline |
| **ProgressCell** | Ô ma trận tiến độ: nền `solid` theo trạng thái, hiện band nếu đã trả, icon đồng hồ nếu muộn. Hover: tooltip chi tiết; bấm: mở bài để chấm | Hiệu ứng `pulse` khi vừa đổi |
| **StatCard** | Thẻ số liệu dashboard: nhãn `caption`, số `display`, so sánh tăng/giảm. Điện thoại (< `sm`): 2 thẻ một hàng, ẩn ô icon | |
| **PageHeader** | Tiêu đề `h1` + mô tả + breadcrumb + vùng nút bên phải | |
| **AssignmentHeader** | Đầu trang của một bài tập (giáo viên): breadcrumb *Bài tập › tên bài*, `h1` tên bài, mô tả của trang, vùng nút; dưới cùng là tab **Chi tiết · Nhóm (Paper) · Tiến độ · Giám sát · Thống kê** gạch chân `brand-600`. Chỉ trang Chi tiết có nút thao tác (Chấm bài này + menu **Thao tác**: Giao thêm, Cài đặt, Nhân bản, Đóng / Mở lại, Xoá) | Điện thoại: hàng tab cuộn ngang, không tràn trang. Bài nháp không có tab |
| **ImportResult** | Kết quả import: 4 số (tạo mới / đã có / bỏ qua / lỗi) + bảng lỗi + nút "Tải file tài khoản" (nổi bật, kèm cảnh báo "chỉ tải được một lần") | |
| **VersionDiff** | So sánh 2 version: chọn version A/B, hiển thị theo `diff-added` / `diff-removed`; đoạn trích của lỗi đang chọn viền `brand-500` | Hiển thị gộp / song song. Diff theo từ (LCS tự viết, `lib/utils/textDiff.ts`), chữ thường — không innerHTML |
| **DescriptorPanel** | Bảng mô tả band thu gọn (tiếng Việt) của một tiêu chí. Mở từ icon ⓘ cạnh tên tiêu chí; mỗi mức 9 → 0 là một dòng gọn, mức đang chọn được tô `brand-50`. Cuối bảng có link "Bản chính thức (ielts.org)" | Popover (desktop) / Drawer (điện thoại) |
| **ScorePicker – trạng thái giới hạn** | Khi có mục checklist **Giới hạn điểm** được tick: các mức lớn hơn band tối đa bị làm mờ và có biểu tượng khoá, tooltip *"Tối đa 5 vì: Không chia đoạn"* | |
| **ChecklistItem – loại Giới hạn** | Mục loại **Giới hạn điểm** có nhãn nhỏ màu `warning` "Tối đa 5" bên phải. Khi được tick, nền của mục chuyển sang `warning-bg` | |
| **ErrorCodePicker** | Thanh nổi khi bôi đen văn bản: các nút mã lỗi hay dùng (`SP`, `WW`, `VT`, `ART`…) nhóm theo tiêu chí, cùng ô "Nhận xét" và "Gợi ý sửa". Phím tắt: gõ mã rồi Enter | |
| **CommentBankPopover** | Danh sách nhận xét mẫu của giáo viên theo tiêu chí, có ô tìm kiếm, sắp theo số lần dùng. Bấm để chèn | |
| **ErrorSummary** | Bảng đếm lỗi theo mã, nhóm theo tiêu chí (vd "GRA: VT ×4, ART ×3") | Trong màn chấm, màn kết quả, hồ sơ lỗi |
| **ChatPanel** | Danh sách tin + ô nhập. Tin của mình bên phải nền `brand-600` chữ trắng; người khác bên trái nền `neutral-50`. "… đang gõ" dạng 3 chấm | Trạng thái gửi: đang gửi / đã gửi / đã xem / lỗi |

### 7.3 Trạng thái tương tác chung
| Trạng thái | Quy cách |
|---|---|
| Hover | Nền đậm hơn 1 bậc (vd `neutral-0` → `neutral-25`, `brand-600` → `brand-700`) |
| Focus (bàn phím) | Viền 2px `brand-600` + khoảng hở 2px. **Không bao giờ bỏ outline** |
| Disabled | Độ mờ 50%, con trỏ `not-allowed`. Có tooltip giải thích nếu lý do không hiển nhiên (vd "Đã hết lượt yêu cầu sửa") |
| Loading | Nút: spinner thay icon, giữ nguyên độ rộng. Trang: Skeleton |
| Lỗi | Viền `error`, thông báo ngay dưới ô |

---

## 8. Chuyển động
| Token | Giá trị | Dùng cho |
|---|---|---|
| `motion-fast` | 120ms ease-out | Hover, focus |
| `motion-base` | 200ms ease-out | Mở dropdown, popover, chuyển tab |
| `motion-slow` | 300ms ease-in-out | Drawer, modal |
| `pulse` | 600ms, 1 lần | Ô ma trận vừa cập nhật, chuông có thông báo mới |

Tôn trọng `prefers-reduced-motion`: tắt hiệu ứng pulse và rung chuông.

---

## 9. Giọng văn (UX writing)
- Xưng hô trung tính, không dùng "bạn/em" trong nhãn. Câu ngắn, động từ đứng đầu nút: "Nộp phần này", "Trả bài", "Tạo lớp".
- Thông báo lỗi nói **chuyện gì xảy ra + làm gì tiếp**: "Đã quá hạn nộp. Liên hệ giáo viên nếu cần gia hạn."
- Thời gian:
  - < 1 phút: "vừa xong";
  - < 24 giờ: "3 giờ trước";
  - còn lại: `DD/MM/YYYY HH:mm`.
  - Hạn nộp luôn ghi đủ ngày giờ.
- Số: band luôn 1 chữ số thập phân ("6.0", không ghi "6"). Phần trăm làm tròn số nguyên.

---

## 10. Ánh xạ sang code
| Thành phần | File |
|---|---|
| Biến CSS và theme Tailwind | `frontend/app/globals.css` |
| Giá trị token cho JavaScript (màu trạng thái, band, chart) | `frontend/lib/theme/tokens.ts` |
| Theme Ant Design (`ConfigProvider`) | `frontend/lib/theme/antdTheme.ts` |
| Map trạng thái → nhãn + màu | `frontend/lib/theme/status.ts` |
| Font | `frontend/app/layout.tsx` (Roboto, Noto Serif, JetBrains Mono) |

Quy tắc dùng trong code:
- Không viết mã màu hex trong component. Dùng class Tailwind theo token (`bg-brand-600`, `text-neutral-500`…), hoặc lấy từ `tokens.ts` khi cần giá trị JavaScript (biểu đồ, style động).
- Trạng thái và band luôn lấy màu qua `status.ts` (`SUBMISSION_STATUS_META`, `getBandLevel`), không tự chọn màu.
