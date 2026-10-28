package com.ltmtu.ielts.paper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ltmtu.ielts.paper.dto.PaperGroupDtos.GroupDto;
import com.ltmtu.ielts.paper.dto.PaperGroupDtos.MemberDto;
import com.ltmtu.ielts.user.User;

/** Ghép nhóm + thành viên thành DTO (dữ liệu đã nạp sẵn bằng hai câu truy vấn, không N+1). */
final class PaperGroupViews {

	private PaperGroupViews() {
	}

	static List<GroupDto> groups(List<PaperGroup> groups, List<PaperGroupMember> members) {
		Map<Long, List<MemberDto>> byGroup = new LinkedHashMap<>();
		for (PaperGroup g : groups) {
			byGroup.put(g.getId(), new ArrayList<>());
		}
		for (PaperGroupMember m : members) {
			User u = m.getStudent();
			List<MemberDto> list = byGroup.get(m.getId().getGroupId());
			if (list != null) {
				list.add(new MemberDto(u.getId(), u.getStudentCode(), u.getFullName(), m.getJoinedAt()));
			}
		}
		return groups.stream()
				.map(g -> new GroupDto(g.getId(), g.getName(), g.isStudentMade(), List.copyOf(byGroup.get(g.getId()))))
				.toList();
	}
}
