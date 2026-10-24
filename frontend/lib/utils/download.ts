// ─── Tải file từ response blob ────────────────────────────────────────────────
// `filenameFromDisposition` là hàm thuần (có test); `saveBlob` cần DOM.

/**
 * Tên file trong header Content-Disposition. Ưu tiên `filename*=UTF-8''…` (tên tiếng Việt),
 * rồi tới `filename="…"`; không có thì null.
 */
export function filenameFromDisposition(header: string | null | undefined): string | null {
  if (!header) return null;
  const encoded = /filename\*\s*=\s*(?:UTF-8|utf-8)''([^;]+)/.exec(header);
  if (encoded) {
    try {
      return decodeURIComponent(encoded[1].trim().replace(/^"|"$/g, ''));
    } catch {
      // tên mã hoá sai → thử dạng thường
    }
  }
  const plain = /filename\s*=\s*"?([^";]+)"?/.exec(header);
  return plain ? plain[1].trim() : null;
}

/** Lưu blob thành file trên máy người dùng. */
export function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1_000);
}
