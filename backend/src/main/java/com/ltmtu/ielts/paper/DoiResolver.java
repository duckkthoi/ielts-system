package com.ltmtu.ielts.paper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.config.AppProperties;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Tra DOI ra bản ghi CSL-JSON (docs/12 mục 5.3): gọi {@code https://doi.org/{doi}} với
 * {@code Accept: application/vnd.citationstyles.csl+json} — doi.org chuyển hướng tới Crossref / DataCite và trả CSL.
 * Đi qua backend để trình duyệt không vướng CORS và để giới hạn tần suất. Không thêm thư viện: dùng RestClient của Spring.
 */
@Slf4j
@Component
public class DoiResolver {

	static final String CSL_JSON = "application/vnd.citationstyles.csl+json";
	/** Bản ghi CSL của một bài báo thường vài chục KB (có cả danh mục tham khảo) — quá 1 MB coi như bất thường. */
	private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

	private final RestClient client;
	private final String baseUrl;
	private final ObjectMapper json;

	public DoiResolver(AppProperties props, ObjectMapper json) {
		this.json = json;
		this.baseUrl = props.paper().doiBaseUrl().replaceAll("/+$", "");
		HttpClient http = HttpClient.newBuilder()
				.connectTimeout(props.paper().doiTimeout())
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
		factory.setReadTimeout(props.paper().doiTimeout());
		this.client = RestClient.builder().requestFactory(factory).build();
	}

	/** Bản ghi CSL gốc của DOI (đã chuẩn hoá). Không có DOI này → 404; dịch vụ lỗi / quá thời gian → 503. */
	JsonNode resolve(String doi) {
		URI uri = URI.create(baseUrl + "/" + UriUtils.encodePath(doi, StandardCharsets.UTF_8));
		try {
			String body = client.get().uri(uri).header("Accept", CSL_JSON).exchange((req, res) -> {
				if (res.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
					throw ApiException.notFound("Không tìm thấy DOI " + doi);
				}
				if (!res.getStatusCode().is2xxSuccessful()) {
					log.warn("Tra DOI {} lỗi: HTTP {}", doi, res.getStatusCode().value());
					throw unavailable();
				}
				return readLimited(res.getBody());
			});
			JsonNode node = json.readTree(body);
			if (!node.isObject()) {
				throw unavailable();
			}
			return node;
		} catch (ResourceAccessException | JacksonException e) {
			log.warn("Tra DOI {} không được: {}", doi, e.getMessage());
			throw unavailable();
		}
	}

	private static String readLimited(InputStream in) throws IOException {
		byte[] bytes = in.readNBytes(MAX_RESPONSE_BYTES + 1);
		if (bytes.length > MAX_RESPONSE_BYTES) {
			throw unavailable();
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static ApiException unavailable() {
		return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DOI_UNAVAILABLE",
				"Chưa tra được DOI lúc này. Hãy thử lại sau hoặc nhập tay");
	}
}
