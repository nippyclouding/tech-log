package com.nippyclouding.tech_log_back.comment.dto;

import com.nippyclouding.tech_log_back.comment.entity.Comment;

public record CommentResponse(
        Long id,
        Long postId,
        String authorName,
        String authorAvatar,
        String authorGithubUrl,
        String content,
        String date,
        boolean ownedByCurrentUser
) {

    public static CommentResponse from(Comment comment) {
        return from(comment, null);
    }

    public static CommentResponse from(Comment comment, Long viewerGithubId) {
        return new CommentResponse(
                comment.getId(),
                comment.getBoard().getId(),
                comment.getGithubName(),
                comment.getGithubAvatarUrl(),
                "https://github.com/" + comment.getGithubName(),
                comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent(),
                comment.getUpdatedAt().toString(),
                viewerGithubId != null && viewerGithubId.equals(comment.getGithubId())
        );
    }
}
