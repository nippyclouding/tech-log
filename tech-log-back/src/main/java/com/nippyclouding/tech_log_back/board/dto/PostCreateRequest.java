package com.nippyclouding.tech_log_back.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostCreateRequest(
        @NotBlank @Size(max = 255) String title,
        String excerpt,
        @NotBlank String content,
        String category,
        String coverImage,
        List<String> tags,
        List<String> categories
) {
}
