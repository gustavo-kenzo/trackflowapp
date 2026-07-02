package com.gustavo.trackflowapp.modules.transaction.dto;

import com.gustavo.trackflowapp.modules.transaction.TransactionType;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TransactionUpdateDTO(
        Long id,
        Long accountId,
        Long categoryId,
        BigDecimal amount,
        TransactionType type,
        LocalDate competenceDate,
        String description
) {
}
