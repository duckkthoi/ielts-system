// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
  bibliographyEntries,
  citationLabels,
  citationOrder,
  cslFromForm,
  formatReference,
  formFromCsl,
  mergeForm,
  missingText,
  parseBibtex
} from './citation.ts';
import type { ReferenceForm } from './citation.ts';

const BIB = String.raw`@article{lecun2015,
  title = {Deep learning},
  author = {LeCun, Yann and Bengio, Yoshua and Hinton, Geoffrey},
  journal = {Nature}, volume = {521}, number = {7553}, pages = {436--444}, year = {2015},
  doi = {10.1038/nature14539}
}
@inproceedings{nguyen2021,
  title = {H{\"o}c m{\'a}y cho ti{\'\^e}ng Vi{\d{\^e}}t},
  author = {Nguy{\~{\^e}}n, V{\u{a}}n An},
  booktitle = {Proc. RIVF}, year = {2021}
}`;

test('parseBibtex: đọc nhiều mục ra CSL, đổi mã LaTeX thành chữ có dấu, bỏ khoá + dữ liệu nội bộ', () => {
  const items = parseBibtex(BIB);
  assert.equal(items.length, 2);
  assert.equal(items[0].type, 'article-journal');
  assert.equal(items[0]['container-title'], 'Nature');
  assert.equal(items[0].DOI, '10.1038/nature14539');
  assert.deepEqual(items[0].issued, { 'date-parts': [[2015]] });
  assert.equal(items[1].type, 'paper-conference');
  assert.equal(items[1].author?.[0].family, 'Nguyễn');
  assert.equal(items[1].author?.[0].given, 'Văn An');
  for (const it of items) {
    assert.equal('_graph' in it, false);
    assert.equal('id' in it, false);
    assert.equal('citation-key' in it, false);
  }
});

test('parseBibtex: không có mục nào / sai ngoặc → lỗi tiếng Việt', () => {
  assert.throws(() => parseBibtex('hello world'), /Không tìm thấy mục nào/);
  assert.throws(() => parseBibtex('@misc{x, title={abc'), /Không đọc được BibTeX|Không tìm thấy mục nào/);
});

test('formatReference: IEEE (bỏ số thứ tự) và APA', () => {
  const [lecun] = parseBibtex(BIB);
  assert.equal(
    formatReference(lecun, 'IEEE'),
    'Y. LeCun, Y. Bengio, and G. Hinton, “Deep learning,” Nature, vol. 521, no. 7553, pp. 436–444, 2015, doi: 10.1038/nature14539.'
  );
  assert.equal(
    formatReference(lecun, 'APA'),
    'LeCun, Y., Bengio, Y., & Hinton, G. (2015). Deep learning. Nature, 521(7553), 436–444. https://doi.org/10.1038/nature14539'
  );
  assert.equal(formatReference({ type: 'webpage', title: 'Chỉ có tiêu đề' }, 'APA'), 'Chỉ có tiêu đề. (n.d.).');
});

const form: ReferenceForm = {
  type: 'book',
  title: ' Clean Code ',
  authors: 'Martin, Robert C.\n\nW3C',
  year: '2008',
  containerTitle: '',
  volume: '',
  issue: '',
  page: '',
  publisher: 'Prentice Hall',
  url: '',
  doi: ''
};

test('cslFromForm: "Họ, Tên" mỗi dòng, dòng không có dấu phẩy giữ nguyên; năm → issued; bỏ trường trống', () => {
  assert.deepEqual(cslFromForm(form), {
    type: 'book',
    title: 'Clean Code',
    author: [{ family: 'Martin', given: 'Robert C.' }, { literal: 'W3C' }],
    issued: { 'date-parts': [[2008]] },
    publisher: 'Prentice Hall'
  });
});

test('formFromCsl → cslFromForm giữ nguyên; mergeForm giữ trường form không quản lý (biên tập viên, ISSN)', () => {
  const csl = cslFromForm(form);
  assert.deepEqual(cslFromForm(formFromCsl(csl)), csl);
  const merged = mergeForm({ ...csl, editor: [{ family: 'X' }], ISSN: ['1'] }, { ...formFromCsl(csl), year: '' });
  assert.deepEqual(merged.editor, [{ family: 'X' }]);
  assert.deepEqual(merged.ISSN, ['1']);
  assert.equal('issued' in merged, false);
});

test('missingText: liệt kê trường thiếu bằng tiếng Việt (BR-30)', () => {
  assert.equal(missingText([]), '');
  assert.equal(missingText(['AUTHOR', 'YEAR']), 'Thiếu tác giả, năm');
});

// ─── Trích dẫn trong bài + danh mục ───

const LIB = new Map(
  parseBibtex(String.raw`@article{a, title={Deep learning}, author={LeCun, Yann and Bengio, Yoshua and Hinton, Geoffrey}, journal={Nature}, year={2015}}
@inproceedings{b, title={Attention is all you need}, author={Vaswani, Ashish and Shazeer, Noam}, booktitle={NeurIPS}, year={2017}}
@book{c, title={Clean code}, author={Martin, Robert C.}, publisher={Prentice Hall}, year={2008}}
@misc{d, title={Pro Git}, author={Chacon, Scott}, year={2014}, url={https://git-scm.com/book}}`).map((csl, i) => [
    [10, 20, 30, 40][i],
    csl
  ])
);

test('citationLabels IEEE: số theo thứ tự xuất hiện lần đầu, gộp từ 3 số liên tiếp, tài liệu đã xoá → [?]', () => {
  const seq = [[20], [10, 20], [30, 10, 20], [40, 99], [20]];
  assert.deepEqual(citationLabels(seq, LIB, 'IEEE'), ['[1]', '[1], [2]', '[1]–[3]', '[4], [?]', '[1]']);
  assert.deepEqual(citationOrder(seq, LIB), [20, 10, 30, 40]);
});

test('citationLabels APA: (Tác giả, năm); thiếu tài liệu → ?', () => {
  assert.deepEqual(citationLabels([[10], [20, 30], [99], [30, 99]], LIB, 'APA'), [
    '(LeCun et al., 2015)',
    '(Martin, 2008; Vaswani & Shazeer, 2017)',
    '(?)',
    '(Martin, 2008; ?)'
  ]);
});

test('bibliographyEntries: chỉ tài liệu được trích; IEEE theo số, APA theo ABC; chưa trích gì → rỗng', () => {
  const seq = [[20], [10], [20, 99]];
  assert.deepEqual(
    bibliographyEntries(seq, LIB, 'IEEE').map((e) => [e.id, e.text.slice(0, 20)]),
    [
      [20, '[1] A. Vaswani and N'],
      [10, '[2] Y. LeCun, Y. Ben']
    ]
  );
  assert.deepEqual(
    bibliographyEntries(seq, LIB, 'APA').map((e) => e.id),
    [10, 20]
  );
  assert.deepEqual(bibliographyEntries([], LIB, 'APA'), []);
});
