package com.gustavo.trackflowapp.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AccountRegisterDTO(
        @NotBlank
        String name,

        @PositiveOrZero
        BigDecimal openingBalance
) {
}
