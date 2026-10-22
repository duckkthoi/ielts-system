'use client';

import { Modal } from 'antd';
import type { ModalProps } from 'antd';

export interface AppModalProps extends Omit<ModalProps, 'onCancel'> {
  onClose?: () => void;
}

export function AppModal({ children, onClose, ...props }: AppModalProps) {
  return (
    <Modal footer={null} centered destroyOnHidden onCancel={onClose} {...props}>
      {children}
    </Modal>
  );
}

export default AppModal;
