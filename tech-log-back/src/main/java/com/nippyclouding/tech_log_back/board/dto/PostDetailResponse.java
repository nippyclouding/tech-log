package com.nippyclouding.tech_log_back.board.dto;

import java.util.List;

public record PostDetailResponse(
        Long id,
        String title,
        String excerpt,
        String content,
        String date,
        PostSummaryResponse.AuthorResponse author,
        String category,
        List<String> tags,
        String coverImage,
        List<PostImageResponse> images,
        boolean published,
        long views
) {
}
