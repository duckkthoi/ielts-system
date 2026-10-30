'use client';

import { Button, Form, Input, InputNumber, Tooltip } from 'antd';
import { Plus, Trash2, Wand2 } from 'lucide-react';

import { defaultLevels, levelProblems, MAX_LEVELS, type LevelInput } from '@/lib/utils/rubricLevels';

// ─── Mức đạt của một tiêu chí bài Paper (rubric phân tích, BR-31) ─────────────
// Mỗi mức: tên, khoảng điểm [thấp nhất, cao nhất], mô tả "bài ở mức này trông như thế nào". Khi chấm, giáo viên bấm mức
// rồi chọn điểm trong khoảng. Không có mức nào thì chấm điểm trực tiếp như bài luận.

export function LevelsEditor({ partIndex, index }: { partIndex: number; index: number }) {
  const form = Form.useFormInstance();
  const criterionPath = ['parts', partIndex, 'criteria', index];

  const scale = () => {
    const c = form.getFieldValue(criterionPath) ?? {};
    return { maxScore: Number(c.maxScore ?? 10), step: Number(c.step ?? 0.25) };
  };

  return (
    <div className="mt-4">
      <p className="mb-1 text-small font-semibold text-neutral-700">Mức đạt</p>
      <p className="mb-2 text-caption text-neutral-500">
        Khi chấm, bấm một mức rồi chọn điểm trong khoảng của mức đó. Các khoảng không được chồng nhau.
      </p>
      <Form.List
        name={[index, 'levels']}
        rules={[
          {
            validator: async (_, value?: LevelInput[]) => {
              const { maxScore, step } = scale();
              const problems = levelProblems(value ?? [], maxScore, step);
              if (problems.length) throw new Error(problems[0]);
            }
          }
        ]}
      >
        {(items, { add, remove }, { errors }) => (
          <div className="flex flex-col gap-2">
            {items.map((item) => (
              <div key={item.key} className="rounded-md border border-neutral-100 bg-neutral-25 p-2">
                <div className="flex flex-wrap items-start gap-2 sm:flex-nowrap">
                  <Form.Item
                    name={[item.name, 'label']}
                    className="mb-0 min-w-0 flex-1"
                    rules={[
                      { required: true, whitespace: true, message: 'Nhập tên mức' },
                      { max: 50, message: 'Tối đa 50 ký tự' }
                    ]}
                  >
                    <Input size="small" placeholder="Tốt" aria-label="Tên mức" />
                  </Form.Item>
                  <Form.Item
                    name={[item.name, 'minScore']}
                    className="mb-0 w-24"
                    rules={[{ required: true, message: 'Nhập điểm' }]}
                  >
                    <InputNumber
                      size="small"
                      min={0}
                      max={10}
                      step={0.25}
                      placeholder="Từ"
                      aria-label="Điểm thấp nhất"
                    />
                  </Form.Item>
                  <span className="pt-1 text-neutral-400">–</span>
                  <Form.Item
                    name={[item.name, 'maxScore']}
                    className="mb-0 w-24"
                    rules={[{ required: true, message: 'Nhập điểm' }]}
                  >
                    <InputNumber
                      size="small"
                      min={0}
                      max={10}
                      step={0.25}
                      placeholder="Đến"
                      aria-label="Điểm cao nhất"
                    />
                  </Form.Item>
                  <Button
                    size="small"
                    type="text"
                    danger
                    icon={<Trash2 size={14} />}
                    onClick={() => remove(item.name)}
                    aria-label="Xoá mức"
                  />
                </div>
                <Form.Item
                  name={[item.name, 'descriptor']}
                  className="mt-2 mb-0"
                  rules={[{ max: 2000, message: 'Tối đa 2000 ký tự' }]}
                >
                  <Input.TextArea
                    size="small"
                    autoSize={{ minRows: 1, maxRows: 4 }}
                    placeholder="Bài ở mức này trông như thế nào"
                    aria-label="Mô tả mức"
                  />
                </Form.Item>
              </div>
            ))}
            <Form.ErrorList errors={errors} />
            <div className="flex flex-wrap gap-2">
              {items.length < MAX_LEVELS && (
                <Button
                  size="small"
                  type="dashed"
                  icon={<Plus size={14} />}
                  onClick={() => add({ label: '', minScore: null, maxScore: null, descriptor: '' })}
                >
                  Thêm mức
                </Button>
              )}
              {items.length === 0 && (
                <Tooltip title="Xuất sắc / Tốt / Đạt / Chưa đạt theo thang điểm và bước điểm của tiêu chí">
                  <Button
                    size="small"
                    icon={<Wand2 size={14} />}
                    onClick={() => {
                      const { maxScore, step } = scale();
                      defaultLevels(maxScore, step).forEach((l) => add({ ...l, descriptor: '' }));
                    }}
                  >
                    Dùng 4 mức mặc định
                  </Button>
                </Tooltip>
              )}
            </div>
          </div>
        )}
      </Form.List>
    </div>
  );
}
