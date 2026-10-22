package com.ltmtu.ielts.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	/** Đăng nhập không phân biệt hoa thường (sinh viên gõ mã SV chữ thường vẫn vào được). */
	Optional<User> findByUsernameIgnoreCase(String username);

	boolean existsByUsernameIgnoreCase(String username);

	Optional<User> findByStudentCode(String studentCode);

	/** Mã SV so khớp không phân biệt hoa thường (import lưu mã viết hoa, dữ liệu cũ có thể khác). */
	Optional<User> findByStudentCodeIgnoreCase(String studentCode);

	Optional<User> findByIdAndRole(Long id, Role role);

	/** {@code upperCodes} phải đã viết hoa. */
	@Query("SELECT u FROM User u WHERE u.role = :role AND upper(u.studentCode) IN :upperCodes")
	List<User> findByRoleAndStudentCodeInIgnoreCase(@Param("role") Role role,
			@Param("upperCodes") Collection<String> upperCodes);

	boolean existsByUsername(String username);

	boolean existsByRole(Role role);

	/**
	 * Tra cứu người dùng. Tham số NULL = không lọc theo tiêu chí đó.
	 * {@code keyword} đã được chuyển thường và bọc {@code %…%}; so khớp tên đăng nhập, họ tên, mã SV, email.
	 */
	@Query("""
			SELECT u FROM User u
			WHERE (:role IS NULL OR u.role = :role)
			  AND (:active IS NULL OR u.active = :active)
			  AND (:keyword IS NULL
			       OR lower(u.username) LIKE :keyword ESCAPE '!'
			       OR lower(u.fullName) LIKE :keyword ESCAPE '!'
			       OR lower(coalesce(u.studentCode, '')) LIKE :keyword ESCAPE '!'
			       OR lower(coalesce(u.email, '')) LIKE :keyword ESCAPE '!')""")
	Page<User> search(@Param("keyword") String keyword, @Param("role") Role role, @Param("active") Boolean active,
			Pageable pageable);
}
