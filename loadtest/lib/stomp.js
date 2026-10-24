// Khung STOMP 1.2 tối giản trên WebSocket thuần (backend không dùng SockJS — docs/05 phần B.1).

/** Tạo một khung: lệnh, các header, thân (tuỳ chọn), kết thúc bằng ký tự NUL. */
export function frame(command, headers, body) {
	let out = command + '\n';
	Object.keys(headers || {}).forEach((k) => {
		out += `${k}:${headers[k]}\n`;
	});
	return out + '\n' + (body || '') + '\0';
}

/** Tách khung server gửi; heart-beat (chỉ xuống dòng) → null. */
export function parse(raw) {
	const text = raw.replace(/\0+$/, '');
	if (text.trim() === '') {
		return null;
	}
	const split = text.indexOf('\n\n');
	const head = (split < 0 ? text : text.substring(0, split)).replace(/^\s+/, '').split('\n');
	const headers = {};
	head.slice(1).forEach((line) => {
		const i = line.indexOf(':');
		if (i > 0 && !(line.substring(0, i) in headers)) {
			headers[line.substring(0, i)] = line.substring(i + 1);
		}
	});
	return { command: head[0], headers, body: split < 0 ? '' : text.substring(split + 2) };
}
