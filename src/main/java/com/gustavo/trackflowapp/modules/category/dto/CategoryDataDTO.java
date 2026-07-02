package com.gustavo.trackflowapp.modules.category.dto;

import com.gustavo.trackflowapp.modules.category.Category;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CategoryDataDTO(
        Long id,
        String owner,
        String name,
        boolean active) {
    public CategoryDataDTO(Category category) {
        this(
                category.getId(),
                category.getUser().getName(),
                category.getName(),
                category.isActive()
        );
    }
}
