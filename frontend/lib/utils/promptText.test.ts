// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { htmlToText, textToHtml } from './promptText.ts';

test('textToHtml: dòng trống tách đoạn, xuống dòng thành <br>, escape ký tự HTML', () => {
  assert.equal(
    textToHtml('The graph below shows <data>.\nSummarise it.\n\n  Write at least 150 words & compare.  '),
    '<p>The graph below shows &lt;data&gt;.<br>Summarise it.</p><p>Write at least 150 words &amp; compare.</p>'
  );
  assert.equal(textToHtml('\n\n  \n'), '');
});

test('htmlToText: đoạn, <br>, danh sách và entity', () => {
  assert.equal(
    htmlToText('<p>Line one<br/>line two</p><p>A &amp; B &lt;ok&gt;</p><ul><li>x</li><li>y</li></ul>'),
    'Line one\nline two\n\nA & B <ok>\n\n- x\n- y'
  );
});

test('textToHtml ∘ htmlToText giữ nguyên văn bản', () => {
  const text = 'Some people think "free" education\nis best.\n\nTo what extent do you agree?';
  assert.equal(htmlToText(textToHtml(text)), text);
});
