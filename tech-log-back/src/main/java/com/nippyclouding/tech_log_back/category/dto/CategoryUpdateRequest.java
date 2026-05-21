package com.nippyclouding.tech_log_back.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(@NotBlank @Size(max = 100) String name) {
}
