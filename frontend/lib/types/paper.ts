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
