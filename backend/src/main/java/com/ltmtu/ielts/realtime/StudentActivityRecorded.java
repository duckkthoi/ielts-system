package com.ltmtu.ielts.realtime;

import com.ltmtu.ielts.realtime.dto.StudentActivityDtos.StudentActivityEvent;

/** Sự kiện nội bộ để đẩy hoạt động sau khi giao dịch lưu đã commit. */
public record StudentActivityRecorded(Long assignmentId, StudentActivityEvent activity) {
}
