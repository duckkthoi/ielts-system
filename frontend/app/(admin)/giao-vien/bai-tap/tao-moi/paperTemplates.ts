import type { CitationStyle } from '@/lib/types/assignment';

import type { CriterionForm, PartForm, SectionForm } from './type';

// ─── Mẫu cho bài loại Paper (docs/12) ─────────────────────────────────────────
// Khung mục theo IMRaD của bài báo ngành IT; rubric mẫu 6 tiêu chí thang 10 (docs/12 mục 5.4). Giáo viên sửa tự do.
// Mô tả tiêu chí là lời tự viết (không chép rubric của trường nào).

export const CITATION_OPTIONS: { value: CitationStyle; label: string }[] = [
  { value: 'IEEE', label: 'IEEE — đánh số [1], [2] (ngành IT)' },
  { value: 'APA', label: 'APA — (Tác giả, năm)' }
];

const section = (title: string, required = true, maxWords: number | null = null): SectionForm => ({
  sectionKey: null,
  title,
  required,
  minWords: null,
  maxWords
});

export function newSection(): SectionForm {
  return section('', true);
}

/** Một giai đoạn của bài paper: phần loại "Tự do", không chế độ thi, có khung mục + rubric mẫu. */
export function newPaperStage(title: string, sections: SectionForm[], prompt: string): PartForm {
  return {
    title,
    taskType: 'CUSTOM',
    promptText: prompt,
    imageUrl: null,
    minWords: null,
    suggestedMinutes: null,
    partDueAt: null,
    weight: null,
    criteria: paperCriteria(),
    examMode: false,
    examDurationMinutes: null,
    sections
  };
}

/** Đề cương → Bản cuối (thường thấy ở môn học / NCKH sinh viên). */
export function defaultPaperStages(): PartForm[] {
  return [
    newPaperStage(
      'Đề cương',
      [
        section('Introduction', true, 800),
        section('Related Work', true, 1000),
        section('Phương pháp dự kiến'),
        section('Kế hoạch thực hiện', false, 400)
      ],
      'Nêu vấn đề, đóng góp dự kiến, các nghiên cứu liên quan chính và phương pháp dự định dùng.'
    ),
    newPaperStage(
      'Bản cuối',
      [
        section('Abstract', true, 250),
        section('Introduction'),
        section('Related Work'),
        section('Method'),
        section('Experiments & Results'),
        section('Discussion', false),
        section('Conclusion')
      ],
      'Bài hoàn chỉnh theo định dạng bài báo. Danh mục tài liệu tham khảo được hệ thống sinh tự động từ các trích dẫn.'
    )
  ];
}

const criterion = (name: string, weight: number, description: string): CriterionForm => ({
  code: null,
  name,
  description,
  maxScore: 10,
  step: 0.25,
  weight,
  checklist: []
});

/** Rubric mẫu paper IT: trọng số dồn vào nội dung khoa học, định dạng chiếm ít (docs/12 mục 5.4). */
export function paperCriteria(): CriterionForm[] {
  return [
    criterion(
      'Vấn đề & đóng góp',
      1.5,
      'Vấn đề rõ, có động lực; đóng góp nêu cụ thể, đo được (Abstract, Introduction).'
    ),
    criterion('Nghiên cứu liên quan', 1.5, 'So sánh có phân tích với các hướng chính, chỉ ra khoảng trống bài lấp.'),
    criterion('Phương pháp / thiết kế', 2.5, 'Mô tả đủ để người khác làm lại được; lựa chọn có lý do.'),
    criterion('Kết quả & đánh giá', 2, 'Thí nghiệm hợp lý, có so sánh, bảng / hình rõ, nêu hạn chế.'),
    criterion('Cấu trúc & văn phong học thuật', 1.5, 'Mạch lạc, đúng khung, văn phong khách quan, ít lỗi.'),
    criterion('Trích dẫn & định dạng', 1, 'Ý của người khác đều có trích dẫn; danh mục đúng chuẩn IEEE / APA.')
  ];
}

/** Tiêu chí trống cho bài paper (thang 10, bước 0,25). */
export function newPaperCriterion(): CriterionForm {
  return criterion('', 1, '');
}
