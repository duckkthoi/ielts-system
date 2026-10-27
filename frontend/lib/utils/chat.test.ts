// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import type { ChannelSummary, ChatMessageEvent } from '@/lib/types/chat';

import {
  applyIncomingToChannels,
  mergeMessages,
  normalizeMessage,
  seenBy,
  setLocalStatus,
  upsertReader
} from './chat.ts';
import type { ChatLine } from './chat.ts';

const ME = 1;

const msg = (id: number, senderId = 2, clientMsgId: string | null = null, channelId = 'class-3'): ChatMessageEvent => ({
  type: 'MESSAGE',
  id,
  channelId,
  senderId,
  senderName: senderId === ME ? 'Cô Hạnh' : 'An',
  content: `tin ${id}`,
  clientMsgId,
  createdAt: '2026-11-05T10:00:00Z'
});

const pending = (clientMsgId: string): ChatLine => ({
  key: `c-${clientMsgId}`,
  id: null,
  senderId: ME,
  senderName: 'Cô Hạnh',
  content: 'đang gửi',
  clientMsgId,
  createdAt: '2026-11-05T10:01:00Z',
  local: 'sending'
});

test('mergeMessages: bỏ trùng theo id, sắp tăng dần, tin chờ xác nhận ở cuối', () => {
  const lines = mergeMessages([pending('a')], [msg(5), msg(3)], ME);
  const again = mergeMessages(lines, [msg(5), msg(4)], ME);
  assert.deepEqual(
    again.map((l) => l.id),
    [3, 4, 5, null]
  );
});

test('mergeMessages: server phát lại tin của mình → thay tin đang gửi cùng clientMsgId', () => {
  const lines = mergeMessages([pending('a'), pending('b')], [msg(9, ME, 'a')], ME);
  assert.deepEqual(
    lines.map((l) => [l.id, l.clientMsgId, l.local]),
    [
      [9, 'a', null],
      [null, 'b', 'sending']
    ]
  );
});

test('mergeMessages: tin của người khác trùng clientMsgId không xoá tin của mình', () => {
  const lines = mergeMessages([pending('a')], [msg(9, 2, 'a')], ME);
  assert.equal(lines.length, 2);
});

test('setLocalStatus: chỉ đổi tin chưa xác nhận', () => {
  const lines = setLocalStatus([pending('a'), pending('b')], 'b', 'failed');
  assert.deepEqual(
    lines.map((l) => l.local),
    ['sending', 'failed']
  );
});

test('normalizeMessage: bỏ khoảng trắng hai đầu; rỗng hoặc quá 4000 ký tự → null', () => {
  assert.equal(normalizeMessage('  chào cô  '), 'chào cô');
  assert.equal(normalizeMessage('   \n '), null);
  assert.equal(normalizeMessage('a'.repeat(4001)), null);
  assert.equal(normalizeMessage('a'.repeat(4000))?.length, 4000);
});

const channel = (channelId: string, lastId: number | null, unread = 0): ChannelSummary => ({
  channelId,
  type: channelId.startsWith('class') ? 'CLASSROOM' : 'SUBMISSION',
  title: channelId,
  lastMessage:
    lastId === null ? null : { id: lastId, senderName: 'An', preview: '…', createdAt: '2026-11-05T09:00:00Z' },
  unreadCount: unread
});

test('applyIncomingToChannels: tin người khác ở kênh không mở → tăng chưa đọc, đưa lên đầu', () => {
  const next = applyIncomingToChannels([channel('submission-1', 8), channel('class-3', 2)], msg(10), ME, null);
  assert.deepEqual(
    next?.map((c) => [c.channelId, c.unreadCount, c.lastMessage?.id]),
    [
      ['class-3', 1, 10],
      ['submission-1', 0, 8]
    ]
  );
});

test('applyIncomingToChannels: kênh đang mở / tin của mình không tăng; tin cũ bỏ qua; kênh lạ → null', () => {
  const list = [channel('class-3', 2)];
  assert.equal(applyIncomingToChannels(list, msg(10), ME, 'class-3')?.[0].unreadCount, 0);
  assert.equal(applyIncomingToChannels(list, msg(10, ME), ME, null)?.[0].unreadCount, 0);
  assert.equal(applyIncomingToChannels(list, msg(1), ME, null), list);
  assert.equal(applyIncomingToChannels(list, msg(10, 2, null, 'class-9'), ME, null), null);
});

test('upsertReader / seenBy: mốc đã đọc chỉ tăng; "đã xem" bỏ chính mình', () => {
  let reads = upsertReader([], { userId: 2, userName: 'An', lastMessageId: 5 });
  reads = upsertReader(reads, { userId: 2, userName: '', lastMessageId: 3 });
  assert.equal(reads[0].lastMessageId, 5);
  reads = upsertReader(reads, { userId: 2, userName: '', lastMessageId: 7 });
  assert.deepEqual(reads, [{ userId: 2, userName: 'An', lastMessageId: 7 }]);
  reads = upsertReader(reads, { userId: ME, userName: 'Cô Hạnh', lastMessageId: 9 });
  assert.deepEqual(seenBy(reads, 6, ME), ['An']);
  assert.deepEqual(seenBy(reads, 8, ME), []);
});
