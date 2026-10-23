package com.ltmtu.ielts.tcpgateway;

/** Lỗi của giao thức TCP, gửi cho client dạng {@code ERR <code> <message>} (docs/05 mục C.4). */
public class TcpException extends RuntimeException {

	private final int code;

	public TcpException(int code, String message) {
		super(message);
		this.code = code;
	}

	public int code() {
		return code;
	}
}
