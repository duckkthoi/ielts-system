// ---------------------------------------------------------------------------
// Date utilities — parse/format chuỗi thời gian từ backend
// ---------------------------------------------------------------------------
// Backend (Spring, kiểu Instant) luôn trả ISO 8601 có hậu tố "Z" (UTC).
// Luôn parse qua `parseServerDate` rồi mới format/tính toán để hiển thị đúng giờ local.

import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import utc from 'dayjs/plugin/utc';
import 'dayjs/locale/vi';

dayjs.extend(utc);
dayjs.extend(relativeTime);
dayjs.locale('vi');

const OFFSET_SUFFIX_RE = /(Z|[+-]\d{2}:?\d{2})$/;

export function parseServerDate(date: dayjs.ConfigType) {
  if (typeof date === 'string' && !OFFSET_SUFFIX_RE.test(date)) {
    return dayjs.utc(date).local();
  }
  return dayjs(date);
}

export const formatDate = (date: dayjs.ConfigType) => parseServerDate(date).format('DD/MM/YYYY');

export const formatDateTime = (date: dayjs.ConfigType) => parseServerDate(date).format('DD/MM/YYYY HH:mm');

export const formatTime = (date: dayjs.ConfigType) => parseServerDate(date).format('HH:mm');

/** "3 phút trước", "2 ngày trước"… */
export const formatRelative = (date: dayjs.ConfigType) => parseServerDate(date).fromNow();
