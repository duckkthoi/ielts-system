// ─── Đề bài: văn bản thường ⇄ HTML ────────────────────────────────────────────
// Giáo viên gõ đề bằng ô văn bản; server lưu HTML (đã lọc, docs/05 A.5). Hàm thuần, có test.

const ESCAPES: Record<string, string> = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };

function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (ch) => ESCAPES[ch]);
}

/** Đoạn cách nhau bằng dòng trống → `<p>`; xuống dòng trong đoạn → `<br>`. Ký tự HTML được escape. */
export function textToHtml(text: string): string {
  return text
    .replace(/\r\n?/g, '\n')
    .split(/\n\s*\n/)
    .map((para) => para.trim())
    .filter(Boolean)
    .map((para) => `<p>${escapeHtml(para).replace(/\n/g, '<br>')}</p>`)
    .join('');
}

const ENTITIES: Record<string, string> = { amp: '&', lt: '<', gt: '>', quot: '"', '#39': "'", apos: "'", nbsp: ' ' };

/** Ngược lại: `<p>` / `<li>` thành đoạn, `<br>` thành xuống dòng, bỏ thẻ khác, giải mã entity cơ bản. */
export function htmlToText(html: string): string {
  return html
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/(p|div|h[1-6])>/gi, '\n\n')
    .replace(/<li[^>]*>/gi, '- ')
    .replace(/<\/li>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&(amp|lt|gt|quot|#39|apos|nbsp);/g, (_, name: string) => ENTITIES[name])
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}
