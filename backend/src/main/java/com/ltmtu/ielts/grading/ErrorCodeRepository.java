package com.ltmtu.ielts.grading;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ErrorCodeRepository extends JpaRepository<ErrorCode, Long> {

	/** Mã của hệ thống (theo thứ tự đã định) rồi tới mã riêng của giáo viên (theo chữ cái). */
	@Query("SELECT e FROM ErrorCode e WHERE e.owner IS NULL OR e.owner.id = :ownerId "
			+ "ORDER BY CASE WHEN e.owner IS NULL THEN 0 ELSE 1 END, e.orderNo, e.code")
	List<ErrorCode> findUsableBy(@Param("ownerId") Long ownerId);

	/** Mã hiển thị được với giáo viên: của hệ thống hoặc của chính họ. Mã riêng của người khác → không thấy. */
	@Query("SELECT e FROM ErrorCode e WHERE e.id = :id AND (e.owner IS NULL OR e.owner.id = :ownerId)")
	Optional<ErrorCode> findUsable(@Param("id") Long id, @Param("ownerId") Long ownerId);

	/** Trùng mã (không phân biệt hoa thường) với mã hệ thống hoặc mã khác của cùng giáo viên. */
	@Query("SELECT COUNT(e) > 0 FROM ErrorCode e WHERE upper(e.code) = upper(:code) "
			+ "AND (e.owner IS NULL OR e.owner.id = :ownerId) AND (:exceptId IS NULL OR e.id <> :exceptId)")
	boolean existsConflicting(@Param("code") String code, @Param("ownerId") Long ownerId,
			@Param("exceptId") Long exceptId);

	long countByOwnerId(Long ownerId);
}
