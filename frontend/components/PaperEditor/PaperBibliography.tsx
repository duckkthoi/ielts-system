import type { CitationStyle } from '@/lib/types/assignment';

// ─── Danh mục tài liệu tham khảo cuối bài (BR-30) ─────────────────────────────
// Sinh tự động từ các trích dẫn có trong bài, không sửa tay: IEEE theo số thứ tự xuất hiện, APA theo ABC (thụt dòng
// treo). Nằm ngoài tài liệu Yjs — lúc nộp / xuất file server dựng lại từ thư viện (B25, B27).

export function PaperBibliography({
  entries,
  style
}: {
  entries: { id: number; text: string }[];
  style: CitationStyle;
}) {
  return (
    <section aria-label="Danh mục tài liệu tham khảo" className="mt-8 border-t border-neutral-100 pt-6">
      <h2 className="m-0 mb-1 font-sans text-h2 font-semibold text-neutral-900">References</h2>
      <p className="m-0 mb-3 text-caption text-neutral-500">
        Tự sinh từ các trích dẫn trong bài theo kiểu {style} — muốn đổi thì sửa trích dẫn hoặc thư viện.
      </p>
      {entries.length === 0 ? (
        <p className="m-0 text-small text-neutral-400">Chưa có trích dẫn nào trong bài.</p>
      ) : (
        <ol className="m-0 flex list-none flex-col gap-2 p-0 font-serif text-[15px]/[26px] text-neutral-900">
          {entries.map((e) => (
            <li key={e.id} className={style === 'APA' ? 'pl-8 -indent-8' : undefined}>
              {e.text}
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
