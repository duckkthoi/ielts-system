package com.ltmtu.ielts.tcpgateway;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Đọc khung tin từ luồng byte: dòng lệnh kết thúc bằng {@code \n}, rồi phần dữ liệu đúng {@code n} byte (docs/05 mục
 * C.1). Không dùng {@code BufferedReader} vì nó đọc trước vào bộ đệm, làm lệch số byte của phần CSV ngay sau dòng
 * {@code IMPORT}. Class thuần để unit test.
 */
public class FrameReader {

	private final InputStream in;
	private final int maxLineBytes;

	/** {@code in} nên là luồng có bộ đệm (vd {@code BufferedInputStream}) vì đọc từng byte. */
	public FrameReader(InputStream in, int maxLineBytes) {
		this.in = in;
		this.maxLineBytes = maxLineBytes;
	}

	/**
	 * Một dòng UTF-8, đã bỏ {@code \n} và {@code \r} cuối. {@code null} nếu luồng kết thúc trước khi có byte nào.
	 * Dòng dài quá giới hạn → {@link TcpException} 400.
	 */
	public String readLine() throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int b;
		while ((b = in.read()) != -1) {
			if (b == '\n') {
				return decode(buf);
			}
			if (buf.size() >= maxLineBytes) {
				throw new TcpException(400, "Dòng lệnh dài quá " + maxLineBytes + " byte");
			}
			buf.write(b);
		}
		if (buf.size() == 0) {
			return null;
		}
		return decode(buf);
	}

	/** Đúng {@code length} byte. Luồng kết thúc trước khi đủ → {@link EOFException}. */
	public byte[] readExactly(int length) throws IOException {
		byte[] data = in.readNBytes(length);
		if (data.length < length) {
			throw new EOFException("Nhận " + data.length + "/" + length + " byte thì kết nối đóng");
		}
		return data;
	}

	private static String decode(ByteArrayOutputStream buf) {
		String line = buf.toString(StandardCharsets.UTF_8);
		return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
	}
}
