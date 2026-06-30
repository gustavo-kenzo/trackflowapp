package com.gustavo.trackflowapp.modules.category.dto;

import com.gustavo.trackflowapp.modules.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CategoryRegisterDTO(
        @NotBlank
        String name,

        @NotNull
        CategoryType type,

        Boolean systemDefault) {
}
