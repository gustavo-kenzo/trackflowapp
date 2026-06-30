package com.gustavo.trackflowapp.modules.transaction.dto;

import com.gustavo.trackflowapp.modules.transaction.Transaction;
import com.gustavo.trackflowapp.modules.transaction.TransactionStatus;
import com.gustavo.trackflowapp.modules.transaction.TransactionType;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TransactionDataDTO(
        Long transactionId,
        Long userId,
        Long accountId,
        Long categoryId,
        Long recurrenceId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        LocalDate competenceDate,
        LocalDate settlementDate,
        String description
) {
    public TransactionDataDTO(Transaction transaction) {
        this(
                transaction.getId(),
                transaction.getUser().getId(),
                transaction.getAccount().getId(),
                transaction.getCategory() != null ? transaction.getCategory().getId() : null,
                transaction.getRecurrence() != null ? transaction.getRecurrence().getId() : null,
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCompetenceDate(),
                transaction.getSettlementDate(),
                transaction.getDescription());
    }
}
