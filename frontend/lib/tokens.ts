import { appStorage, STORAGE_KEYS } from '@/lib/storage';
import type { TokenPair } from '@/lib/types/auth';

// ─── Lưu / đọc JWT ────────────────────────────────────────────────────────────

export function getAccessToken(): string | null {
  return appStorage.get(STORAGE_KEYS.accessToken);
}

export function getRefreshToken(): string | null {
  return appStorage.get(STORAGE_KEYS.refreshToken);
}

export function saveTokens(tokens: TokenPair) {
  appStorage.set(STORAGE_KEYS.accessToken, tokens.accessToken);
  appStorage.set(STORAGE_KEYS.refreshToken, tokens.refreshToken);
}

export function clearStoredTokens() {
  appStorage.remove(STORAGE_KEYS.accessToken);
  appStorage.remove(STORAGE_KEYS.refreshToken);
}
