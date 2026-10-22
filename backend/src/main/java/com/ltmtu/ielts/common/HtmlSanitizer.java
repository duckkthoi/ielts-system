package com.ltmtu.ielts.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Lọc HTML do người dùng nhập (vd đề bài) trước khi lưu, chống XSS:
 * chỉ giữ thẻ định dạng cơ bản (đoạn, đậm, nghiêng, gạch chân, danh sách, link an toàn), bỏ script, style, sự kiện.
 */
public final class HtmlSanitizer {

	private static final Safelist SAFELIST = Safelist.basic().addTags("h3", "h4");
	private static final Document.OutputSettings OUTPUT = new Document.OutputSettings().prettyPrint(false);

	private HtmlSanitizer() {
	}

	public static String sanitize(String html) {
		if (html == null) {
			return null;
		}
		return Jsoup.clean(html, "", SAFELIST, OUTPUT).trim();
	}
}
