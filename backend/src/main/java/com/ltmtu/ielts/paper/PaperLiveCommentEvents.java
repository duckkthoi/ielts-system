package com.ltmtu.ielts.paper;

import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.CommentDto;

public final class PaperLiveCommentEvents {

	private PaperLiveCommentEvents() {
	}

	public record Changed(Long documentId, String type, CommentDto comment, Long commentId) {
	}
}
