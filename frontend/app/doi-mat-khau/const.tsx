import type { FormRule } from 'antd';

export const PASSWORD_MIN_LENGTH = 8;

export const NEW_PASSWORD_RULES: FormRule[] = [
  { required: true, message: 'Vui lòng nhập mật khẩu mới' },
  { min: PASSWORD_MIN_LENGTH, message: `Mật khẩu tối thiểu ${PASSWORD_MIN_LENGTH} ký tự` },
  { pattern: /(?=.*[A-Za-z])(?=.*\d)/, message: 'Mật khẩu phải có cả chữ và số' }
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
