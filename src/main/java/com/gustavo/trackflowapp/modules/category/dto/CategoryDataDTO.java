package com.gustavo.trackflowapp.modules.category.dto;

import com.gustavo.trackflowapp.modules.category.Category;
import com.gustavo.trackflowapp.modules.category.CategoryType;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CategoryDataDTO(
        Long id,
        String owner,
        String name,
        CategoryType type,
        Boolean systemDefault,
        boolean active) {
    public CategoryDataDTO(Category category) {
        this(
                category.getId(),
                category.getUser().getName(),
                category.getName(),
                category.getType(),
                category.isSystemDefault(),
                category.isActive()
        );
    }
}
