// Hàm dùng chung cho các kịch bản k6 (bước 14). Tài khoản lấy từ dữ liệu mẫu của profile demo:
// giáo viên gv.<prefix>, lớp <PREFIX>-LOAD, sinh viên <PREFIX>-T0001… (xem DemoSeeder).
import http from 'k6/http';
import { check, fail } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'https://localhost';
export const WS_URL = __ENV.WS_URL || 'wss://localhost/ws';
export const PREFIX = (__ENV.DEMO_PREFIX || 'DEMO').toUpperCase();
export const PASSWORD = __ENV.DEMO_PASSWORD || 'Demo@2026';

export const TEACHER = 'gv.' + PREFIX.toLowerCase();

/** Tên đăng nhập của sinh viên thứ n (bắt đầu từ 1) trong lớp kiểm thử tải. */
export function loadStudent(n) {
	return `${PREFIX}-T${('000' + n).slice(-4)}`;
}

export function auth(token, extra) {
	return { headers: Object.assign({ Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, extra || {}) };
}

/** Đăng nhập → access token; lỗi thì trả null (lượt lỗi đã được tính vào http_req_failed / checks). */
export function login(username) {
	const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ username, password: PASSWORD }), {
		headers: { 'Content-Type': 'application/json' },
		tags: { name: 'login' },
	});
	const ok = check(res, { 'đăng nhập 200': (r) => r.status === 200 });
	return ok ? res.json('data.accessToken') : null;
}

function data(res, what) {
	if (res.status < 200 || res.status >= 300) {
		fail(`${what}: HTTP ${res.status} ${res.body}`);
	}
	return res.json('data');
}

/**
 * Giáo viên tạo bài Task 2 (rubric IELTS mẫu của hệ thống) hạn sau dueMinutes phút và giao cho lớp kiểm thử tải.
 * Dùng trong setup(): mỗi lần chạy một bài mới nên sinh viên luôn còn phần chưa nộp.
 */
export function prepareAssignment(token, title, dueMinutes) {
	const templates = data(http.get(`${BASE_URL}/api/teacher/rubric-templates?taskType=WRITING_TASK2`, auth(token)),
		'rubric mẫu');
	const template = templates.find((t) => t.system);
	const criteria = template.criteria.map((c) => ({
		code: c.code, name: c.name, description: c.description, maxScore: c.maxScore, step: c.step, weight: c.weight,
		checklist: c.checklist.map((i) => ({ content: i.content, kind: i.kind, maxBand: i.maxBand })),
	}));

	const classes = data(http.get(`${BASE_URL}/api/teacher/classes?keyword=${PREFIX}-LOAD`, auth(token)), 'lớp');
	const cls = classes.find((c) => c.code === `${PREFIX}-LOAD`);
	if (!cls) {
		fail(`Không có lớp ${PREFIX}-LOAD — backend phải chạy profile demo với DEMO_LOAD_STUDENTS > 0`);
	}

	const body = {
		title,
		dueAt: new Date(Date.now() + dueMinutes * 60000).toISOString(),
		allowLate: true,
		parts: [{
			title: 'Task 2', taskType: 'WRITING_TASK2', minWords: 250, suggestedMinutes: 40, criteria,
			prompt: '<p>Some people prefer to work from home. Discuss the advantages and disadvantages.</p>',
		}],
	};
	const created = data(http.post(`${BASE_URL}/api/teacher/assignments`, JSON.stringify(body), auth(token)), 'tạo bài');
	data(http.post(`${BASE_URL}/api/teacher/assignments/${created.id}/publish`,
		JSON.stringify({ classIds: [cls.id], studentIds: [] }), auth(token)), 'phát hành');
	return { assignmentId: created.id, partId: created.parts[0].id, classId: cls.id, members: cls.memberCount };
}

/** Id mọi sinh viên của lớp (để nhắc thủ công). */
export function classMemberIds(token, classId) {
	const ids = [];
	for (let page = 0; ; page++) {
		const rows = data(http.get(`${BASE_URL}/api/teacher/classes/${classId}/members?page=${page}&size=100`,
			auth(token)), 'thành viên lớp');
		rows.forEach((m) => ids.push(m.studentId));
		if (rows.length < 100) {
			return ids;
		}
	}
}
