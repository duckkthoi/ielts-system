import type { FormRule } from 'antd';

export const PASSWORD_MIN_LENGTH = 8;

const LETTER_AND_DIGIT = /(?=.*[A-Za-z])(?=.*\d)/;

/** Các điều kiện hiện ngay dưới ô mật khẩu mới, tick xanh khi đạt. Cùng quy tắc với server (đổi mật khẩu, docs/05). */
export const PASSWORD_CHECKS: { label: string; passed: (password: string, current: string) => boolean }[] = [
  { label: `Ít nhất ${PASSWORD_MIN_LENGTH} ký tự`, passed: (password) => password.length >= PASSWORD_MIN_LENGTH },
  { label: 'Có cả chữ và số', passed: (password) => LETTER_AND_DIGIT.test(password) },
  { label: 'Khác mật khẩu hiện tại', passed: (password, current) => password.length > 0 && password !== current }
];

// Chi tiết từng điều kiện đã có ở danh sách PASSWORD_CHECKS nên lỗi của ô chỉ cần một dòng chung.
export const NEW_PASSWORD_RULES: FormRule[] = [
  { required: true, message: 'Vui lòng nhập mật khẩu mới' },
  ({ getFieldValue }) => ({
    validator(_, value: string | undefined) {
      const current: string = getFieldValue('currentPassword') ?? '';
      if (!value || PASSWORD_CHECKS.every((check) => check.passed(value, current))) return Promise.resolve();
      return Promise.reject(new Error('Mật khẩu mới chưa đạt các điều kiện bên dưới'));
    }
  })
];

export const CONFIRM_PASSWORD_RULES: FormRule[] = [
  { required: true, message: 'Vui lòng nhập lại mật khẩu mới' },
  ({ getFieldValue }) => ({
    validator(_, value) {
      if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
      return Promise.reject(new Error('Mật khẩu nhập lại không khớp'));
    }
  })
];
