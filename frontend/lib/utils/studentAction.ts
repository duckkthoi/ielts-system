// ─── Nút chính trên thẻ bài được giao (Prompt 7 A) ────────────────────────────
// Hàm thuần, không import alias để `node --test` chạy trực tiếp. Chỉ chọn nút để hiển thị —
// được nộp hay không vẫn do server quyết định qua `canSubmit` (frontend/AI-CODING-RULES.md mục 8).

interface PartLike {
  partId: number;
  status: string;
  canSubmit: boolean;
}

/** revise = "Sửa bài" · write = "Làm bài" · result = "Xem kết quả" · view = "Xem chi tiết". */
export type StudentActionKind = 'revise' | 'write' | 'result' | 'view';

/**
 * Ưu tiên: phần đang "Cần sửa" còn nộp được → phần chưa làm / đang làm còn nộp được (theo thứ tự phần)
 * → đã có phần được trả bài → xem chi tiết.
 */
export function nextStudentAction(parts: readonly PartLike[]): { kind: StudentActionKind; partId: number | null } {
  const revise = parts.find((p) => p.status === 'REVISION_REQUESTED' && p.canSubmit);
  if (revise) return { kind: 'revise', partId: revise.partId };
  const write = parts.find((p) => (p.status === 'NOT_STARTED' || p.status === 'DRAFT') && p.canSubmit);
  if (write) return { kind: 'write', partId: write.partId };
  if (parts.some((p) => p.status === 'RETURNED')) return { kind: 'result', partId: null };
  return { kind: 'view', partId: null };
}
