package com.nippyclouding.tech_log_back.board.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryResponse(
        Long id,
        String title,
        String excerpt,
        String date,
        AuthorResponse author,
        String category,
        List<String> tags,
        String coverImage,
        boolean published,
        long views
) {

    public record AuthorResponse(String name, String avatar, String role) {
    }

    public static PostSummaryResponse of(
            Long id,
            String title,
            String excerpt,
            LocalDateTime updatedAt,
            String category,
            List<String> tags,
            String coverImage,
            long views
    ) {
        return new PostSummaryResponse(
                id,
                title,
                excerpt,
                updatedAt.toString(),
                new AuthorResponse("Sangwon", "", "Owner"),
                category,
                tags,
                coverImage,
                true,
                views
        );
    }
}
