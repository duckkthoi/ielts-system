// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { filenameFromDisposition } from './download.ts';

test('filenameFromDisposition: ưu tiên filename* UTF-8 (tên tiếng Việt)', () => {
  const header =
    'attachment; filename="=?UTF-8?Q?t=C3=A0i?=.xlsx"; filename*=UTF-8\'\'t%C3%A0i-kho%E1%BA%A3n-DEMO-A.xlsx';
  assert.equal(filenameFromDisposition(header), 'tài-khoản-DEMO-A.xlsx');
});

test('filenameFromDisposition: dạng filename="…" thường', () => {
  assert.equal(
    filenameFromDisposition('attachment; filename="mau-danh-sach-sinh-vien.csv"'),
    'mau-danh-sach-sinh-vien.csv'
  );
  assert.equal(filenameFromDisposition('attachment; filename=bang-diem.xlsx'), 'bang-diem.xlsx');
});

test('filenameFromDisposition: không có header / không có tên → null', () => {
  assert.equal(filenameFromDisposition(null), null);
  assert.equal(filenameFromDisposition('inline'), null);
});
