package com.ltmtu.ielts.stats;

/**
 * SQL của thống kê tổng hợp (H5). Mọi truy vấn bắt đầu bằng {@link #SCOPE}: tập bài đã phát hành theo bộ lọc
 * ({@code sa}), người nhận ({@code rcp}) và đơn vị (sinh viên, phần) kèm trạng thái bài làm ({@code units}). Tham số:
 * {@code :teacherId}, {@code :classId}, {@code :assignmentId}, {@code :kind}, {@code :status} (null = không lọc),
 * {@code :now}, {@code :from}, {@code :to} (khoảng [from, to) theo giờ), {@code :zone}.
 */
final class AnalyticsSql {

	private AnalyticsSql() {
	}

	/** Lọc lớp trên một cột {@code student_id}. */
	private static final String IN_CLASS = """
			(CAST(:classId AS BIGINT) IS NULL
			 OR EXISTS (SELECT 1 FROM class_members cm WHERE cm.classroom_id = :classId AND cm.student_id = %s))""";

	static final String SCOPE = """
			WITH sa AS (
			    SELECT a.id, a.kind, a.status, a.due_at, a.title, a.teacher_id FROM assignments a
			    WHERE a.status <> 'DRAFT'
			      AND (CAST(:teacherId AS BIGINT) IS NULL OR a.teacher_id = :teacherId)
			      AND (CAST(:assignmentId AS BIGINT) IS NULL OR a.id = :assignmentId)
			      AND (CAST(:kind AS VARCHAR) IS NULL OR a.kind = :kind)
			      AND (CAST(:status AS VARCHAR) IS NULL OR a.status = :status)
			),
			rcp AS (
			    SELECT DISTINCT x.assignment_id, x.student_id FROM (
			        SELECT t.assignment_id, t.student_id FROM assignment_targets t JOIN sa ON sa.id = t.assignment_id
			        WHERE t.student_id IS NOT NULL
			        UNION
			        SELECT t.assignment_id, m.student_id FROM assignment_targets t JOIN sa ON sa.id = t.assignment_id
			        JOIN class_members m ON m.classroom_id = t.classroom_id
			    ) x
			    WHERE %s
			),
			units AS (
			    SELECT r.assignment_id, r.student_id, p.id AS part_id, coalesce(p.part_due_at, sa.due_at) AS due_at,
			           sub.id AS submission_id, coalesce(sub.status, 'NOT_STARTED') AS status,
			           coalesce(sub.current_version, 0) AS current_version
			    FROM rcp r
			    JOIN sa ON sa.id = r.assignment_id
			    JOIN assignment_parts p ON p.assignment_id = r.assignment_id
			    LEFT JOIN LATERAL (
			        SELECT s.id, s.status, s.current_version FROM submissions s
			        JOIN submission_members sm ON sm.submission_id = s.id
			        WHERE s.part_id = p.id AND sm.student_id = r.student_id
			        ORDER BY s.id LIMIT 1
			    ) sub ON TRUE
			)
			""".formatted(IN_CLASS.formatted("x.student_id"));

	/** Lần nộp (submission_versions {@code v}) thuộc phạm vi — bài nhóm đếm một lần. */
	static final String VERSIONS = """
			FROM submission_versions v
			JOIN submissions s ON s.id = v.submission_id
			JOIN assignment_parts p ON p.id = s.part_id
			JOIN sa ON sa.id = p.assignment_id
			WHERE (CAST(:classId AS BIGINT) IS NULL OR EXISTS (
			        SELECT 1 FROM submission_members sm JOIN class_members cm ON cm.student_id = sm.student_id
			        WHERE sm.submission_id = s.id AND cm.classroom_id = :classId))""";

	static final String UNIT_COUNTS = SCOPE + """
			SELECT count(*) AS total,
			       count(*) FILTER (WHERE status = 'NOT_STARTED') AS not_started,
			       count(*) FILTER (WHERE status = 'DRAFT') AS in_progress,
			       count(*) FILTER (WHERE status IN ('SUBMITTED', 'GRADED')) AS submitted,
			       count(*) FILTER (WHERE status = 'REVISION_REQUESTED') AS revision,
			       count(*) FILTER (WHERE status = 'RETURNED') AS returned,
			       count(*) FILTER (WHERE status IN ('NOT_STARTED', 'DRAFT') AND due_at < :now) AS overdue
			FROM units""";

	static final String BY_ASSIGNMENT = SCOPE + """
			SELECT sa.id, sa.title, sa.kind,
			       count(u.*) FILTER (WHERE u.status = 'NOT_STARTED') AS not_started,
			       count(u.*) FILTER (WHERE u.status = 'DRAFT') AS in_progress,
			       count(u.*) FILTER (WHERE u.status IN ('SUBMITTED', 'GRADED')) AS submitted,
			       count(u.*) FILTER (WHERE u.status = 'REVISION_REQUESTED') AS revision,
			       count(u.*) FILTER (WHERE u.status = 'RETURNED') AS returned,
			       count(u.*) FILTER (WHERE u.status IN ('NOT_STARTED', 'DRAFT') AND u.due_at < :now) AS overdue
			FROM sa LEFT JOIN units u ON u.assignment_id = sa.id
			GROUP BY sa.id, sa.title, sa.kind, sa.due_at
			ORDER BY sa.due_at DESC NULLS LAST, sa.id DESC
			LIMIT 12""";

	/** Số lần nộp + số lần nộp muộn trong [from, to). */
	static final String VERSION_COUNTS = SCOPE + """
			SELECT count(*) AS total, count(*) FILTER (WHERE v.is_late) AS late
			""" + VERSIONS + " AND v.submitted_at >= :from AND v.submitted_at < :to";

	static final String BY_DAY = SCOPE + """
			, days AS (SELECT generate_series(CAST(:fromDate AS date), CAST(:toDate AS date), interval '1 day')::date AS d),
			counted AS (
			    SELECT (v.submitted_at AT TIME ZONE :zone)::date AS d, count(*) AS n
			""" + VERSIONS + """
			     AND v.submitted_at >= :from AND v.submitted_at < :to
			    GROUP BY 1
			)
			SELECT days.d AS day, coalesce(counted.n, 0) AS total FROM days LEFT JOIN counted ON counted.d = days.d
			ORDER BY days.d""";

	/** Điểm trung bình các bài trả trong [from, to): bài luận (band phần) và Paper (điểm nhóm thang 10). */
	static final String AVG_RETURNED = SCOPE + """
			SELECT round(avg(g.overall_score) FILTER (WHERE sa.kind = 'ESSAY'), 2) AS band,
			       round(avg(round(g.overall_score, 2)) FILTER (WHERE sa.kind = 'PAPER'), 2) AS paper
			FROM grades g JOIN submission_versions v ON v.id = g.submission_version_id
			JOIN submissions s ON s.id = v.submission_id
			JOIN assignment_parts p ON p.id = s.part_id
			JOIN sa ON sa.id = p.assignment_id
			WHERE g.status = 'RETURNED' AND g.returned_at >= :from AND g.returned_at < :to
			  AND (CAST(:classId AS BIGINT) IS NULL OR EXISTS (
			        SELECT 1 FROM submission_members sm JOIN class_members cm ON cm.student_id = sm.student_id
			        WHERE sm.submission_id = s.id AND cm.classroom_id = :classId))""";

	/** Kết quả đã trả mới nhất của mỗi (bài làm, sinh viên) trong phạm vi — CTE {@code latest} của {@link StatsSql}. */
	static final String LATEST = SCOPE + ", " + StatsSql.latestReturned(
			"p.assignment_id IN (SELECT id FROM sa) AND " + IN_CLASS.formatted("sm.student_id"))
			.replaceFirst("^\\s*", "") + "\n";

	static final String SCORE_SUMMARY = LATEST + """
			SELECT count(*) AS n, round(avg(overall_score), 2) AS mean,
			       round(CAST(percentile_cont(0.5) WITHIN GROUP (ORDER BY overall_score) AS NUMERIC), 2) AS median,
			       min(overall_score) AS lo, max(overall_score) AS hi
			FROM latest WHERE paper = :paper AND overall_score IS NOT NULL""";

	/** Bài luận: theo band (bước 0,5); Paper: làm tròn xuống nửa điểm. */
	static final String HISTOGRAM = LATEST + """
			SELECT floor(overall_score * 2) / 2 AS bucket, count(*) AS n
			FROM latest WHERE paper = :paper AND overall_score IS NOT NULL
			GROUP BY 1 ORDER BY 1""";

	static final String CRITERIA = LATEST + """
			SELECT CASE WHEN l.paper THEN 'PAPER' ELSE 'ESSAY' END AS kind, rc.name,
			       round(avg(cs.score), 2) AS average, max(rc.max_score) AS max_score, count(*) AS n
			FROM latest l
			JOIN grade_criterion_scores cs ON cs.grade_id = l.grade_id
			JOIN rubric_criteria rc ON rc.id = cs.criterion_id
			WHERE cs.score IS NOT NULL
			GROUP BY 1, rc.name
			ORDER BY 1, n DESC, rc.name
			LIMIT 16""";

	static final String TOP_ERRORS = LATEST + StatsSql.TOP_ERRORS;

	/** Hoạt động trong [from, to): lần nộp + cập nhật tài liệu Paper, theo thứ trong tuần × giờ. */
	static final String HEATMAP = SCOPE + """
			, ev AS (
			    SELECT v.submitted_at AS t
			""" + VERSIONS + """
			     AND v.submitted_at >= :from AND v.submitted_at < :to
			    UNION ALL
			    SELECT u.created_at FROM paper_doc_updates u
			    JOIN paper_documents d ON d.id = u.document_id
			    JOIN assignment_parts p ON p.id = d.part_id
			    JOIN sa ON sa.id = p.assignment_id
			    WHERE u.created_at >= :from AND u.created_at < :to
			      AND """ + IN_CLASS.formatted("u.author_id") + """
			)
			SELECT CAST(extract(isodow FROM t AT TIME ZONE :zone) AS INT) AS dow,
			       CAST(extract(hour FROM t AT TIME ZONE :zone) AS INT) AS hr, count(*) AS n
			FROM ev GROUP BY 1, 2 ORDER BY 1, 2""";

	static final String PAPER = SCOPE + """
			SELECT
			  (SELECT count(*) FROM paper_groups g JOIN sa ON sa.id = g.assignment_id) AS groups,
			  (SELECT count(*) FROM rcp r JOIN sa ON sa.id = r.assignment_id
			   WHERE sa.kind = 'PAPER' AND NOT EXISTS (
			     SELECT 1 FROM paper_group_members m WHERE m.assignment_id = r.assignment_id AND m.student_id = r.student_id)
			  ) AS without_group,
			  (SELECT count(*) FROM paper_doc_updates u JOIN paper_documents d ON d.id = u.document_id
			   JOIN assignment_parts p ON p.id = d.part_id JOIN sa ON sa.id = p.assignment_id
			   WHERE u.created_at >= :from AND u.created_at < :to) AS updates,
			  (SELECT count(*) FROM paper_live_comments c JOIN paper_documents d ON d.id = c.document_id
			   JOIN assignment_parts p ON p.id = d.part_id JOIN sa ON sa.id = p.assignment_id
			   WHERE c.created_at >= :from AND c.created_at < :to) AS comments,
			  (SELECT count(*) FROM paper_live_comments c JOIN paper_documents d ON d.id = c.document_id
			   JOIN assignment_parts p ON p.id = d.part_id JOIN sa ON sa.id = p.assignment_id
			   WHERE c.resolved_at IS NULL) AS open_comments""";

	/** Bài theo loại × trạng thái (cả nháp), theo giáo viên. */
	static final String ASSIGNMENTS = """
			SELECT a.kind, a.status, count(*) AS n FROM assignments a
			WHERE (CAST(:teacherId AS BIGINT) IS NULL OR a.teacher_id = :teacherId)
			  AND (CAST(:kind AS VARCHAR) IS NULL OR a.kind = :kind)
			GROUP BY 1, 2 ORDER BY 1, 2""";

	static final String CLASSES = """
			SELECT count(*) AS total,
			       count(*) FILTER (WHERE EXISTS (
			         SELECT 1 FROM assignment_targets t JOIN assignments a ON a.id = t.assignment_id
			         WHERE t.classroom_id = c.id AND a.status = 'PUBLISHED')) AS open
			FROM classrooms c
			WHERE (CAST(:teacherId AS BIGINT) IS NULL OR c.teacher_id = :teacherId)
			  AND (CAST(:classId AS BIGINT) IS NULL OR c.id = :classId)""";

	/**
	 * Bảng sinh viên: số phần được giao / đã nộp / quá hạn / lần sửa, band gần nhất + tiến bộ, hoạt động cuối (phiên,
	 * lần nộp, nháp, sửa tài liệu Paper). {@code :alertsOnly} = chỉ người có cảnh báo; phân trang {@code :limit},
	 * {@code :offset}; {@code total} = tổng số dòng (trước phân trang).
	 */
	static final String STUDENTS = LATEST + """
			, per AS (
			    SELECT u.student_id, count(*) AS assigned,
			           count(*) FILTER (WHERE u.status IN ('SUBMITTED', 'GRADED', 'RETURNED', 'REVISION_REQUESTED')) AS submitted,
			           count(*) FILTER (WHERE u.status IN ('NOT_STARTED', 'DRAFT') AND u.due_at < :now) AS overdue,
			           count(*) FILTER (WHERE u.status = 'REVISION_REQUESTED')
			             + coalesce(sum(greatest(u.current_version - 1, 0)), 0) AS revisions
			    FROM units u GROUP BY u.student_id
			),
			bands AS (
			    SELECT student_id,
			           (array_agg(overall_score ORDER BY returned_at DESC))[1] AS latest,
			           (array_agg(overall_score ORDER BY returned_at DESC))[1]
			             - (array_agg(overall_score ORDER BY returned_at))[1] AS progress
			    FROM latest WHERE NOT paper AND overall_score IS NOT NULL GROUP BY student_id
			),
			rows AS (
			    SELECT per.*, us.student_code, us.full_name, b.latest, b.progress,
			           (SELECT string_agg(c.code, ', ' ORDER BY c.code) FROM class_members cm
			            JOIN classrooms c ON c.id = cm.classroom_id
			            WHERE cm.student_id = per.student_id
			              AND (CAST(:teacherId AS BIGINT) IS NULL OR c.teacher_id = :teacherId)) AS classes,
			           greatest(
			             (SELECT max(ss.last_used_at) FROM user_sessions ss WHERE ss.user_id = per.student_id),
			             (SELECT max(v.submitted_at) FROM submission_versions v
			              JOIN submission_members sm ON sm.submission_id = v.submission_id WHERE sm.student_id = per.student_id),
			             (SELECT max(s.draft_saved_at) FROM submissions s
			              JOIN submission_members sm ON sm.submission_id = s.id WHERE sm.student_id = per.student_id),
			             (SELECT max(pu.created_at) FROM paper_doc_updates pu WHERE pu.author_id = per.student_id)
			           ) AS last_activity
			    FROM per JOIN users us ON us.id = per.student_id
			    LEFT JOIN bands b ON b.student_id = per.student_id
			),
			flagged AS (
			    SELECT rows.*,
			           (last_activity IS NULL OR last_activity < :inactiveBefore) AS inactive,
			           (overdue > 0 OR revisions >= 2 OR last_activity IS NULL OR last_activity < :inactiveBefore) AS alert
			    FROM rows
			)
			SELECT flagged.*, count(*) OVER () AS total FROM flagged
			WHERE NOT CAST(:alertsOnly AS BOOLEAN) OR alert
			ORDER BY alert DESC, overdue DESC, inactive DESC, revisions DESC, full_name, student_id
			LIMIT :limit OFFSET :offset""";
}
