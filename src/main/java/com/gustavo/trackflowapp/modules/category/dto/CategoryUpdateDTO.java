package com.gustavo.trackflowapp.modules.category.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CategoryUpdateDTO(
        String name,
        Boolean active
) {
}
