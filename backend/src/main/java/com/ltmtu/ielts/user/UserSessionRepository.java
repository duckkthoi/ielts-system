package com.ltmtu.ielts.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

	/** Khoá dòng khi refresh để hai request cùng token không xoay vòng chồng nhau. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.refreshTokenHash = :hash")
	Optional<UserSession> findByRefreshTokenHashForUpdate(@Param("hash") String hash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.previousTokenHash = :hash")
	Optional<UserSession> findByPreviousTokenHashForUpdate(@Param("hash") String hash);

	/** Thông tin cần cho mỗi request có access token: phiên còn hiệu lực và tài khoản còn hoạt động. */
	@Query("""
			SELECT new com.ltmtu.ielts.user.SessionAuthView(
				s.id, s.revokedAt, u.id, u.username, u.role, u.active, u.mustChangePassword)
			FROM UserSession s JOIN s.user u WHERE s.id = :sessionId""")
	Optional<SessionAuthView> findAuthView(@Param("sessionId") Long sessionId);

	@Query("SELECT count(s) FROM UserSession s WHERE s.user.id = :userId AND s.revokedAt IS NULL AND s.expiresAt > :now")
	long countActive(@Param("userId") Long userId, @Param("now") Instant now);

	/** Id các phiên chưa thu hồi của user (trừ một phiên), để báo cho thiết bị của các phiên đó trước khi thu hồi. */
	@Query("SELECT s.id FROM UserSession s WHERE s.user.id = :userId AND s.revokedAt IS NULL "
			+ "AND (:exceptId IS NULL OR s.id <> :exceptId)")
	List<Long> findActiveIds(@Param("userId") Long userId, @Param("exceptId") Long exceptSessionId);

	@Modifying
	@Query("""
			UPDATE UserSession s SET s.revokedAt = :now, s.revokeReason = :reason
			WHERE s.user.id = :userId AND s.revokedAt IS NULL AND (:exceptId IS NULL OR s.id <> :exceptId)""")
	int revokeAllOfUser(@Param("userId") Long userId, @Param("exceptId") Long exceptSessionId,
			@Param("reason") RevokeReason reason, @Param("now") Instant now);
}
