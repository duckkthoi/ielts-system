// ---------------------------------------------------------------------------
// Đếm từ theo quy ước IELTS (BR-20, docs/08-nghiep-vu-cham-ielts.md mục 3.4)
// ---------------------------------------------------------------------------
// Tách theo khoảng trắng (kể cả khoảng trắng không ngắt); mỗi cụm có ít nhất một chữ cái
// hoặc chữ số là một từ. Cùng thuật toán với backend (submission/WordCounter.java); cả hai
// chạy bộ ca kiểm thử chung shared/word-count-cases.json.

const SEPARATOR = /[\s   ]+/u;
const HAS_WORD_CHAR = /[\p{L}\p{N}]/u;

export function countWords(text: string | null | undefined): number {
  if (!text || !text.trim()) return 0;
  return text
    .trim()
    .split(SEPARATOR)
    .filter((token) => HAS_WORD_CHAR.test(token)).length;
}
