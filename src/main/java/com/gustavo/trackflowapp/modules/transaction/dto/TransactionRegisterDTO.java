package com.gustavo.trackflowapp.modules.transaction.dto;

import com.gustavo.trackflowapp.modules.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TransactionRegisterDTO(
        @NotNull
        Long accountId,

        Long categoryId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotNull
        TransactionType type,

        @NotNull
        LocalDate competenceDate,

        LocalDate settlementDate,

        @Size(max = 255)
        String description
) {
}
