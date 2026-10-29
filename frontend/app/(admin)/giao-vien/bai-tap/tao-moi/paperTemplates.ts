import type { CitationStyle } from '@/lib/types/assignment';
import { defaultLevels } from '@/lib/utils/rubricLevels';

import type { CriterionForm, LevelForm, PartForm, SectionForm } from './type';

// ─── Mẫu cho bài loại Paper (docs/12) ─────────────────────────────────────────
// Khung mục theo IMRaD của bài báo ngành IT; rubric mẫu 6 tiêu chí thang 10, mỗi tiêu chí 4 mức đạt (docs/12 mục 5.4).
// Giáo viên sửa tự do. Mô tả tiêu chí / mức là lời tự viết (không chép rubric của trường nào).

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

/** 4 mức của thang 10 bước 0,25: mô tả theo thứ tự Xuất sắc → Chưa đạt (trống = giáo viên tự viết). */
function levels(descriptors: [string, string, string, string] = ['', '', '', '']): LevelForm[] {
  return defaultLevels(10, 0.25).map((l, i) => ({ ...l, descriptor: descriptors[i] }));
}

const criterion = (
  name: string,
  weight: number,
  description: string,
  descriptors?: [string, string, string, string]
): CriterionForm => ({
  code: null,
  name,
  description,
  maxScore: 10,
  step: 0.25,
  weight,
  checklist: [],
  levels: levels(descriptors)
});

/** Rubric mẫu paper IT: trọng số dồn vào nội dung khoa học, định dạng chiếm ít (docs/12 mục 5.4). */
export function paperCriteria(): CriterionForm[] {
  return [
    criterion(
      'Vấn đề & đóng góp',
      1.5,
      'Vấn đề rõ, có động lực; đóng góp nêu cụ thể, đo được (Abstract, Introduction).',
      [
        'Vấn đề rõ, có động lực; đóng góp nêu cụ thể, đo được',
        'Vấn đề rõ, đóng góp còn chung chung',
        'Vấn đề mơ hồ, đóng góp khó nhận ra',
        'Không nêu được vấn đề / đóng góp'
      ]
    ),
    criterion('Nghiên cứu liên quan', 1.5, 'So sánh có phân tích với các hướng chính, chỉ ra khoảng trống bài lấp.', [
      'So sánh có phân tích với các hướng chính, chỉ ra khoảng trống mà bài lấp',
      'Đủ nguồn, có so sánh nhưng nông',
      'Liệt kê nguồn, không so sánh',
      'Rất ít hoặc không có nguồn'
    ]),
    criterion('Phương pháp / thiết kế', 2.5, 'Mô tả đủ để người khác làm lại được; lựa chọn có lý do.', [
      'Mô tả đủ để người khác làm lại được; lựa chọn có lý do',
      'Đủ ý chính, thiếu vài chi tiết',
      'Thiếu nhiều chi tiết, khó làm lại',
      'Không mô tả được cách làm'
    ]),
    criterion('Kết quả & đánh giá', 2, 'Thí nghiệm hợp lý, có so sánh, bảng / hình rõ, nêu hạn chế.', [
      'Thí nghiệm hợp lý, có so sánh với phương pháp khác, bảng / hình rõ, có phân tích hạn chế',
      'Có thí nghiệm và số liệu, phân tích còn mỏng',
      'Số liệu ít, không so sánh',
      'Không có kết quả kiểm chứng'
    ]),
    criterion('Cấu trúc & văn phong học thuật', 1.5, 'Mạch lạc, đúng khung, văn phong khách quan, ít lỗi.', [
      'Mạch lạc, đúng khung, văn phong khách quan, gần như không lỗi',
      'Mạch lạc, ít lỗi',
      'Rời rạc ở vài chỗ, nhiều lỗi',
      'Khó đọc, sai khung'
    ]),
    criterion('Trích dẫn & định dạng', 1, 'Ý của người khác đều có trích dẫn; danh mục đúng chuẩn IEEE / APA.', [
      'Mọi ý của người khác đều có trích dẫn, danh mục đúng chuẩn',
      'Vài lỗi định dạng nhỏ',
      'Thiếu trích dẫn ở vài chỗ',
      'Không trích dẫn / có dấu hiệu đạo văn'
    ])
  ];
}

/** Tiêu chí trống cho bài paper (thang 10, bước 0,25). */
export function newPaperCriterion(): CriterionForm {
  return criterion('', 1, '');
}
