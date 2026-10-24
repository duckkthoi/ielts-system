// NFR-01 — 200 sinh viên nộp bài trong 5 phút cuối trước hạn: p95 < 500 ms, lỗi < 1%.
//
// Mỗi VU là một sinh viên của lớp kiểm thử tải: đăng nhập trong 30 giây đầu, mở bài, rồi vừa gõ vừa tự lưu nháp
// mỗi 3–5 giây (FR-32, có draftVersion — SD-06) cho tới thời điểm nộp ngẫu nhiên trong phút 1 → 4.8, nộp
// (SD-07) và xem lại lịch sử nộp. Cùng lúc một giáo viên mở ma trận tiến độ mỗi 5 giây (truy vấn nặng nhất).
//
// Chạy (xem docs/09-kiem-thu-tai.md):
//   docker compose -f docker-compose.yml -f loadtest/compose.loadtest.yml run --rm k6 run submit-rush.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, TEACHER, apiOk, auth, login, loadStudent, prepareAssignment } from './lib/api.js';

const STUDENTS = parseInt(__ENV.STUDENTS || '200', 10);
const WINDOW_S = 300;

const submitted = new Counter('submitted');
const submitTime = new Trend('submit_duration', true);
const draftTime = new Trend('draft_duration', true);

export const options = {
	insecureSkipTLSVerify: true, // chứng chỉ tự ký của nginx (deploy/nginx/gen-cert.sh)
	setupTimeout: '60s',
	scenarios: {
		students: {
			executor: 'per-vu-iterations', vus: STUDENTS, iterations: 1, maxDuration: '6m', exec: 'student',
		},
		teacher: {
			executor: 'constant-vus', vus: 1, duration: `${WINDOW_S}s`, exec: 'teacher',
		},
	},
	thresholds: {
		'http_req_duration{scenario:students}': ['p(95)<500'],
		http_req_failed: ['rate<0.01'],
		checks: ['rate>0.99'],
		submitted: [`count>=${STUDENTS}`],
	},
	summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
	const token = login(TEACHER);
	// Hạn sau 6 phút: toàn bộ lượt nộp rơi vào "5 phút cuối".
	const a = prepareAssignment(token, `Kiểm thử tải — nộp sát hạn ${new Date().toISOString()}`, 6);
	return Object.assign({ teacherToken: token, startedAt: Date.now() }, a);
}

const SENTENCES = [
	'Working from home has become increasingly common in recent years.',
	'One clear advantage is that employees save a considerable amount of time on commuting.',
	'This time can be spent on family, exercise or further study.',
	'However, staying at home all day may leave some workers feeling isolated from their colleagues.',
	'Moreover, it can be difficult to separate working hours from personal life.',
	'Companies, on the other hand, can reduce the cost of renting large offices.',
	'In my opinion, the benefits outweigh the drawbacks if workers manage their time carefully.',
];

export function student(data) {
	sleep(Math.random() * 30);
	// Số thứ tự trong scenario, không dùng __VU (đánh chung cho mọi scenario).
	const token = login(loadStudent(exec.scenario.iterationInTest + 1));
	if (!token) {
		return;
	}
	const submitAt = data.startedAt + (60 + Math.random() * 228) * 1000;
	const partId = data.partId;
	const list = http.get(`${BASE_URL}/api/student/assignments?tab=TODO`, Object.assign(auth(token), { tags: { name: 'list' } }));
	check(list, { 'danh sách bài thành công': apiOk });
	const detail = http.get(`${BASE_URL}/api/student/assignments/${data.assignmentId}`,
		Object.assign(auth(token), { tags: { name: 'detail' } }));
	check(detail, { 'chi tiết bài thành công': apiOk });

	const draft = http.get(`${BASE_URL}/api/student/parts/${partId}/draft`, Object.assign(auth(token), { tags: { name: 'draft-get' } }));
	check(draft, { 'nạp nháp thành công': apiOk });
	let draftVersion = draft.json('data.draftVersion');
	let content = '';
	let i = 0;
	while (Date.now() < submitAt) {
		sleep(3 + Math.random() * 2);
		content += (content ? ' ' : '') + SENTENCES[i++ % SENTENCES.length];
		const res = http.put(`${BASE_URL}/api/student/parts/${partId}/draft`, JSON.stringify({ content, draftVersion }),
			Object.assign(auth(token), { tags: { name: 'draft-save' } }));
		draftTime.add(res.timings.duration);
		if (check(res, { 'lưu nháp thành công': apiOk })) {
			draftVersion = res.json('data.draftVersion');
		}
	}

	const res = http.post(`${BASE_URL}/api/student/parts/${partId}/submit`, JSON.stringify({ content, expectedVersion: 0 }),
		Object.assign(auth(token), { tags: { name: 'submit' } }));
	submitTime.add(res.timings.duration);
	if (check(res, { 'nộp bài thành công': apiOk, 'không bị tính muộn': (r) => r.json('data.late') === false })) {
		submitted.add(1);
	}
	const versions = http.get(`${BASE_URL}/api/student/parts/${partId}/versions`, Object.assign(auth(token), { tags: { name: 'versions' } }));
	check(versions, { 'lịch sử nộp thành công': apiOk });
}

export function teacher(data) {
	const res = http.get(`${BASE_URL}/api/teacher/assignments/${data.assignmentId}/progress`,
		Object.assign(auth(data.teacherToken), { tags: { name: 'progress' } }));
	check(res, { 'ma trận tiến độ thành công': apiOk });
	sleep(5);
}
