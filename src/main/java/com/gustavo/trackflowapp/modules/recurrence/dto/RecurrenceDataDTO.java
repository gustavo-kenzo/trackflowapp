package com.gustavo.trackflowapp.modules.recurrence.dto;

import com.gustavo.trackflowapp.modules.recurrence.Recurrence;
import com.gustavo.trackflowapp.modules.recurrence.RecurrenceFrequency;

import java.time.LocalDate;

public record RecurrenceDataDTO(
        Long id,
        String owner,
        RecurrenceFrequency frequency,
        LocalDate startDate,
        LocalDate endDate,
        int installmentsTotal
) {
    public RecurrenceDataDTO(Recurrence recurrence) {
        this(
                recurrence.getId(),
                recurrence.getUser().getName(),
                recurrence.getFrequency(),
                recurrence.getStartDate(),
                recurrence.getEndDate(),
                recurrence.getInstallmentsTotal()
        );
    }
}
