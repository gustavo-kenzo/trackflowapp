package com.gustavo.trackflowapp.modules.recurrence.dto;

import com.gustavo.trackflowapp.modules.recurrence.RecurrenceFrequency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RecurrenceRegisterDTO(
        @NotNull
        RecurrenceFrequency frequency,

        LocalDate startDate,
        LocalDate endDate,

        @NotNull
        @PositiveOrZero
        int installmentsTotal
) {
}
