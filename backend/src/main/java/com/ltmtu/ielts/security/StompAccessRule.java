package com.ltmtu.ielts.security;

/**
 * Luật quyền cho một nhóm kênh STOMP (docs/05 B.2). Module nghiệp vụ (vd {@code realtime}) cài luật của kênh mình;
 * {@link StompAuthInterceptor} hỏi lần lượt mọi luật — nhờ vậy module {@code security} không phải phụ thuộc module
 * nghiệp vụ. Kênh không luật nào nhận phụ trách thì bị từ chối.
 */
public interface StompAccessRule {

	/** {@code null} = không phụ trách kênh này; {@code true} / {@code false} = cho / không cho SUBSCRIBE. */
	Boolean canSubscribe(AuthPrincipal user, String destination);

	/** {@code null} = không phụ trách; {@code true} / {@code false} = cho / không cho SEND. */
	Boolean canSend(AuthPrincipal user, String destination);
}
