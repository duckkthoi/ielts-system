// ─── Design tokens (giá trị cho JavaScript) ──────────────────────────────────
// Nguồn: docs/07-design-system.md. Cùng bộ giá trị với @theme trong app/globals.css.
// Dùng khi cần giá trị JS: theme antd, biểu đồ, style động. Trong JSX ưu tiên class Tailwind.

export const BRAND = {
  50: '#EFF4FF',
  100: '#DBE6FE',
  200: '#BFD3FE',
  500: '#3B6FF6',
  600: '#2457E6',
  700: '#1C44C2',
  900: '#172C6E'
} as const;

export const ACCENT = {
  50: '#FFFBEB',
  500: '#F59E0B'
} as const;

export const NEUTRAL = {
  0: '#FFFFFF',
  25: '#F8FAFC',
  50: '#F1F5F9',
  100: '#E2E8F0',
  300: '#CBD5E1',
  400: '#94A3B8',
  500: '#64748B',
  700: '#334155',
  900: '#0F172A'
} as const;

export interface ColorSet {
  text: string;
  bg: string;
  border: string;
}

export interface SolidColorSet extends ColorSet {
  solid: string;
}

export const SEMANTIC = {
  success: { text: '#16A34A', bg: '#F0FDF4', border: '#BBF7D0' },
  warning: { text: '#D97706', bg: '#FFFBEB', border: '#FDE68A' },
  error: { text: '#DC2626', bg: '#FEF2F2', border: '#FECACA' },
  info: { text: '#0284C7', bg: '#F0F9FF', border: '#BAE6FD' }
} as const satisfies Record<string, ColorSet>;

export const PRESENCE = {
  online: '#22C55E',
  away: '#F59E0B',
  offline: '#CBD5E1'
} as const;

export const FONT_FAMILY = {
  sans: 'var(--font-roboto), system-ui, -apple-system, "Segoe UI", sans-serif',
  serif: 'var(--font-noto-serif), Georgia, "Times New Roman", serif',
  mono: 'var(--font-jetbrains-mono), ui-monospace, Consolas, monospace'
} as const;

export const RADIUS = { sm: 4, md: 8, lg: 12, full: 9999 } as const;

export const SHADOW = {
  sm: '0 1px 2px rgba(15,23,42,.06)',
  md: '0 4px 12px rgba(15,23,42,.08)',
  lg: '0 12px 32px rgba(15,23,42,.12)'
} as const;

export const LAYOUT = {
  headerHeight: 64,
  siderWidth: 240,
  siderCollapsedWidth: 72,
  contentMaxWidth: 1440,
  essayMaxWidth: 720
} as const;
