import type { LiveCommentKind } from '@/lib/theme/status';

// ─── Paper theo nhóm (docs/05 mục A.10, docs/12) ──────────────────────────────

export interface GroupMember {
  studentId: number;
  studentCode: string;
  fullName: string;
  joinedAt: string;
}

/** `studentMade`: sinh viên tự lập — tự xoá khi không còn ai. */
export interface PaperGroup {
  id: number;
  name: string;
  studentMade: boolean;
  members: GroupMember[];
}

export interface StudentRefLite {
  id: number;
  studentCode: string;
  fullName: string;
}

/** Màn chia nhóm của giáo viên. `lockAt`: hạn chốt nhóm thực tế (không đặt thì là hạn nộp). */
export interface TeacherGroups {
  assignmentId: number;
  maxGroupSize: number;
  lockAt: string | null;
  locked: boolean;
  groups: PaperGroup[];
  ungrouped: StudentRefLite[];
}

/** Phía sinh viên: `canChange` = còn tự lập / vào / rời nhóm được (BR-27). */
export interface StudentGroups {
  assignmentId: number;
  myGroupId: number | null;
  canChange: boolean;
  maxGroupSize: number;
  lockAt: string | null;
  groups: PaperGroup[];
}

// ─── Tài liệu paper cùng sửa (Yjs, docs/05 A.10 + B.2) ────────────────────────
// Nhị phân Yjs đi dạng base64.

export interface DocUpdate {
  seq: number;
  update: string;
  authorId: number | null;
  createdAt: string;
}

/** Dựng tài liệu = áp `snapshot` rồi lần lượt `updates`. `editable = false`: nhóm đã nộp / bài đã đóng. */
export interface DocumentState {
  documentId: number;
  partId: number;
  groupId: number;
  snapshot: string | null;
  snapshotSeq: number;
  lastSeq: number;
  updates: DocUpdate[];
  editable: boolean;
}

/** `resync = true`: các update cần lấy đã bị gộp — tải lại toàn bộ `DocumentState`. */
export interface UpdatesAfter {
  updates: DocUpdate[];
  lastSeq: number;
  resync: boolean;
}

/** `/topic/paper/{id}`: update vừa được lưu. */
export interface UpdatePush {
  documentId: number;
  seq: number;
  update: string;
  authorId: number;
  clientId: string | null;
}

// ─── Thư viện trích dẫn của nhóm (docs/05 A.10, B24) ──────────────────────────
// Tài liệu tham khảo là bản ghi CSL-JSON (server đã lọc trường); định dạng IEEE / APA làm ở client.

export interface CslName {
  given?: string;
  family?: string;
  literal?: string;
}

export interface CslDate {
  'date-parts'?: (number | string)[][];
  literal?: string;
  raw?: string;
}

export interface CslItem {
  type?: string;
  title?: string;
  author?: CslName[];
  editor?: CslName[];
  issued?: CslDate;
  'container-title'?: string;
  volume?: string | number;
  issue?: string | number;
  page?: string;
  publisher?: string;
  URL?: string;
  DOI?: string;
  [field: string]: unknown;
}

/** Trường bắt buộc còn thiếu (BR-30) — chỉ cảnh báo. */
export type MissingField = 'AUTHOR' | 'YEAR' | 'SOURCE';

export interface ReferenceDto {
  id: number;
  csl: CslItem;
  doi: string | null;
  missing: MissingField[];
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  updatedAt: string;
}

/** `skipped`: số mục bỏ qua vì DOI đã có trong thư viện hoặc lặp lại trong lần nhập. */
export interface BatchResult {
  added: ReferenceDto[];
  skipped: number;
}

/** Kết quả tra DOI — chưa lưu. */
export interface DoiLookup {
  doi: string;
  csl: CslItem;
  missing: MissingField[];
}

// ─── Nhận xét trực tiếp trên Paper đang viết (H4, docs/05 mục A.10) ──────────

export type { LiveCommentKind };

/** Offset tính trên chữ thuần của tài liệu lúc tạo (`docText`); `quote` giữ nguyên để phát hiện đoạn đã đổi. */
export interface LiveComment {
  id: number;
  documentId: number;
  authorId: number;
  authorName: string;
  startOffset: number;
  endOffset: number;
  quote: string;
  comment: string;
  kind: LiveCommentKind;
  suggestion: string | null;
  sectionKey: string | null;
  resolvedAt: string | null;
  resolvedByName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LiveCommentPush {
  type: 'CREATED' | 'UPDATED' | 'DELETED' | 'RESOLVED';
  comment: LiveComment | null;
  commentId: number;
}

export interface LiveCommentInput {
  startOffset: number;
  endOffset: number;
  quote: string;
  comment: string;
  kind: LiveCommentKind;
  suggestion?: string | null;
  sectionKey?: string | null;
}

/** Tài liệu của một nhóm cho một giai đoạn — danh sách xem trực tiếp của giáo viên. */
export interface TeacherPaperDocument {
  documentId: number;
  partId: number;
  partTitle: string;
  groupId: number;
  groupName: string;
  members: string | null;
  lastSeq: number;
  updatedAt: string;
  openComments: number;
  totalComments: number;
}
