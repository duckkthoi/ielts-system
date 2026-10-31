package com.ltmtu.ielts.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ltmtu.ielts.user.dto.AdminSessionDtos.SessionView;

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

	/**
	 * UC-08: danh sách phiên cho admin. Tham số NULL = không lọc. {@code keyword} đã chuyển thường + bọc {@code %…%}
	 * (tên đăng nhập, họ tên); {@code status} ∈ ACTIVE / EXPIRED / REVOKED; {@code from} / {@code to} lọc theo lúc
	 * đăng nhập (luôn có giá trị — PostgreSQL không đoán được kiểu của tham số thời gian NULL). Sắp xếp theo {@code Pageable} (service đã lọc trường được phép).
	 */
	@Query(value = """
			SELECT new com.ltmtu.ielts.user.dto.AdminSessionDtos$SessionView(
				s.id, u.id, u.username, u.fullName, u.role, s.ip, s.userAgent, s.createdAt, s.lastUsedAt, s.expiresAt,
				s.revokedAt, s.revokeReason)
			FROM UserSession s JOIN s.user u
			WHERE (:role IS NULL OR u.role = :role)
			  AND (:keyword IS NULL
			       OR lower(u.username) LIKE :keyword ESCAPE '!'
			       OR lower(u.fullName) LIKE :keyword ESCAPE '!')
			  AND (:status IS NULL
			       OR (:status = 'ACTIVE' AND s.revokedAt IS NULL AND s.expiresAt > :now)
			       OR (:status = 'EXPIRED' AND s.revokedAt IS NULL AND s.expiresAt <= :now)
			       OR (:status = 'REVOKED' AND s.revokedAt IS NOT NULL))
			  AND s.createdAt >= :from AND s.createdAt < :to""",
			countQuery = """
					SELECT count(s) FROM UserSession s JOIN s.user u
					WHERE (:role IS NULL OR u.role = :role)
					  AND (:keyword IS NULL
					       OR lower(u.username) LIKE :keyword ESCAPE '!'
					       OR lower(u.fullName) LIKE :keyword ESCAPE '!')
					  AND (:status IS NULL
					       OR (:status = 'ACTIVE' AND s.revokedAt IS NULL AND s.expiresAt > :now)
					       OR (:status = 'EXPIRED' AND s.revokedAt IS NULL AND s.expiresAt <= :now)
					       OR (:status = 'REVOKED' AND s.revokedAt IS NOT NULL))
					  AND s.createdAt >= :from AND s.createdAt < :to""")
	Page<SessionView> search(@Param("keyword") String keyword, @Param("role") Role role,
			@Param("status") String status, @Param("from") Instant from, @Param("to") Instant to,
			@Param("now") Instant now, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.id = :id")
	Optional<UserSession> findByIdForUpdate(@Param("id") Long id);

	/** Dọn phiên đã hết hạn (refresh token của chúng vốn đã vô hiệu). */
	@Modifying
	@Query("DELETE FROM UserSession s WHERE s.expiresAt <= :now")
	int deleteExpired(@Param("now") Instant now);
}
