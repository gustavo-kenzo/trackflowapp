package com.gustavo.trackflowapp.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CategoryRegisterDTO(
        @NotBlank
        String name,

        Boolean systemDefault) {
}
