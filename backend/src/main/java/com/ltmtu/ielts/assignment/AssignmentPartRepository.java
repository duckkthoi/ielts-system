package com.ltmtu.ielts.assignment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentPartRepository extends JpaRepository<AssignmentPart, Long> {

	@Query("SELECT p FROM AssignmentPart p JOIN FETCH p.assignment WHERE p.id = :id")
	Optional<AssignmentPart> findWithAssignment(@Param("id") Long id);
}
