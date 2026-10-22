# Prompt cho Figma Make — Hệ thống giao bài & chấm bài IELTS

## Hướng dẫn sử dụng

1. **Thứ tự**: dán **Prompt 0** trước (bối cảnh + design system), rồi **Prompt 1** (thư viện component), sau đó mới tới các prompt màn hình 2 → 10.
   - Nếu Figma Make giới hạn độ dài, ở mỗi prompt sau hãy dán lại đoạn **"Shared context"** của Prompt 0 lên đầu.
2. **Mỗi lần một prompt.** Làm xong thì đối chiếu với **checklist cuối file** rồi mới sang prompt tiếp theo. Chỗ nào chưa đúng thì nhắn sửa ngay trong cùng phiên, ví dụ: *"Make the status tags use exactly the colors in the Status table"*.
3. Prompt viết bằng **tiếng Anh** để AI làm chính xác hơn. **Mọi chữ hiển thị trên giao diện phải là tiếng Việt**, và đều đã nằm sẵn trong prompt (trong dấu ngoặc kép).
4. Nguồn gốc nội dung:
   - màu, font, khoảng cách: [docs/07-design-system.md](../docs/07-design-system.md);
   - luồng và quy tắc nghiệp vụ: [docs/02-use-case.md](../docs/02-use-case.md), [docs/06-trang-thai.md](../docs/06-trang-thai.md).
   Sửa design system thì sửa ở `docs/07` trước, rồi cập nhật Prompt 0.
5. Khi bàn giao cho code, xem mục **"Yêu cầu để code dễ"** ở cuối file.

---

## Prompt 0 — Shared context & design system

```text
You are designing a web app called "IELTS Writing" — a Vietnamese university system where teachers assign and grade IELTS Writing tasks online.

## Shared context
- Users: Admin (Quản trị viên), Teacher (Giáo viên), Student (Sinh viên).
- Core flow: teacher imports a student list file → accounts are auto-created by student ID → teacher creates an assignment with multiple parts (Task 1, Task 2), each part has rubric criteria + checklist → assigns to classes or individual students with deadlines → students write in the browser (auto-saved drafts) and submit each part separately → teacher grades with a checklist per criterion, inline comments, returns the work or requests a revision → realtime notifications, progress tracking, statistics.
- ALL UI TEXT IS VIETNAMESE (use the exact Vietnamese strings I give). IELTS terms stay in English (Task Response, Coherence and Cohesion, Lexical Resource, Grammatical Range and Accuracy, Band).
- The UI will be implemented with Ant Design 6 + Tailwind. Keep components close to Ant Design anatomy (Button, Input, Select, Table, Tabs, Drawer, Modal, Tag, Badge, Tooltip, Popover, Steps, Segmented) so they map 1:1 to code. Do not invent exotic controls.
- Feel: calm, academic, trustworthy, lots of white space, focused on reading and writing. Not playful, no gradients, no illustrations except simple empty states.

## Design tokens (create these as Figma variables with these exact names)
Colors
- brand-50 #EFF4FF, brand-100 #DBE6FE, brand-200 #BFD3FE, brand-500 #3B6FF6, brand-600 #2457E6 (PRIMARY), brand-700 #1C44C2 (primary hover), brand-900 #172C6E
- accent-50 #FFFBEB, accent-500 #F59E0B (highlights, high band)
- neutral-0 #FFFFFF (cards), neutral-25 #F8FAFC (page background), neutral-50 #F1F5F9 (table header, subtle fill), neutral-100 #E2E8F0 (default border), neutral-300 #CBD5E1 (input border), neutral-400 #94A3B8 (placeholder), neutral-500 #64748B (secondary text), neutral-700 #334155 (body text), neutral-900 #0F172A (headings)
- success #16A34A (bg #F0FDF4, border #BBF7D0), warning #D97706 (bg #FFFBEB, border #FDE68A), error #DC2626 (bg #FEF2F2, border #FECACA), info #0284C7 (bg #F0F9FF, border #BAE6FD)

Submission status colors (text / bg / border / solid) — use EVERYWHERE for status tags and progress cells:
- "Chưa làm" NOT_STARTED: #475569 / #F1F5F9 / #E2E8F0 / #CBD5E1
- "Đang làm" DRAFT: #0369A1 / #F0F9FF / #BAE6FD / #7DD3FC
- "Đã nộp" SUBMITTED: #1C44C2 / #EFF4FF / #BFD3FE / #3B6FF6
- "Nộp muộn" LATE: #C2410C / #FFF7ED / #FED7AA / #FB923C
- "Đang chấm" GRADED: #6D28D9 / #F5F3FF / #DDD6FE / #A78BFA
- "Đã trả bài" RETURNED: #15803D / #F0FDF4 / #BBF7D0 / #22C55E
- "Cần sửa" REVISION_REQUESTED: #B91C1C / #FEF2F2 / #FECACA / #EF4444
Assignment status: "Nháp" (#475569 / #F1F5F9), "Đang mở" (#1C44C2 / #EFF4FF), "Đã đóng" (#334155 / #E2E8F0).

IELTS band colors (text / bg / solid):
- 0–4.5 "Yếu": #B91C1C / #FEF2F2 / #EF4444
- 5.0–5.5 "Trung bình": #C2410C / #FFF7ED / #F97316
- 6.0–6.5 "Khá": #A16207 / #FEFCE8 / #EAB308
- 7.0–7.5 "Tốt": #15803D / #F0FDF4 / #22C55E
- 8.0–9.0 "Xuất sắc": #0F766E / #F0FDFA / #14B8A6
Band is always shown with one decimal: "6.0", "6.5".

Text highlights: inline comment = bg #FEF08A + underline #CA8A04 (active: bg #FDE047); diff added = bg #DCFCE7 text #166534; diff removed = bg #FEE2E2 text #991B1B strikethrough.
Presence dot: online #22C55E, away #F59E0B, offline #CBD5E1 (8px, 2px white ring).

Typography
- UI font: Roboto (supports Vietnamese). Essay/writing area: Noto Serif. Codes (student ID, class code, IP): JetBrains Mono.
- Scale (size/line-height/weight): display 30/38/700, h1 24/32/600, h2 20/28/600, h3 16/24/600, body 14/22/400 (default), body-strong 14/22/500, small 13/20/400, caption 12/18/400, essay 17/30 Noto Serif (mobile 16/28), prompt 15/24.
- Essay text column max width 720px.

Spacing: 4px grid (4, 8, 12, 16, 20, 24, 32, 40, 48). Page padding 24 desktop / 16 mobile. Card padding 24 / 16.
Radius: sm 4 (tags), md 8 (buttons, inputs — default), lg 12 (cards, modals, drawers), full (avatars, pills).
Shadows: sm 0 1 2 rgba(15,23,42,.06) cards; md 0 4 12 rgba(15,23,42,.08) dropdowns; lg 0 12 32 rgba(15,23,42,.12) modals/drawers.
Controls: height 36 (default), 32 (small), 40 (large). Focus ring: 2px brand-600 + 3px brand-100 glow. Icons: Lucide, 1.75 stroke, 16px in buttons/menus, 20px in header.

## Layout
- Frames: Desktop 1440×900, Tablet 1024×768, Mobile 390×844.
- App shell "with sidebar": white sidebar 240px (collapsed 72px), logo area 64px high; white header 64px with bottom border neutral-100 (left: breadcrumb; right: connection indicator, notification bell, account menu); content area on neutral-25 with 24px padding; every page starts with a Page header (h1 title, short description, action buttons on the right).
- App shell "fullscreen": header only (back button "Quay lại", title, actions). Used for writing, grading, chat.
- Mobile: sidebar becomes a drawer opened by a ☰ button; tables become stacked cards.

## Sample data (use consistently)
- Teacher: "ThS. Nguyễn Thị Hạnh". Admin: "Quản trị viên".
- Class: "IELTS K15 – Lớp A", code "IELTS-K15A", 32 students. Second class "IELTS K15 – Lớp B" ("IELTS-K15B").
- Students (ID — name): 2151000001 Nguyễn Văn An · 2151000002 Trần Thị Bình · 2151000003 Lê Hoàng Cường · 2151000004 Phạm Thu Dung · 2151000005 Võ Minh Đức · 2151000006 Đặng Khánh Linh
- Assignment: "Tuần 5 – Writing practice", deadline "23:59 07/11/2026", 2 parts:
  - "Task 1 – Line graph": "The graph below shows the number of international students enrolled in universities in three countries between 2000 and 2020. Summarise the information by selecting and reporting the main features, and make comparisons where relevant." min 150 words, 20 minutes.
  - "Task 2 – Opinion essay": "Some people believe that university education should be free for everyone. To what extent do you agree or disagree?" min 250 words, 40 minutes.
- Essay sample opening: "In recent years, the question of whether higher education should be provided free of charge has become a controversial issue. While some people argue that tuition fees are necessary, I strongly believe that university education should be free for everyone..."

Acknowledge and wait for the next prompt. Do not generate screens yet.
```

---

## Prompt 1 — Component library page

```text
Create a page "Components" that documents reusable components built from the tokens above. Use Auto Layout and component variants (properties) everywhere. Name components exactly as below.

1. Button — variants: type (primary, default, text, danger), size (small 32, middle 36, large 40), state (default, hover, focus, disabled, loading), with/without leading icon. Primary = brand-600 bg, white text, hover brand-700.
2. IconButton — square icon-only button with tooltip label below it as an example ("Chỉnh sửa").
3. Input, Select, DatePicker, Textarea — label above, placeholder, helper text, error state (error border + message "Vui lòng nhập tên lớp").
4. StatusTag — pill, 6px dot + label, one variant per submission status ("Chưa làm", "Đang làm", "Đã nộp", "Nộp muộn", "Đang chấm", "Đã trả bài", "Cần sửa") and per assignment status ("Nháp", "Đang mở", "Đã đóng"). Sizes sm/md.
5. BandBadge — circle with band number ("4.5", "5.5", "6.5", "7.0", "8.5") colored by band level; sizes sm 24, md 32, lg 56; empty variant shows "—".
6. ScorePicker — segmented row 0, 0.5 … 9.0 (19 options, compact) for one criterion; selected option brand-600 bg white text; a compact variant as a dropdown for mobile.
7. CriterionCard — card with criterion name (h3, e.g. "Task Response"), one-line description, checklist (checkbox + text; checked text turns neutral-500), ScorePicker, comment textarea "Nhận xét cho tiêu chí này…". Variants: collapsed, expanded, completed (4px left border brand-600 + check icon).
8. WordCounter — "248 / 250 từ" in warning color when below minimum, "274 / 250 từ" in success color when enough.
9. AutosaveIndicator — 4 variants: "Đã lưu lúc 14:32" (cloud-check, neutral-500), "Đang lưu…" (spinner), "Ngoại tuyến – đã lưu trên máy" (cloud-off, warning), "Lỗi lưu – thử lại" (error).
10. CountdownTimer — variants: "Còn 2 ngày" (neutral), "Còn 05:12:30" (neutral), "Còn 01:45:10" (warning), "Còn 00:12:05" (error), "Quá hạn 1 giờ" (error tag).
11. ConnectionBanner — full-width 32px strip: "Mất kết nối… đang thử lại (lần 3)" warning style; "Đã kết nối lại" success style.
12. NotificationBell — bell icon with red badge "3" and "99+"; plus the popover: title "Thông báo", link "Đánh dấu đã đọc tất cả", 5 items (icon, title, 1-line body, relative time "3 phút trước", unread dot), footer "Xem tất cả".
13. PresenceDot — online / away / offline, shown on an avatar and before a name.
14. ProgressCell — 64×40 cell filled with status solid color; shows band text if returned ("6.5"); small clock icon if late; hover tooltip "Nguyễn Văn An · Task 2 · Đã nộp lúc 23:41 06/11"; a "pulse" variant (brand-200 ring) for just-updated.
15. StatCard — label caption, big number (display), delta "+12% so với tuần trước".
16. PageHeader — breadcrumb, h1, description, right-side actions.
17. Table — header neutral-50, 48px rows, hover neutral-25, sticky action column with IconButtons, pagination "1–20 trên 125".
18. Empty state — 24px icon, sentence, primary button (e.g. "Chưa có lớp nào" + "Tạo lớp").
19. Drawer (right, 600px, title + subtitle, sticky footer with "Huỷ" and "Lưu") and Modal (confirm: title, text, "Huỷ" / danger "Xoá lớp").
20. ChatMessage bubbles — mine (right, brand-600 bg, white text), others (left, neutral-50), meta line with time and status ("Đã gửi", "Đã xem"), typing indicator "Cô Hạnh đang gõ…".
21. InlineComment — highlighted text span + popover with quoted text, comment textarea, buttons "Lưu" / "Xoá".
```

---

## Prompt 2 — Đăng nhập & khung ứng dụng

```text
Using the components page, design these frames.

A) Login — Desktop 1440 and Mobile 390.
- Desktop: two columns. Left 50%: brand-600 panel with app name "IELTS Writing", tagline "Giao bài và chấm bài IELTS Writing trực tuyến", 3 short benefit lines with icons ("Tự lưu bài khi đang viết", "Chấm theo tiêu chí và checklist", "Thông báo tức thì khi có kết quả"). Right 50%: centered card 400px: h1 "Đăng nhập", inputs "Tên đăng nhập / Mã sinh viên" and "Mật khẩu" (with show/hide eye), primary large button full width "Đăng nhập", caption "Quên mật khẩu? Liên hệ giáo viên hoặc quản trị viên."
- Error state variant: alert above the form "Tên đăng nhập hoặc mật khẩu không đúng".
- Rate-limited variant: alert "Bạn đã nhập sai quá nhiều lần. Thử lại sau 5 phút." and disabled button.
- Mobile: single column, brand header strip with app name, then the form.

B) First-login change password — card 440px: h1 "Đổi mật khẩu", info alert "Đây là lần đăng nhập đầu tiên, vui lòng đặt mật khẩu mới.", fields "Mật khẩu hiện tại", "Mật khẩu mới", "Nhập lại mật khẩu mới", live checklist under the new password ("Ít nhất 8 ký tự", "Có cả chữ và số", "Khác mật khẩu cũ") with green checks when satisfied, primary button "Lưu mật khẩu".

C) App shell (desktop) for each role — show the sidebar menus:
- Admin: "Tổng quan", "Giáo viên", "Người dùng", "Nhật ký hệ thống", "Phiên đăng nhập".
- Teacher: "Tổng quan", "Lớp học", "Bài tập", "Chấm bài" (badge "12"), "Ngân hàng đề", "Thống kê", "Trao đổi".
- Student: "Bài được giao" (badge "3"), "Kết quả học tập", "Trao đổi".
Header right side: small connection dot + text "Trực tuyến" (green), NotificationBell, avatar + name + role caption ("ThS. Nguyễn Thị Hạnh · Giáo viên"). Account dropdown: "Thông tin cá nhân", "Đổi mật khẩu", divider, "Đăng xuất" (danger).
- Variant with ConnectionBanner under the header (disconnected state) and header dot "Mất kết nối" (warning).
- Collapsed sidebar variant (72px, icons only with tooltips).
- Mobile shell: header with ☰, title, bell; sidebar as left drawer.

D) Notifications page "Thông báo": tabs "Tất cả" / "Chưa đọc (3)", list grouped by "Hôm nay", "Hôm qua", "Trước đó". Item types and copy:
- "Bài tập mới: Tuần 5 – Writing practice" — "Hạn nộp 23:59 07/11/2026"
- "Đã có kết quả: Tuần 4 – Task 2" — "Band 6.5"
- "Cần sửa bài: Tuần 4 – Task 1" — "Hạn sửa 23:59 10/11. Lời nhắn: Bổ sung đoạn tổng quan."
- "Còn 2 giờ đến hạn nộp" — "Tuần 5 – Task 2 chưa được nộp"
- (teacher) "Nguyễn Văn An vừa nộp Task 2" — "Tuần 5 – Writing practice"
- Toast example top-right for a realtime notification.

E) Session kicked modal: "Tài khoản vừa đăng nhập ở nơi khác" — "Bạn đã bị đăng xuất khỏi thiết bị này." — button "Đăng nhập lại".
```

---

## Prompt 3 — Giáo viên: lớp học & import sinh viên

```text
Teacher screens, desktop 1440 (with sidebar shell, menu "Lớp học" active) plus a mobile version of the class list.

A) Class list "Lớp học" — PageHeader with primary button "Tạo lớp". Search input "Tìm theo tên hoặc mã lớp". Card grid (3 columns): each card shows class name, code in mono font, "32 sinh viên", "3 bài tập đang mở", menu (…) with "Sửa", "Xoá". Include an Empty state variant "Chưa có lớp nào".
Drawer "Tạo lớp": fields "Mã lớp" (mono, helper "Dùng khi import qua admin-cli"), "Tên lớp", "Mô tả"; footer "Huỷ" / "Tạo lớp".

B) Class detail "IELTS K15 – Lớp A" — breadcrumb "Lớp học / IELTS K15 – Lớp A". Header actions: "Import sinh viên" (primary, upload icon), "Thêm sinh viên", "Tải file mẫu" (text button).
Tabs: "Thành viên (32)", "Bài tập (5)", "Thống kê".
Members table columns: PresenceDot, "Mã SV" (mono), "Họ tên", "Email", "Ngày tham gia", "Trạng thái tài khoản" (tag "Chưa đổi mật khẩu" in warning style or "Hoạt động"), actions (remove from class). Use the sample students.

C) Import flow (3 frames):
1. Modal "Import sinh viên" step 1: dashed drag-and-drop zone "Kéo thả file .xlsx hoặc .csv vào đây, hoặc bấm để chọn file", helper "Cột bắt buộc: ma_sv, ho_ten. Cột tuỳ chọn: email. Tối đa 10 MB.", link "Tải file mẫu", checkbox "Gửi thông tin đăng nhập qua email cho sinh viên mới", buttons "Huỷ" / "Import".
2. Processing state: file chip "danh-sach-k15a.xlsx · 45 dòng", progress bar "Đang xử lý 28/45 dòng…".
3. Result: 4 StatCards in a row — "Tạo mới 40" (success), "Đã có tài khoản 3" (info), "Đã trong lớp 1" (neutral), "Lỗi 1" (error). Error table: "Dòng", "Mã SV", "Lý do" (row 17, "21A", "Mã sinh viên không hợp lệ"). A highlighted warning box: "File tài khoản chứa mật khẩu tạm và chỉ tải được một lần. Hãy tải về và lưu cẩn thận." with large primary button "Tải file tài khoản (.xlsx)". Caption "Email đã được đưa vào hàng gửi cho 40 sinh viên." Button "Đóng". Also a confirm modal when closing without downloading: "Bạn chưa tải file tài khoản. Nếu đóng, sẽ phải đặt lại mật khẩu cho từng sinh viên." — "Ở lại" / danger "Vẫn đóng".
```

---

## Prompt 4 — Giáo viên: danh sách bài tập & trình tạo bài tập

```text
Teacher screens, desktop 1440, menu "Bài tập" active.

A) Assignment list "Bài tập" — PageHeader with primary "Tạo bài tập". Filters: status Segmented ("Tất cả", "Nháp", "Đang mở", "Đã đóng"), search. Table columns: "Tên bài tập" (title + caption "2 phần · Task 1, Task 2"), "Trạng thái" (StatusTag assignment), "Giao cho" ("IELTS-K15A, IELTS-K15B · 64 SV"), "Hạn nộp" ("23:59 07/11/2026" + small CountdownTimer), "Đã nộp" (mini progress bar "41/64"), "Chờ chấm" (badge "12"), actions: "Tiến độ", "Chấm bài", "…" (Sửa, Nhân bản, Đóng bài, Xoá).

B) Create assignment wizard (full page inside the shell) with AntD Steps on top: "1. Thông tin chung", "2. Các phần", "3. Tiêu chí & checklist", "4. Giao bài". Sticky bottom bar: "Lưu nháp" (default), "Quay lại", "Tiếp tục" (primary); on the last step "Phát hành" (primary). Show each step as its own frame:

Step 1: "Tên bài tập", "Mô tả" (textarea), "Thời điểm mở" (date-time, helper "Để trống = mở ngay khi phát hành"), "Hạn nộp", switch "Cho phép nộp muộn" (helper "Bài nộp sau hạn sẽ được đánh dấu Nộp muộn"), number "Số lần yêu cầu sửa tối đa" (default 2).

Step 2: list of part cards with drag handles; each card: "Tiêu đề phần", Select "Loại" ("IELTS Writing Task 1", "IELTS Writing Task 2", "Tự do"), rich-text "Đề bài" (simple toolbar: bold, italic, list), image upload "Ảnh biểu đồ" (show uploaded line graph thumbnail), "Số từ tối thiểu" (150), "Thời gian gợi ý (phút)" (20), optional "Hạn riêng cho phần này". Button "+ Thêm phần". Show 2 parts filled with the sample prompts.

Step 3: left vertical tabs = parts ("Task 1 – Line graph", "Task 2 – Opinion essay"); right = criteria editor. Top: button "Nạp rubric mẫu IELTS" with a dropdown ("IELTS Writing Task 1 (Academic)", "IELTS Writing Task 2"). Each criterion row: name, "Điểm tối đa" 9, "Bước" 0.5, "Trọng số" 1, collapsible checklist items (editable text, add/remove "+ Thêm mục"). Show the 4 Task 2 criteria: Task Response (items: "Trả lời đầy đủ tất cả các phần của đề", "Quan điểm rõ ràng và nhất quán xuyên suốt bài", "Ý chính được phát triển, giải thích và có ví dụ cụ thể", "Kết luận tóm tắt quan điểm, không thêm ý mới", "Đủ tối thiểu 250 từ"), Coherence and Cohesion, Lexical Resource, Grammatical Range and Accuracy.

Step 4: two blocks. "Giao cho lớp": checkbox list of classes with student counts. "Giao cho từng sinh viên": searchable multi-select "Tìm theo mã SV hoặc tên" showing chosen students as tags. Summary card on the right: "Tổng số sinh viên nhận bài: 33 (đã loại trùng)", deadline, parts count, and a review list; primary "Phát hành". Confirm modal: "Phát hành bài tập?" — "33 sinh viên sẽ nhận thông báo và email." — "Huỷ" / "Phát hành".
```

---

## Prompt 5 — Giáo viên: tiến độ realtime & thống kê bài

```text
Teacher screens, desktop 1440.

A) Progress "Tiến độ – Tuần 5 – Writing practice" — breadcrumb "Bài tập / Tuần 5 – Writing practice / Tiến độ". Top: 4 StatCards ("Hoàn thành 62%", "Đã nộp 41/64", "Nộp muộn 4", "Đang online 9" with green dot). Live indicator chip "● Cập nhật trực tiếp" (green).
Toolbar: class Select ("Tất cả lớp"), status filter, search, button "Nhắc nhở (23)" (bell icon) enabled when rows are selected, secondary "Xuất Excel".
Matrix table: sticky first columns (checkbox, PresenceDot, "Mã SV", "Họ tên"); then one column per part ("Task 1", "Task 2") with ProgressCell; last column "Band" (BandBadge of the overall). Header row shows per-part completion "28/32". Show a realistic mix of all statuses, including one cell in the "pulse" just-updated state and one student with presence "away". Legend bar at the bottom explaining each color with its Vietnamese label.
Remind modal: "Gửi nhắc nhở cho 23 sinh viên chưa nộp?" with optional message textarea, "Huỷ" / "Gửi nhắc nhở".

B) Assignment statistics "Thống kê" tab — per part: band distribution bar chart (bars colored by band level, x = 4.0…8.5), horizontal bars "Điểm trung bình theo tiêu chí" (4 IELTS criteria), donut "Đúng hạn 90% / Muộn 10%", table "Top 5 cao nhất" and "Cần hỗ trợ" (lowest 5). Overall card "Band trung bình 6.0".
```

---

## Prompt 6 — Giáo viên: hàng đợi chấm & màn hình chấm bài

```text
A) Grading queue "Chấm bài" (sidebar shell): filters (assignment, part, class, status Segmented "Chờ chấm (12)", "Đang chấm (3)", "Đã trả (40)", "Cần sửa (5)"), table: "Sinh viên", "Bài tập / Phần", "Version" ("v2" tag with tooltip "Bài nộp lại"), "Nộp lúc" (+ "Nộp muộn" tag when late), "Số từ", "Trạng thái", button "Chấm".

B) Grading screen — FULLSCREEN shell, desktop 1440×900. This is the most important screen; make it clean and efficient.
Header: "Quay lại", title "Nguyễn Văn An · 2151000001", subtitle "Tuần 5 – Writing practice · Task 2 – Opinion essay", version Select "Version 2 (mới nhất)", tag "Nộp muộn" if late, navigation "‹ Trước" "Sau ›" "(3/12 trong hàng đợi)".
Left 58% — essay panel: collapsible prompt box at top (prompt text, "274 từ · nộp lúc 23:41 06/11/2026"); essay in Noto Serif 17/30, max width 720, generous margins. Show 3 inline comment highlights; one active with its InlineComment popover ("people is" → "Sai hoà hợp chủ ngữ – động từ: people are"). When text is selected, a floating mini toolbar appears: "+ Nhận xét". Right margin shows small comment markers aligned with highlights.
Right 42% — grading panel (scrolls independently): 4 CriterionCards (Task Response expanded with checklist partly checked and score 6.0 selected; others collapsed showing their selected score), then "Band của phần" big BandBadge lg "6.5" with caption "Trung bình có trọng số, làm tròn theo IELTS", then textarea "Nhận xét chung", then a sticky bottom action bar: "Lưu nháp" (default) + caption "Đã lưu lúc 14:32", "Yêu cầu sửa" (default with pencil icon), "Trả bài" (primary). Optional button "✦ Gợi ý AI" (text button) at the top of the panel.
Also show: a side tab "Lịch sử" listing versions with their bands ("v1 · 5.5 · 01/11", "v2 · đang chấm"); a similarity warning alert variant "Bài này giống 78% với bài của Trần Thị Bình" (pink, Mức 3).

C) Request revision modal: "Yêu cầu sửa bài" — textarea "Lời nhắn cho sinh viên" ("Phát triển thêm ví dụ ở đoạn 2 và sửa lỗi ngữ pháp đã đánh dấu."), date-time "Hạn sửa", caption "Còn 1 lần yêu cầu sửa cho bài này", buttons "Huỷ" / "Gửi yêu cầu sửa". Disabled variant with tooltip "Đã hết lượt yêu cầu sửa".

D) Tablet 1024: the two panels become tabs "Bài làm" / "Chấm điểm".

E) AI suggestion drawer (right): "Gợi ý của AI (chỉ để tham khảo)", per criterion suggested band + short reason, list of detected errors with "Thêm thành nhận xét" buttons, note "Điểm cuối cùng do giáo viên quyết định".
```

---

## Prompt 7 — Sinh viên: bài được giao, làm bài, kết quả

```text
Student screens. Provide Desktop 1440 AND Mobile 390 for A, C and D.

A) "Bài được giao" (sidebar shell): tabs with count badges "Cần làm (2)", "Sắp hết hạn (1)", "Quá hạn (0)", "Đã nộp (4)", "Cần sửa (1)". Assignment cards: title, teacher name, parts row with StatusTags per part ("Task 1 · Đã nộp", "Task 2 · Đang làm"), deadline + CountdownTimer, primary button "Làm bài" / "Xem kết quả" / "Sửa bài". "Cần sửa" cards show the teacher's note excerpt and revision deadline in error style. Empty variant: "Không có bài nào cần làm. Tuyệt vời!".

B) Assignment detail: description, per-part rows (title, type, min words, status, band if returned, action), history link "Lịch sử nộp".

C) Writing screen — FULLSCREEN shell (the second most important screen).
Header: "Quay lại", title "Tuần 5 – Writing practice", CountdownTimer "Còn 05:12:30", AutosaveIndicator, primary large button "Nộp phần này" (send icon).
Below header: part tabs "Task 1 – Line graph · Đã nộp ✓" and "Task 2 – Opinion essay · Đang làm".
Desktop body: left 40% prompt panel (prompt text in prompt style, line graph image with zoom icon, info chips "Tối thiểu 250 từ", "Gợi ý 40 phút"); right 60% writing area: borderless white sheet, Noto Serif 17/30, max width 720, placeholder "Bắt đầu viết bài của bạn tại đây…", bottom status bar: WordCounter "198 / 250 từ" (warning), elapsed "Đã viết 24 phút", AutosaveIndicator.
States (separate frames): (1) normal "Đã lưu lúc 14:32"; (2) offline — ConnectionBanner + AutosaveIndicator "Ngoại tuyến – đã lưu trên máy", submit button disabled with tooltip "Cần có mạng để nộp bài"; (3) draft conflict modal "Bài này vừa được lưu ở một tab hoặc thiết bị khác" with two preview columns "Bản trên máy này (14:35)" and "Bản trên máy chủ (14:33)" and buttons "Dùng bản trên máy này" / "Dùng bản trên máy chủ"; (4) submit confirm modal "Nộp Task 2?" — "Bài có 198 từ, ít hơn yêu cầu 250 từ." — "Viết tiếp" / "Vẫn nộp"; (5) success state: green alert "Đã nộp lúc 23:58 07/11/2026" and the editor becomes read-only; (6) deadline passed + late not allowed: error alert "Đã quá hạn nộp. Liên hệ giáo viên nếu cần gia hạn." and read-only.
Mobile: prompt collapsed into an expandable card at the top ("Xem đề bài ▾"), editor full width (essay 16/28), sticky bottom bar with word counter and "Nộp".
Exam mode variant (Mức 3): big timer in header "Còn 38:12", banner "Chế độ thi: rời khỏi trang sẽ được ghi lại".

D) Result view: header with overall BandBadge lg "6.5" and level "Khá", returned date. Left: essay (read-only) with inline comment highlights; clicking one shows the teacher's note. Right: per-criterion scores with BandBadge, checked/unchecked checklist (unchecked items shown with an empty circle as "chưa đạt"), criterion comments, general comment card. Revision-requested variant: red alert at top "Giáo viên yêu cầu sửa bài · Hạn sửa 23:59 10/11/2026 · Lời nhắn: …" with primary "Sửa bài".

E) Version history & diff: list of versions with band; compare picker "So sánh v1 ↔ v2"; diff view with added (green) and removed (red strikethrough) text; toggle "Gộp / Song song".
```

---

## Prompt 8 — Dashboard, kết quả học tập, trao đổi

```text
A) Teacher dashboard "Tổng quan": 4 StatCards ("Bài chờ chấm 12", "Bài tập đang mở 3", "Sinh viên 64", "Band trung bình 6.1"), list "Cần chấm ngay" (oldest submissions first), list "Hạn nộp sắp tới" with completion bars, line chart "Band trung bình theo tuần" (8 weeks), recent activity feed (realtime: "Trần Thị Bình vừa nộp Task 1 · 2 phút trước").

B) Student "Kết quả học tập": current level card (BandBadge lg + "Mục tiêu 7.0" progress), line chart band over time per task type (Task 1, Task 2), radar or grouped bars of the 4 criteria averages, stats "Đã nộp 12 bài", "Nộp muộn 1", table of graded work.

C) Chat "Trao đổi" — FULLSCREEN shell, desktop: left 320px channel list with two groups "Theo bài nộp" (e.g. "Tuần 5 · Task 2 – Nguyễn Văn An", unread badge) and "Theo lớp" ("IELTS-K15A"); each item shows last message, time, PresenceDot. Right: channel header (name, members/presence, link "Mở bài nộp"), message list with date separators, ChatMessage bubbles incl. statuses "Đang gửi", "Đã gửi", "Đã xem", a failed message with "Gửi lại", typing indicator, composer "Nhập tin nhắn…" + send. Mobile: channel list and conversation as two screens.
Also a compact chat panel docked on the right side of the Grading screen ("Trao đổi với sinh viên").
```

---

## Prompt 9 — Quản trị viên

```text
Admin screens, desktop 1440 (sidebar shell, admin menu).

A) "Tổng quan": StatCards (users by role, classes, assignments, submissions today), bar chart "Bài nộp theo ngày" (14 days), small table "Đăng nhập gần đây".

B) "Giáo viên": table ("Tên đăng nhập" mono, "Họ tên", "Email", "Số lớp", "Trạng thái" tag "Hoạt động"/"Đã khoá", "Đăng nhập gần nhất", actions "Sửa", "Đặt lại mật khẩu", "Khoá"). Drawer "Thêm giáo viên". After creating/resetting: modal showing the temporary password once in mono font with a copy button and warning "Mật khẩu này chỉ hiển thị một lần".

C) "Người dùng": filters role/status/search; table; user detail drawer with tabs "Thông tin", "Lớp", "Phiên đăng nhập".

D) "Nhật ký hệ thống": filters (người thực hiện, hành động, khoảng thời gian, IP); table "Thời gian", "Người thực hiện", "Hành động" (tags like "Đăng nhập", "Đăng nhập thất bại" (error), "Import sinh viên", "Import qua TCP", "Trả bài", "Khoá tài khoản"), "Đối tượng", "IP" (mono); row expands to show JSON detail.

E) "Phiên đăng nhập": table "Người dùng", "Thiết bị / trình duyệt", "IP", "Bắt đầu", "Hoạt động gần nhất", action "Buộc đăng xuất" (danger) with confirm modal.
```

---

## Prompt 10 — Trạng thái chung & kiểm tra lại

```text
Create a page "States" with:
- Skeleton loading versions of: assignment list, progress matrix, grading screen.
- Error pages: 404 "Không tìm thấy trang" (button "Về trang chủ"), 403 "Bạn không có quyền truy cập trang này", 500 "Đã có lỗi xảy ra" with "Thử lại".
- Inline error example in a form, toast examples (success "Đã lưu", error "Không kết nối được tới máy chủ").
- Accessibility check: all text meets WCAG AA contrast, every icon-only button has a tooltip, focus rings visible on a keyboard-focus example of the login form.
Then review all screens for consistency: same status colors, same band colors, same spacing, all text in Vietnamese with correct diacritics, Auto Layout everywhere, components used instead of detached copies. List any inconsistencies you fixed.
```

---

## Prompt 6b — Bổ sung màn chấm theo nghiệp vụ IELTS (dán sau Prompt 6 và Prompt 1)

> Bổ sung sau khi nghiên cứu bảng mô tả band chính thức của IELTS ([docs/08-nghiep-vu-cham-ielts.md](../docs/08-nghiep-vu-cham-ielts.md)). Nếu đã làm Prompt 6 thì dán prompt này để cập nhật.

```text
Update the grading components and the Grading screen to follow official IELTS marking practice.

1. ScorePicker: default scale is WHOLE bands 0–9 (10 options). Add a "half-band" variant (0, 0.5 … 9) used only when the teacher enables it. Add a LIMITED state: options above the limit are dimmed with a small lock icon and a tooltip "Tối đa 5 vì: Không chia đoạn".
2. ChecklistItem gets two kinds:
   - "Đạt" (positive feature, plain checkbox).
   - "Giới hạn điểm" (negative feature) with a small warning-coloured pill on the right, e.g. "Tối đa 5". When ticked, the row background becomes warning-bg.
   Show an example for Coherence and Cohesion (Task 2) with: "Mỗi đoạn có câu chủ đề rõ ràng" (Đạt, checked), "Từ nối đa dạng, dùng chính xác" (Đạt), "Không chia đoạn / chia đoạn không đạt" (Giới hạn, Tối đa 5, ticked) → ScorePicker shows the limited state.
3. DescriptorPanel: an ⓘ icon next to each criterion name opens a popover listing bands 9 → 0, one short Vietnamese line each (e.g. Task Response 7: "Trả lời các phần chính; quan điểm rõ; ý được mở rộng nhưng có lúc khái quát quá"). The row matching the current score is highlighted brand-50. Footer link "Bản chính thức (ielts.org)".
4. ErrorCodePicker: when text is selected in the essay, show a floating bar with code buttons grouped by criterion — LR: SP, WW, WC, WF, COL, REP · GRA: VT, SV, ART, PREP, SING/PL, WO, P, FRAG, RO, ^ · CC: LINK, PARA · TA/TR: IRR, DEV · other: ?, GOOD (green). Plus fields "Nhận xét" and "Gợi ý sửa". Inline highlight shows the code as a small superscript tag (e.g. "people is ᵛᵀ"); a suggestion renders as strikethrough old text → bold new text.
5. CommentBankPopover: a "Nhận xét mẫu" button inside each criterion comment box and the general comment box; popover with search, list sorted by most used, click to insert. Button "+ Lưu nhận xét này vào ngân hàng".
6. ErrorSummary card in the grading panel: "Lỗi đã đánh dấu: GRA – VT ×4, ART ×3 · LR – SP ×2 · CC – LINK ×1".
7. Word-count warnings at the top of the essay panel: "Bài có 18 từ (≤ 20) – mọi tiêu chí tối đa Band 1" (error) and "236 / 250 từ – dưới số từ tối thiểu" (warning).
8. "Chấm 0" text button (danger) in the grading panel header → modal with required reason radio: "Không làm bài", "Không viết bằng tiếng Anh", "Bài học thuộc hoàn toàn".
9. Band display: part band shown rounded (e.g. "6.5"); assignment band card "Band bài tập 6.5" with caption "Task 2 tính gấp đôi Task 1 · ước lượng, IELTS không công bố công thức chính xác". If a part is not yet returned: "Chưa đủ phần".
10. Student result view: unchecked "Đạt" items shown with an empty circle; ticked "Giới hạn" items shown as an alert line "Coherence and Cohesion bị giới hạn ở 5 vì: không chia đoạn". While the work is in "Cần sửa" status, inline comments show only the error code and note, NOT the suggestion (students fix it themselves); add a small hint "Gợi ý sửa sẽ hiện sau khi bạn nộp bản sửa".
```

---

## Checklist kiểm tra sau mỗi prompt

- [ ] Chữ tiếng Việt có đủ dấu, không bị lỗi font (thử chữ "Đặng Khánh Linh", "Nhận xét", "Quá hạn").
- [ ] Màu trạng thái và màu band **đúng mã màu** trong Prompt 0, giống nhau ở mọi màn.
- [ ] Vùng bài làm dùng **Noto Serif**, độ rộng tối đa 720px.
- [ ] Nút chính màu `#2457E6`; mỗi khu vực chỉ có **một** nút primary.
- [ ] Dùng component đã tạo ở Prompt 1 (instance), không vẽ lại rời rạc.
- [ ] Auto Layout cho mọi khối. Khoảng cách theo bội số của 4.
- [ ] Có bản điện thoại cho: đăng nhập, khung app, danh sách bài của sinh viên, màn làm bài, màn kết quả, chat.
- [ ] Đủ các trạng thái: lưu / đang lưu / ngoại tuyến / xung đột / quá hạn; mất kết nối; danh sách trống; đang tải.

## Yêu cầu để code dễ (nhắc Figma Make khi cần)

- Đặt tên layer và component đúng tên trong design system (`StatusTag`, `BandBadge`, `CriterionCard`…). Code dùng đúng những tên này trong `frontend/components/`.
- Biến màu trong Figma đặt đúng tên token (`brand-600`, `neutral-25`…), khớp với class Tailwind `bg-brand-600`.
- Không dùng màu hay cỡ chữ lẻ ngoài bảng token. Gặp chỗ cần màu mới thì bổ sung vào `docs/07-design-system.md` trước.
- Khi xong, xuất link Figma (hoặc Dev Mode) để đối chiếu khi code từng trang.
