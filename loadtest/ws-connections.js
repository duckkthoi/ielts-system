// NFR-02 — giữ 500 kết nối WebSocket đồng thời; kèm NFR-03 — thông báo tới người nhận dưới 1 giây.
//
// Mỗi VU là một sinh viên lớp kiểm thử tải: đăng nhập, mở wss://…/ws, gửi STOMP CONNECT (Bearer token, heart-beat
// 10 s), SUBSCRIBE /user/queue/notifications rồi giữ kết nối tới mốc chung. 500 kết nối mở dần trong 60 giây.
// Ở giây 100, khi mọi kết nối đang mở, giáo viên bấm "Nhắc nhở" cho cả 500 sinh viên (một request): mỗi kết nối phải
// nhận đúng thông báo MANUAL_REMINDER, độ trễ = giờ nhận − createdAt của thông báo (cùng đồng hồ máy chủ Docker).
//
// Chạy: docker compose -f docker-compose.yml -f loadtest/compose.loadtest.yml run --rm k6 run ws-connections.js
import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, TEACHER, WS_URL, apiOk, auth, classMemberIds, login, loadStudent, prepareAssignment } from './lib/api.js';
import { frame, parse } from './lib/stomp.js';

const CONNECTIONS = parseInt(__ENV.CONNECTIONS || '500', 10);
const RAMP_S = 60;
const REMIND_AT_S = 100;
const HOLD_S = 160;

const stompConnected = new Counter('stomp_connected');
const stompErrors = new Counter('stomp_errors');
const reminders = new Counter('reminder_received');
const connectTime = new Trend('stomp_connect_duration', true);
const notifyLatency = new Trend('notification_latency', true);

export const options = {
	insecureSkipTLSVerify: true,
	setupTimeout: '60s',
	scenarios: {
		sockets: {
			executor: 'per-vu-iterations', vus: CONNECTIONS, iterations: 1, maxDuration: '5m', exec: 'socket',
		},
		remind: {
			executor: 'per-vu-iterations', vus: 1, iterations: 1, startTime: `${REMIND_AT_S}s`, exec: 'remind',
		},
	},
	thresholds: {
		stomp_connected: [`count>=${CONNECTIONS}`],
		stomp_errors: ['count==0'],
		reminder_received: [`count>=${Math.floor(CONNECTIONS * 0.99)}`],
		notification_latency: ['p(95)<1000'],
		checks: ['rate>0.99'],
	},
	summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
	const token = login(TEACHER);
	const a = prepareAssignment(token, `Kiểm thử tải — WebSocket ${new Date().toISOString()}`, 60);
	return { teacherToken: token, assignmentId: a.assignmentId, studentIds: classMemberIds(token, a.classId),
		closeAt: Date.now() + HOLD_S * 1000 };
}

export function socket(data) {
	// Số thứ tự trong scenario (0…), không dùng __VU: số VU đánh chung cho mọi scenario nên có thể vượt quá 500.
	const n = exec.scenario.iterationInTest;
	sleep((n * RAMP_S) / CONNECTIONS);
	const token = login(loadStudent(n + 1));
	if (!token) {
		return;
	}
	const started = Date.now();
	const res = ws.connect(WS_URL, { tags: { name: 'ws' } }, (s) => {
		s.on('open', () => {
			s.send(frame('CONNECT', {
				'accept-version': '1.2', host: 'localhost', 'heart-beat': '10000,10000', Authorization: `Bearer ${token}`,
			}));
		});
		s.on('message', (raw) => {
			const f = parse(raw);
			if (!f) {
				return; // heart-beat của server
			}
			if (f.command === 'CONNECTED') {
				stompConnected.add(1);
				connectTime.add(Date.now() - started);
				s.send(frame('SUBSCRIBE', { id: 'sub-0', destination: '/user/queue/notifications' }));
				s.setInterval(() => s.send('\n'), 8000); // heart-beat phía client
			} else if (f.command === 'MESSAGE') {
				const n = JSON.parse(f.body);
				if (n.type === 'MANUAL_REMINDER') {
					reminders.add(1);
					notifyLatency.add(Date.now() - Date.parse(n.createdAt));
				}
			} else if (f.command === 'ERROR') {
				stompErrors.add(1);
				console.error(`SV ${n + 1}: STOMP ERROR ${f.headers.message}`);
			}
		});
		s.on('error', (e) => {
			stompErrors.add(1);
			console.error(`SV ${n + 1}: ${e.error()}`);
		});
		s.setTimeout(() => {
			s.send(frame('DISCONNECT', {}));
			s.close();
		}, Math.max(1000, data.closeAt - Date.now()));
	});
	check(res, { 'bắt tay WebSocket 101': (r) => r && r.status === 101 });
}

export function remind(data) {
	const res = http.post(`${BASE_URL}/api/teacher/assignments/${data.assignmentId}/remind`,
		JSON.stringify({ studentIds: data.studentIds }), Object.assign(auth(data.teacherToken), { tags: { name: 'remind' } }));
	check(res, { 'nhắc nhở thành công': apiOk });
	console.log(`Đã nhắc ${res.json('data.sent')} sinh viên, request mất ${Math.round(res.timings.duration)} ms`);
}
