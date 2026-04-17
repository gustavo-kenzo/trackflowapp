package com.gustavo.trackflowapp.modules.account.dto;

import com.gustavo.trackflowapp.modules.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AccountRegisterDTO(
        @NotBlank
        String name,

        @NotNull
        AccountType type,

        @PositiveOrZero
        BigDecimal openingBalance
) {
}
