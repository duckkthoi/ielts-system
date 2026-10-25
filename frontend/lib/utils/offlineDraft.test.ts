// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { decideRestore, isNetworkFailure, offlineDraftKey } from './offlineDraft.ts';

const local = (content: string, baseDraftVersion: number) => ({ content, baseDraftVersion, updatedAt: 0 });

test('decideRestore: không có bản trên máy hoặc trùng bản server → none', () => {
  assert.equal(decideRestore(null, { content: 'a', draftVersion: 3 }), 'none');
  assert.equal(decideRestore(local('a', 1), { content: 'a', draftVersion: 3 }), 'none');
});

test('decideRestore: server không đổi từ lúc sửa offline → restore (lưu bản trên máy lên)', () => {
  assert.equal(decideRestore(local('viết thêm khi mất mạng', 3), { content: 'cũ', draftVersion: 3 }), 'restore');
});

test('decideRestore: server đã có bản mới hơn (nơi khác lưu) → conflict, hỏi chọn bản', () => {
  assert.equal(decideRestore(local('bản trên máy', 3), { content: 'bản nơi khác', draftVersion: 4 }), 'conflict');
});

test('isNetworkFailure: không có phản hồi / 502–504 là mất mạng; lỗi nghiệp vụ thì không', () => {
  assert.equal(isNetworkFailure(undefined), true);
  assert.equal(isNetworkFailure(502), true);
  assert.equal(isNetworkFailure(504), true);
  assert.equal(isNetworkFailure(409), false);
  assert.equal(isNetworkFailure(422), false);
  assert.equal(isNetworkFailure(500), false);
});

test('offlineDraftKey: theo người dùng và phần', () => {
  assert.equal(offlineDraftKey(42, 7), '42:7');
});
