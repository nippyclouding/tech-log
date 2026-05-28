package com.nippyclouding.tech_log_back.board.dto;

import com.nippyclouding.tech_log_back.image.entity.Image;

public record PostImageResponse(
        String url,
        String originalName,
        String storedName,
        int order,
        boolean thumbnail
) {

    public static PostImageResponse from(Image image) {
        return new PostImageResponse(
                image.getFileKey(),
                image.getOriginalName(),
                image.getStoredName(),
                image.getImageOrder(),
                image.isThumbnail()
        );
    }
}
