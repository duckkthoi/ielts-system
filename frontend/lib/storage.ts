// ─── Storage của app ──────────────────────────────────────────────────────────
// Mọi key localStorage đi qua namespace `ielts:` để không đụng app khác cùng origin.
// Bọc try/catch: SSR hoặc trình duyệt chặn storage thì trả null thay vì crash.

const PREFIX = 'ielts:';

export const STORAGE_KEYS = {
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  /** Store auth (user) — `useAuthStore`. */
  auth: 'auth'
} as const;

type StorageKey = (typeof STORAGE_KEYS)[keyof typeof STORAGE_KEYS];

export const appStorage = {
  prefix: PREFIX,
  get(key: StorageKey): string | null {
    try {
      return globalThis.localStorage?.getItem(PREFIX + key) ?? null;
    } catch {
      return null;
    }
  },
  set(key: StorageKey, value: string) {
    try {
      globalThis.localStorage?.setItem(PREFIX + key, value);
    } catch {
      // bỏ qua: storage bị chặn
    }
  },
  remove(key: StorageKey) {
    try {
      globalThis.localStorage?.removeItem(PREFIX + key);
    } catch {
      // bỏ qua: storage bị chặn
    }
  }
};
