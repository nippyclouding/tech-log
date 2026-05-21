package com.nippyclouding.tech_log_back.category.dto;

import com.nippyclouding.tech_log_back.category.entity.Category;

public record CategoryResponse(Long id, String name, String slug) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), toSlug(category.getName()));
    }

    private static String toSlug(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9가-힣]+", "-").replaceAll("(^-|-$)", "");
    }
}
