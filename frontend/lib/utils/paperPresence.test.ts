// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { applyPresence, decodePresence, encodePresence, onlinePeers, PRESENCE_TIMEOUT_MS } from './paperPresence.ts';

test('encode / decode: giữ mục đang soạn, state lạ bị bỏ qua', () => {
  assert.deepEqual(decodePresence(encodePresence(true, 'gioi-thieu')), { v: 1, active: true, section: 'gioi-thieu' });
  assert.equal(decodePresence('không phải json'), null);
  assert.equal(decodePresence('{"cursor":3}'), null);
});

test('applyPresence: nhịp mới ghi đè, active = false thì xoá', () => {
  let peers = applyPresence({}, { userId: 1, userName: 'An', state: encodePresence(true, null) }, 1_000);
  peers = applyPresence(peers, { userId: 1, userName: 'An', state: encodePresence(true, 'ket-luan') }, 2_000);
  assert.equal(peers[1].section, 'ket-luan');
  assert.equal(peers[1].lastSeen, 2_000);
  peers = applyPresence(peers, { userId: 1, userName: 'An', state: encodePresence(false, null) }, 3_000);
  assert.deepEqual(peers, {});
});

test('onlinePeers: quá 40 giây không nhịp thì coi như đã rời; xếp theo tên', () => {
  const peers = {
    1: { userId: 1, userName: 'Bình', section: null, lastSeen: 0 },
    2: { userId: 2, userName: 'An', section: null, lastSeen: 0 },
    3: { userId: 3, userName: 'Cường', section: null, lastSeen: -1 }
  };
  assert.deepEqual(
    onlinePeers(peers, PRESENCE_TIMEOUT_MS).map((p) => p.userName),
    ['An', 'Bình']
  );
});
