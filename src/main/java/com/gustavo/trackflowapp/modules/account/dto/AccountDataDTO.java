package com.gustavo.trackflowapp.modules.account.dto;

import com.gustavo.trackflowapp.modules.account.Account;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AccountDataDTO(
        Long id,
        String owner,
        String accountName,
        BigDecimal currentBalance,
        boolean active
) {
    public AccountDataDTO(Account account) {
        this(
                account.getId(),
                account.getUser().getName(),
                account.getName(),
                account.getCurrentBalance(),
                account.isActive());
    }
}
