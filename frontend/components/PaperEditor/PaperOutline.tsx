'use client';

import { AlertTriangle, Check, Circle } from 'lucide-react';

import { getPaperWordHint, PAPER_WORD_HINT_META, type PaperWordHint } from '@/lib/theme/status';
import { NEUTRAL } from '@/lib/theme/tokens';
import { wordLimitLabel, type SectionCount } from '@/lib/utils/paperDoc';

// ─── Mục lục của paper + số từ từng mục (FR-73) ───────────────────────────────
// Bấm một mục → cuộn tới mục đó trong trình soạn (`#muc-{sectionKey}`, do node paperSection đặt).
// Thiếu / vượt / mục bắt buộc còn trống chỉ để nhắc; server mới là nơi chặn nộp (BR-29, B25).

function HintIcon({ hint }: { hint: PaperWordHint }) {
  const color = PAPER_WORD_HINT_META[hint].text;
  if (hint === 'ok') return <Check size={14} color={color} aria-hidden />;
  if (hint === 'empty-optional') return <Circle size={14} color={color} aria-hidden />;
  return <AlertTriangle size={14} color={color} aria-hidden />;
}

export function PaperOutline({ counts }: { counts: SectionCount[] }) {
  const total = counts.reduce((sum, c) => sum + c.words, 0);

  const jump = (sectionKey: string) =>
    document.getElementById(`muc-${sectionKey}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });

  return (
    <nav aria-label="Mục lục" className="flex flex-col gap-1">
      <h3 className="mb-1 text-small font-semibold tracking-wide text-neutral-500 uppercase">Mục lục</h3>
      <ol className="m-0 flex list-none flex-col gap-0.5 p-0">
        {counts.map((c) => {
          const hint = getPaperWordHint(c.hint, c.required);
          const meta = PAPER_WORD_HINT_META[hint];
          const limit = wordLimitLabel(c.minWords, c.maxWords);
          return (
            <li key={c.sectionKey}>
              <button
                type="button"
                onClick={() => jump(c.sectionKey)}
                title={meta.label}
                className="flex w-full cursor-pointer items-start gap-2 rounded-md border-0 bg-transparent px-2 py-1.5 text-left hover:bg-neutral-50"
              >
                <span className="mt-1 shrink-0">
                  <HintIcon hint={hint} />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-body text-neutral-900">
                    {c.title}
                    {c.required && <span className="text-error"> *</span>}
                  </span>
                  <span className="block text-caption" style={{ color: hint === 'ok' ? NEUTRAL[500] : meta.text }}>
                    {c.words} từ{limit ? ` / ${limit}` : ''}
                    {hint !== 'ok' && hint !== 'empty-optional' ? ` · ${meta.label}` : ''}
                  </span>
                </span>
              </button>
            </li>
          );
        })}
      </ol>
      <p className="mt-1 border-t border-neutral-100 px-2 pt-2 text-caption text-neutral-500">
        Cả bài: <span className="font-semibold text-neutral-700">{total} từ</span>
        <span className="block">
          <span className="text-error">*</span> bắt buộc có nội dung mới nộp được
        </span>
      </p>
    </nav>
  );
}
