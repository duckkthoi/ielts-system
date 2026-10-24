import { Tag } from 'antd';

// ─── Trạng thái tài khoản: Hoạt động / Đã khoá, kèm "Chưa đổi mật khẩu" ──────

export function AccountStatusTag({ active, mustChangePassword }: { active: boolean; mustChangePassword?: boolean }) {
  return (
    <span className="inline-flex flex-wrap gap-1">
      {active ? <Tag color="success">Hoạt động</Tag> : <Tag color="error">Đã khoá</Tag>}
      {active && mustChangePassword && <Tag color="warning">Chưa đổi mật khẩu</Tag>}
    </span>
  );
}
