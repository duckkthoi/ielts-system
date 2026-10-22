// Chạy: npm run test:unit  (node --test, Node ≥ 23 chạy được TypeScript trực tiếp)
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

import { countWords } from './wordCount.ts';

interface Case {
  name: string;
  text: string;
  count: number;
}

const { cases } = JSON.parse(
  readFileSync(new URL('../../../shared/word-count-cases.json', import.meta.url), 'utf8')
) as { cases: Case[] };

for (const c of cases) {
  test(`BR-20 đếm từ: ${c.name}`, () => {
    assert.equal(countWords(c.text), c.count);
  });
}
