'use client';

import { useMemo, useState } from 'react';

import { Alert, App, Button, Empty, Input, Popconfirm, Skeleton, Tag } from 'antd';
import { FileCode2, Pencil, Plus, Trash2 } from 'lucide-react';

import { AppDrawer } from '@/components/AppDrawer';
import { IconButton } from '@/components/IconButton';
import { paperReferenceApi } from '@/lib/api/paperReference';
import type { CitationStyle } from '@/lib/types/assignment';
import type { CslItem, ReferenceDto } from '@/lib/types/paper';
import { formatReference, missingText } from '@/lib/utils/citation';

import { BibtexImportModal } from './BibtexImportModal';
import { ReferenceFormModal } from './ReferenceFormModal';

export { useGroupReferences } from './useGroupReferences';

// ─── Thư viện trích dẫn của nhóm (UC-36, BR-30) ───────────────────────────────
// Drawer trên màn làm bài Paper: tra DOI, thêm tay, nhập BibTeX, sửa, xoá. Mỗi mục hiện theo kiểu trích dẫn của bài
// (IEEE / APA) và cảnh báo trường còn thiếu do server trả về. `editable = false`: chỉ xem (giáo viên, bài đã đóng).
// Danh sách do `useGroupReferences` của màn hình giữ (trình soạn cũng dùng để đánh số trích dẫn); sửa xong gọi `reload`.

type FormState = { mode: 'add' | 'doi' | 'edit'; initial: CslItem; id?: number } | null;

const FORM_TEXT = {
  add: { title: 'Thêm tài liệu tham khảo', ok: 'Thêm' },
  doi: { title: 'Kết quả tra DOI — kiểm tra lại rồi thêm', ok: 'Thêm vào thư viện' },
  edit: { title: 'Sửa tài liệu tham khảo', ok: 'Lưu' }
} as const;

export function ReferenceLibrary({
  open,
  onClose,
  groupId,
  style,
  editable,
  refs,
  failed,
  reload
}: {
  open: boolean;
  onClose: () => void;
  groupId: number;
  style: CitationStyle;
  editable: boolean;
  refs: ReferenceDto[] | null;
  failed: boolean;
  reload: () => void;
}) {
  const { message } = App.useApp();
  const [doi, setDoi] = useState('');
  const [looking, setLooking] = useState(false);
  const [form, setForm] = useState<FormState>(null);
  const [bibOpen, setBibOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  const formatted = useMemo(
    () => new Map((refs ?? []).map((r) => [r.id, formatReference(r.csl, style)])),
    [refs, style]
  );

  const lookup = async () => {
    if (!doi.trim()) return;
    setLooking(true);
    try {
      const r = await paperReferenceApi.lookupDoi(doi.trim());
      setForm({ mode: 'doi', initial: r.csl });
    } catch {
      // axiosClient đã toast (không có DOI, dịch vụ lỗi, tra quá nhiều)
    } finally {
      setLooking(false);
    }
  };

  const save = async (csl: CslItem) => {
    if (!form) return;
    setSaving(true);
    try {
      if (form.mode === 'edit' && form.id !== undefined) {
        await paperReferenceApi.update(groupId, form.id, csl);
        message.success('Đã lưu tài liệu tham khảo');
      } else {
        await paperReferenceApi.add(groupId, csl);
        message.success('Đã thêm vào thư viện');
        setDoi('');
      }
      setForm(null);
      reload();
    } catch {
      // đã toast (trùng DOI, thiếu tiêu đề…)
    } finally {
      setSaving(false);
    }
  };

  const importBib = async (items: CslItem[]) => {
    setSaving(true);
    try {
      const r = await paperReferenceApi.addBatch(groupId, items);
      message.success(
        `Đã thêm ${r.added.length} tài liệu${r.skipped ? `, bỏ qua ${r.skipped} tài liệu trùng DOI` : ''}`
      );
      setBibOpen(false);
      reload();
    } catch {
      // đã toast (mục sai, vượt giới hạn)
    } finally {
      setSaving(false);
    }
  };

  const remove = async (id: number) => {
    try {
      await paperReferenceApi.remove(groupId, id);
      reload();
    } catch {
      // đã toast
    }
  };

  return (
    <AppDrawer
      open={open}
      onClose={onClose}
      title="Tài liệu tham khảo của nhóm"
      subtitle={`Kiểu trích dẫn: ${style}${refs ? ` · ${refs.length} tài liệu` : ''}`}
      size={640}
    >
      <div className="flex flex-col gap-4">
        {editable && (
          <div className="flex flex-col gap-2">
            <Input.Search
              value={doi}
              onChange={(e) => setDoi(e.target.value)}
              onSearch={() => void lookup()}
              loading={looking}
              enterButton="Tra DOI"
              placeholder="Dán DOI hoặc link doi.org, vd 10.1145/3368089.3409731"
              aria-label="DOI cần tra"
            />
            <div className="flex flex-wrap gap-2">
              <Button icon={<Plus size={16} />} onClick={() => setForm({ mode: 'add', initial: {} })}>
                Nhập tay
              </Button>
              <Button icon={<FileCode2 size={16} />} onClick={() => setBibOpen(true)}>
                Nhập BibTeX
              </Button>
            </div>
          </div>
        )}

        {failed ? (
          <Alert
            type="error"
            showIcon
            title="Không tải được thư viện"
            action={<Button onClick={reload}>Thử lại</Button>}
          />
        ) : refs === null ? (
          <Skeleton active paragraph={{ rows: 6 }} />
        ) : refs.length === 0 ? (
          <Empty
            description={
              editable
                ? 'Chưa có tài liệu nào. Tra DOI, nhập tay hoặc nhập BibTeX để bắt đầu.'
                : 'Nhóm chưa có tài liệu nào.'
            }
          />
        ) : (
          <ul className="m-0 flex list-none flex-col gap-2 p-0">
            {refs.map((r) => (
              <li key={r.id} className="flex gap-3 rounded-md border border-neutral-100 bg-neutral-0 p-3">
                <div className="min-w-0 flex-1">
                  <p className="m-0 font-serif text-body break-words text-neutral-900">{formatted.get(r.id)}</p>
                  <div className="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-caption text-neutral-500">
                    {r.missing.length > 0 && (
                      <Tag color="warning" className="m-0">
                        {missingText(r.missing)}
                      </Tag>
                    )}
                    {r.createdByName && <span>Thêm bởi {r.createdByName}</span>}
                  </div>
                </div>
                {editable && (
                  <div className="flex shrink-0 items-start gap-1">
                    <IconButton
                      title="Sửa"
                      type="text"
                      size="small"
                      icon={<Pencil size={16} />}
                      onClick={() => setForm({ mode: 'edit', initial: r.csl, id: r.id })}
                    />
                    <Popconfirm
                      title="Xoá tài liệu này khỏi thư viện?"
                      description="Chỗ đang trích dẫn tài liệu này trong bài sẽ mất số tham chiếu."
                      okText="Xoá"
                      okButtonProps={{ danger: true }}
                      cancelText="Huỷ"
                      onConfirm={() => remove(r.id)}
                    >
                      <IconButton title="Xoá" type="text" size="small" danger icon={<Trash2 size={16} />} />
                    </Popconfirm>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      <ReferenceFormModal
        open={form !== null}
        title={form ? FORM_TEXT[form.mode].title : ''}
        okText={form ? FORM_TEXT[form.mode].ok : ''}
        initial={form?.initial ?? {}}
        saving={saving}
        onSave={(csl) => void save(csl)}
        onClose={() => setForm(null)}
      />
      <BibtexImportModal
        open={bibOpen}
        style={style}
        saving={saving}
        onImport={(items) => void importBib(items)}
        onClose={() => setBibOpen(false)}
      />
    </AppDrawer>
  );
}
