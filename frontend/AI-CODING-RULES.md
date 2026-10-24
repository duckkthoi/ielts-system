# Quy tắc code frontend (`frontend/`) — bắt buộc cho AI agent và người viết code

> Đọc [`../CLAUDE.md`](../CLAUDE.md) trước (quy trình làm từng bước, ràng buộc bảo mật). File này chỉ nói **viết code frontend thế nào**.
> **PHẢI** = bắt buộc · **KHÔNG** = cấm · *Nên* = mặc định, lệch thì ghi lý do vào PROGRESS.
> Giao diện chỉ làm khi **đã có thiết kế Figma** cho màn đó. Chưa có thì chỉ làm logic (API, store, hook) và để `PagePlaceholder`.

---

## 1. Nguồn sự thật

| Cần gì | Xem |
|---|---|
| Màn hình trông thế nào | Bản thiết kế Figma (người dùng gửi link) — làm đúng thiết kế, không tự sáng tác bố cục |
| Màu, chữ, khoảng cách, component nghiệp vụ, giọng văn | [`docs/07-design-system.md`](../docs/07-design-system.md) |
| Endpoint, dạng dữ liệu, mã lỗi, kênh WebSocket | [`docs/05-api.md`](../docs/05-api.md) |
| Trạng thái và nhãn (bài nộp, bài tập, kết nối, bản nháp) | [`docs/06-trang-thai.md`](../docs/06-trang-thai.md) |
| Nghiệp vụ chấm IELTS (checklist giới hạn, mã lỗi, band) | [`docs/08-nghiep-vu-cham-ielts.md`](../docs/08-nghiep-vu-cham-ielts.md) |

**Backend là nơi quyết định.** Frontend chỉ hiển thị và gửi yêu cầu. Mọi quy tắc (quyền, hạn nộp, giới hạn điểm, band chính thức) đã được server kiểm tra — frontend KHÔNG tự cài lại để "chặn", chỉ dùng để hiển thị trước / khoá nút cho dễ dùng.

## 2. Công nghệ

| Thành phần | Ghi chú |
|---|---|
| **Next.js 16** (App Router) | Khác nhiều so với bản cũ. **Trước khi dùng API của Next, đọc tài liệu trong `node_modules/next/dist/docs/`** và để ý cảnh báo deprecated (xem `AGENTS.md`). `cacheComponents` đang tắt |
| React 19, TypeScript `strict` | KHÔNG dùng `any`; thiếu kiểu thì khai báo trong `lib/types/` |
| Ant Design 6 | Component nền (Form, Table, Modal, Drawer…). Theme qua `lib/theme/antdTheme.ts` |
| Tailwind CSS 4 | Bố cục và màu theo token (`@theme` trong `app/globals.css`) |
| Zustand 5 | Chỉ cho state toàn app (mục 6) |
| axios · dayjs · lucide-react · `@stomp/stompjs` | Gọi API · ngày giờ · icon · WebSocket |

KHÔNG thêm thư viện khi chưa ghi lý do vào PROGRESS. KHÔNG dùng `@super/ui`, `@super/sdk` hay code giao diện chép từ cbs-fe (chỉ học **cấu trúc** thư mục).

---

## 3. Cấu trúc thư mục và trang

```
app/
├─ layout.tsx · providers.tsx · globals.css
├─ dang-nhap/ · doi-mat-khau/          trang không cần đăng nhập / bắt buộc đổi mật khẩu
├─ (admin)/                            layout CÓ sidebar, dùng cho cả 3 vai trò (menu: const.tsx → MENU_BY_ROLE)
│   ├─ quan-tri/… · giao-vien/… · sinh-vien/…
│   └─ thong-bao/ · ca-nhan/           dùng chung
└─ (main)/                             layout toàn màn hình: làm bài, chấm bài, chat
components/                            component dùng ở ≥ 2 trang (AppDrawer, AppModal, StatusTag, BandBadge…)
lib/
├─ axiosClient.ts                      MỘT axios instance duy nhất
├─ api/<nhóm>.ts                       hàm gọi API theo nhóm nghiệp vụ
├─ types/<nhóm>.ts                     kiểu dữ liệu khớp DTO ở docs/05
├─ constants/ · theme/ · utils/        hằng số · token + map trạng thái · hàm tiện ích (có test)
├─ storage.ts · tokens.ts              localStorage có kiểm soát · lưu / đọc token
stores/                                zustand: useAuthStore, useAppStore (+ store thông báo, kết nối khi làm F1)
```

- **Đường dẫn** là tiếng Việt không dấu, `kebab-case`, theo tiền tố vai trò: `/quan-tri/…`, `/giao-vien/…`, `/sinh-vien/…`. Tiền tố quyết định quyền xem trang (`ROLE_PATH_PREFIX`). Thêm trang dùng chung thì thêm vào `SHARED_PATHS`.
- **Mỗi trang nghiệp vụ** là một thư mục gồm:

  | File | Nội dung |
  |---|---|
  | `page.tsx` | Server component mỏng: `export const metadata = { title: '… — IELTS Writing' }` + render component client (bọc `<Suspense>` nếu dùng `useSearchParams`) |
  | `index.tsx` | `'use client'`, toàn bộ giao diện và tương tác |
  | `const.tsx` | Cột bảng, cấu hình form, map nhãn của riêng trang |
  | `type.ts` | Kiểu dữ liệu form / state của riêng trang |
  | `<Entity>DetailDrawer.tsx` | Drawer chi tiết (nếu có) |

  Mẫu: `app/dang-nhap/`.
- Component > 300 dòng thì tách. Component chỉ dùng ở một trang để trong thư mục trang đó; dùng ở ≥ 2 trang mới đưa vào `components/`.
- Tên component nghiệp vụ PHẢI trùng tên trong `docs/07` mục 7.2 (`ScorePicker`, `EssayEditor`, `ProgressCell`, `ErrorCodePicker`…).

---

## 4. Gọi API

- KHÔNG gọi `axios` / `fetch` trong component. Mọi request đi qua hàm trong `lib/api/<nhóm>.ts`, dùng `axiosClient`.
- Mỗi file API export các hàm `async` trả **`data` đã bóc** (không trả cả `ApiResponse`, trừ API phân trang cần `totalRecord`), rồi gom thành object `xxxApi` (mẫu: `lib/api/auth.ts`). CRUD chuẩn thì dùng `createBaseCrud` trong `lib/api/base.ts`; đường dẫn có tham số dùng `formatUrl('/api/…/{id}', { id })`.
- Kiểu dữ liệu trong `lib/types/` PHẢI khớp `docs/05` (tên trường `camelCase`, thời gian là `string` ISO, band / điểm là `number | null`). Backend đổi DTO thì sửa kiểu ở đây.
- Cờ của `axiosClient` (đặt trong config của request):

  | Cờ | Dùng khi |
  |---|---|
  | `controlLoading: true` | Trang tự hiển thị loading (vd tự lưu nháp, polling) — không bật spinner toàn cục |
  | `toastError: false` | Trang tự xử lý lỗi (vd hiện lỗi theo từng trường, xử lý `409` riêng) |
  | `isAuthCall: true` | Chỉ cho đăng nhập / refresh |
  | `rawResponse: true` | Tải file (blob), vd xuất Excel, file tài khoản import |

- **Lỗi:** mặc định `axiosClient` đã toast `errorMessage` tiếng Việt của server và reject bằng `ApiError` (`lib/apiError.ts`: `message`, `code` = `errorCode` của server, `status`, `fieldErrors`) → component chỉ `try { … } catch { /* đã toast */ }`. Lỗi validate có `fieldErrors` → gán vào form (`form.setFields`). Lỗi nghiệp vụ cần xử lý riêng thì xem `code` (vd `VERSION_CONFLICT` → tải lại trạng thái; `SCORE_LIMITED` → hiện lý do cạnh ô điểm).
- Server luôn trả HTTP 200; lỗi là body `success: false` với `status` (mã kiểu HTTP). `status` 401 được `axiosClient` tự refresh token **một lần cho mọi request đang chờ**; refresh thất bại → về trang đăng nhập. KHÔNG tự viết lại logic này ở chỗ khác.

---

## 5. Đăng nhập, phân quyền, điều hướng

- Layout cần đăng nhập dùng `useAuthGuard()` (`lib/utils/hooks.ts`): chưa đăng nhập → `/dang-nhap?returnUrl=…`; phải đổi mật khẩu → `/doi-mat-khau`; sai vai trò → trang chủ vai trò.
- Ẩn nút / menu theo vai trò chỉ để giao diện gọn — **quyền thật do backend kiểm tra**.
- `returnUrl` chỉ nhận đường dẫn nội bộ vai trò được vào (xem `resolveTarget` trong `app/dang-nhap/index.tsx`) — chống open redirect.
- Token chỉ đọc / ghi qua `lib/tokens.ts`. KHÔNG đặt token vào URL, KHÔNG log token.

## 6. State

| Loại | Đặt ở đâu |
|---|---|
| Phiên đăng nhập, người dùng hiện tại | `useAuthStore` |
| Loading toàn cục, instance `message` của antd | `useAppStore` |
| Thông báo (danh sách mới nhất, số chưa đọc), trạng thái kết nối WebSocket | `useNotificationStore`, `useRealtimeStore` (F1b) |
| Dữ liệu của một trang (danh sách, chi tiết, form) | state / hook trong trang — KHÔNG đưa vào store |

- Chọn từng phần store: `useAuthStore((s) => s.user)`, KHÔNG lấy cả store.
- `localStorage` chỉ qua `lib/storage.ts` (khoá trong `STORAGE_KEYS`). Bản nháp offline (Mức 2) dùng IndexedDB.

---

## 7. Giao diện

- **Màu:** KHÔNG viết mã hex trong component. Dùng class Tailwind theo token (`bg-brand-600`, `text-neutral-500`…), cần giá trị JS (biểu đồ, style động) thì lấy từ `lib/theme/tokens.ts`.
- **Trạng thái và band** luôn lấy nhãn + màu qua `lib/theme/status.ts`: `SUBMISSION_STATUS_META`, `getSubmissionDisplayStatus`, `ASSIGNMENT_STATUS_META`, `getBandLevel`, `formatBand`. KHÔNG tự chọn màu hay tự viết nhãn trạng thái.
- **Ngày giờ:** chỉ qua `lib/utils/date.ts` (`formatDate`, `formatDateTime`, `formatTime`, `formatRelative`) — đã xử lý giờ server (UTC) sang giờ Việt Nam.
- **Icon:** `lucide-react`, cỡ theo `docs/07` mục 6.
- **Chữ hiển thị** bằng tiếng Việt, theo giọng văn `docs/07` mục 9 (ngắn, rõ, nói cách khắc phục). Thông báo thành công lấy `message` server trả về nếu có.
- Mỗi màn có đủ trạng thái: đang tải (Skeleton), rỗng (Empty có lời hướng dẫn), lỗi, và responsive theo breakpoint `docs/07` mục 5.
- HTML từ server (đề bài `prompt`) đã được server lọc; chỉ hiển thị trường này bằng `dangerouslySetInnerHTML`, KHÔNG dùng cho dữ liệu nào khác.

---

## 8. Nghiệp vụ phía client — dùng đúng hàm có sẵn

| Việc | PHẢI dùng | Lý do |
|---|---|---|
| Đếm từ khi gõ | `lib/utils/wordCount.ts` | Cùng thuật toán với server, kiểm bằng bộ ca chung `shared/word-count-cases.json` (BR-20). Sửa thì sửa cả hai phía và chạy test hai phía |
| Band hiển thị | Giá trị `band` server trả | Server tính bằng `BandCalculator` (BR-12). Client chỉ được tính **xem trước** khi đang chấm, và phải dùng đúng quy tắc làm tròn .25 / .75 |
| Giới hạn điểm khi chấm | `maxAllowed` / `limitReason` trong `GradeDto` | Server tính theo checklist LIMIT và số từ (BR-17, BR-18); ô điểm khoá các mức lớn hơn và hiện lý do |
| Hạn nộp, đếm ngược | `effectiveDueAt`, `serverTime` (Mức 2: `/api/time`) | KHÔNG dùng giờ máy người dùng để quyết định quá hạn; server là nơi quyết định |
| Nộp bài | Gửi `expectedVersion` = `currentVersion` vừa đọc | Chống nộp trùng; `409 VERSION_CONFLICT` → tải lại trạng thái |
| Nhận xét inline | Offset theo `String` của JavaScript (UTF-16) trên **đúng nội dung version** | Gửi kèm `quote`; server từ chối nếu lệch |
| Gợi ý sửa khi "Cần sửa" | Hiển thị theo `suggestionsHidden` | Server đã ẩn (BR-21); không tự đoán |

## 9. Realtime (WebSocket STOMP) — đã cài ở F1b

Nghe kênh bằng `subscribeRealtime(destination, handler)` (trả hàm huỷ, gọi trong `useEffect`), gửi bằng `publishRealtime` — cả hai trong `lib/realtime.ts`. KHÔNG tạo `Client` STOMP mới; `RealtimeBridge` (trong `providers.tsx`) lo kết nối / đóng theo phiên đăng nhập.

- **Một** STOMP client cho cả app (một module trong `lib/`, khởi tạo sau khi đăng nhập, đóng khi đăng xuất).
- CONNECT tới `NEXT_PUBLIC_WS_URL` với header `Authorization: Bearer <accessToken>`, heartbeat 10 giây mỗi chiều; chỉ SUBSCRIBE kênh có trong `docs/05` phần B.
- Mất kết nối: thử lại theo backoff 1s → 2s → 4s … tối đa 30s, hiện `ConnectionBanner`. Token hết hạn: refresh qua REST rồi kết nối lại.
- **Sau mỗi lần kết nối lại** PHẢI gọi `GET /api/notifications?after=<createdAt của thông báo cuối đã có>` rồi gộp, để không mất thông báo (SD-09). Trạng thái kết nối theo `docs/06` mục 6.
- Thông báo nhận qua WebSocket và qua REST là cùng kiểu `NotificationDto`; bấm vào thì đi tới `link`.

---

## 10. Phong cách code

- Prettier (`.prettierrc`): 2 dấu cách, nháy đơn, có `;`, không dấu phẩy cuối, dòng ≤ 120. ESLint theo `eslint.config.mjs` (Next + TypeScript + Prettier). Import dùng alias `@/…`.
- Thứ tự import: thư viện ngoài → `@/…` → file cùng thư mục, cách nhau một dòng trống (theo code hiện có).
- Đặt tên: component `PascalCase`, hook `useXxx`, hằng số `UPPER_SNAKE_CASE`, file tiện ích `camelCase.ts`.
- Comment tiếng Việt, giải thích *vì sao*; chia section bằng `// ─── Tiêu đề ───`. KHÔNG để code chết, `console.log`, TODO không có mã việc trong PROGRESS.
- Viết giống code xung quanh trước khi nghĩ cách mới.

## 11. Kiểm thử

- Hàm thuần trong `lib/` (đếm từ, làm tròn band xem trước, tính độ lệch giờ…) PHẢI có test `*.test.ts`, chạy bằng `node --test` (`npm run test:unit`, không cần thư viện thêm).
- Trước khi bàn giao: `npm run lint`, `npm run build`, `npm run test:unit` đều sạch, và chạy thử tay luồng vừa làm với backend thật (`npm run dev` + `mvn spring-boot:run`).

---

## Checklist trước khi bàn giao (frontend)

```
[ ] Màn hình làm theo thiết kế Figma + docs/07; không có mã hex trong component
[ ] Gọi API qua lib/api/*, kiểu ở lib/types/* khớp docs/05
[ ] Trạng thái / band / ngày giờ qua status.ts và date.ts
[ ] Có trạng thái đang tải, rỗng, lỗi; chạy được ở màn hình nhỏ
[ ] Không tự quyết định nghiệp vụ mà server đã quyết định (mục 8)
[ ] Không log token, không đặt token vào URL
[ ] npm run lint · npm run build · npm run test:unit sạch; đã chạy thử với backend thật
[ ] PROGRESS.md đã có mục mới; báo cáo xong thì dừng chờ xác nhận
```
