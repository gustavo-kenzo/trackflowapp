package com.gustavo.trackflowapp.modules.transaction.dto;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TransactionSettleDTO(
        @NotNull
        Long id,
        LocalDate settlementDate
) {
}
