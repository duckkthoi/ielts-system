// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import type { LiveComment } from '@/lib/types/paper';
import { applyCommentPush, isAnchored, openCount } from './liveComments.ts';

const base: LiveComment = {
  id: 1,
  documentId: 9,
  authorId: 2,
  authorName: 'GV',
  startOffset: 4,
  endOffset: 9,
  quote: 'quick',
  comment: 'x',
  kind: 'COMMENT',
  suggestion: null,
  sectionKey: null,
  resolvedAt: null,
  resolvedByName: null,
  createdAt: '2026-10-31T00:00:00Z',
  updatedAt: '2026-10-31T00:00:00Z'
};

test('isAnchored: đoạn tại offset còn đúng quote mới tô; chữ dịch đi thì không gắn sang chỗ khác', () => {
  assert.equal(isAnchored('The quick fox', base), true);
  assert.equal(isAnchored('A The quick fox', base), false);
  assert.equal(isAnchored('The', base), false);
});

test('applyCommentPush: thêm / sửa thay tại chỗ theo vị trí, xoá bỏ đi', () => {
  const second = { ...base, id: 2, startOffset: 0 };
  let list = applyCommentPush([], { type: 'CREATED', comment: base, commentId: 1 });
  list = applyCommentPush(list, { type: 'CREATED', comment: second, commentId: 2 });
  assert.deepEqual(
    list.map((c) => c.id),
    [2, 1]
  );
  list = applyCommentPush(list, {
    type: 'RESOLVED',
    comment: { ...base, resolvedAt: '2026-10-31T01:00:00Z' },
    commentId: 1
  });
  assert.equal(openCount(list), 1);
  list = applyCommentPush(list, { type: 'DELETED', comment: null, commentId: 2 });
  assert.deepEqual(
    list.map((c) => c.id),
    [1]
  );
});
