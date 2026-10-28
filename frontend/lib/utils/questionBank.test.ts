// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { partToQuestionBank, questionBankToAssignment, questionBankToPart } from './questionBank.ts';

test('questionBankToPart: sao chép đề/rubric và bỏ id server', () => {
  const part = questionBankToPart({
    id: 8,
    title: 'Opinion essay',
    taskType: 'WRITING_TASK2',
    prompt: '<p>Discuss both views.</p>',
    imageUrl: null,
    minWords: 250,
    suggestedMinutes: 40,
    tags: ['opinion'],
    createdAt: '2026-10-28T00:00:00Z',
    updatedAt: '2026-10-28T00:00:00Z',
    criteria: [
      {
        id: 11,
        code: 'TR',
        name: 'Task Response',
        description: null,
        maxScore: 9,
        step: 1,
        weight: 1,
        orderNo: 1,
        checklist: [{ id: 12, content: 'Đủ hai quan điểm', kind: 'CHECK', maxBand: null, orderNo: 1 }]
      }
    ]
  });

  assert.equal(part.promptText, 'Discuss both views.');
  assert.deepEqual(part.criteria[0], {
    code: 'TR',
    name: 'Task Response',
    description: null,
    maxScore: 9,
    step: 1,
    weight: 1,
    checklist: [{ content: 'Đủ hai quan điểm', kind: 'CHECK', maxBand: null }]
  });
  assert.equal('id' in part.criteria[0], false);
});

test('questionBankToAssignment: tạo bài một phần để giao trực tiếp', () => {
  const request = questionBankToAssignment(
    {
      id: 9,
      title: 'Line graph – energy use',
      taskType: 'WRITING_TASK1',
      prompt: '<p>The graph shows energy use.</p>',
      imageUrl: '/uploads/graph.png',
      minWords: 150,
      suggestedMinutes: 20,
      tags: ['line graph'],
      criteria: [
        {
          id: 4,
          code: 'TA',
          name: 'Task Achievement',
          description: null,
          maxScore: 9,
          step: 1,
          weight: 1,
          orderNo: 0,
          checklist: []
        }
      ],
      createdAt: '2026-10-28T00:00:00Z',
      updatedAt: '2026-10-28T00:00:00Z'
    },
    {
      title: 'Bài luyện Line graph',
      openAt: null,
      dueAt: '2026-11-01T16:59:00.000Z',
      allowLate: true,
      maxRevisions: 2,
      showSuggestionsOnRevision: false
    }
  );

  assert.equal(request.title, 'Bài luyện Line graph');
  assert.equal(request.parts.length, 1);
  assert.equal(request.parts[0].prompt, '<p>The graph shows energy use.</p>');
  assert.equal(request.parts[0].criteria[0].code, 'TA');
  assert.equal(request.parts[0].partDueAt, null);
});

test('partToQuestionBank: chỉ lưu dữ liệu dùng lại và chuẩn hoá checklist', () => {
  const request = partToQuestionBank(
    {
      title: 'Task 2',
      taskType: 'WRITING_TASK2',
      promptText: 'Dòng 1\n\nDòng 2',
      imageUrl: '',
      minWords: 250,
      suggestedMinutes: 40,
      criteria: [
        {
          code: 'TR',
          name: ' Task Response ',
          description: ' ',
          maxScore: 9,
          step: 1,
          weight: 1,
          checklist: [
            { content: ' Có luận điểm ', kind: 'CHECK', maxBand: 5 },
            { content: ' Thiếu kết luận ', kind: 'LIMIT', maxBand: 6 }
          ]
        }
      ]
    },
    ' Đề mẫu ',
    [' opinion ', '', 'academic']
  );

  assert.equal(request.title, 'Đề mẫu');
  assert.equal(request.prompt, '<p>Dòng 1</p><p>Dòng 2</p>');
  assert.equal(request.imageUrl, null);
  assert.deepEqual(request.tags, ['opinion', 'academic']);
  assert.deepEqual(request.criteria[0].checklist, [
    { content: 'Có luận điểm', kind: 'CHECK', maxBand: null },
    { content: 'Thiếu kết luận', kind: 'LIMIT', maxBand: 6 }
  ]);
});
