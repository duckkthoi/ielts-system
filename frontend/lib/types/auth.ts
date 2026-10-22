import type { Role } from '@/lib/constants';

export interface AccountInfo {
  id: number;
  username: string;
  fullName: string;
  email: string | null;
  role: Role;
  /** Chỉ có với sinh viên. */
  studentCode: string | null;
  mustChangePassword: boolean;
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  /** Thời điểm hết hạn access token (ISO 8601, UTC). */
  accessTokenExpiresAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse extends TokenPair {
  user: AccountInfo;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
