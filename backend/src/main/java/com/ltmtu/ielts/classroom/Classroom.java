package com.ltmtu.ielts.classroom;

import com.ltmtu.ielts.common.BaseEntity;
import com.ltmtu.ielts.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "classrooms")
public class Classroom extends BaseEntity {

	/** Mã lớp, viết hoa, duy nhất toàn hệ thống; dùng trong lệnh TCP {@code IMPORT <mã lớp>}. */
	@Column(nullable = false, unique = true, length = 30)
	private String code;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	/** Giáo viên phụ trách — chỉ người này thao tác được với lớp (BR-15). */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id")
	private User teacher;
}
