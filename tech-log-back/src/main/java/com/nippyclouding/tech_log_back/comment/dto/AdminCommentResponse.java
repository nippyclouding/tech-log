package com.nippyclouding.tech_log_back.comment.dto;

import com.nippyclouding.tech_log_back.comment.entity.Comment;

public record AdminCommentResponse(
        Long id,
        Long postId,
        String postTitle,
        String authorName,
        String authorAvatar,
        String authorGithubUrl,
        String content,
        boolean deleted,
        String accessIp,
        String date
) {

    public static AdminCommentResponse from(Comment comment) {
        return new AdminCommentResponse(
                comment.getId(),
                comment.getBoard().getId(),
                comment.getBoard().getTitle(),
                comment.getGithubName(),
                comment.getGithubAvatarUrl(),
                "https://github.com/" + comment.getGithubName(),
                comment.getContent(),
                comment.isDeleted(),
                comment.getAccessIp(),
                comment.getUpdatedAt().toString()
        );
    }
}
