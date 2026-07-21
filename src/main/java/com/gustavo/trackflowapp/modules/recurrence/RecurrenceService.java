package com.gustavo.trackflowapp.modules.recurrence;

import com.gustavo.trackflowapp.modules.recurrence.dto.RecurrenceDataDTO;
import com.gustavo.trackflowapp.modules.recurrence.dto.RecurrenceRegisterDTO;
import com.gustavo.trackflowapp.modules.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Deprecated
@Service
@RequiredArgsConstructor
public class RecurrenceService {

    private final RecurrenceRepository recurrenceRepository;

    @Transactional
    public RecurrenceDataDTO createRecurrence(RecurrenceRegisterDTO dto, User user) {
        var recurrence = new Recurrence(user, dto.frequency(), dto.startDate(), dto.endDate(), dto.installmentsTotal());
        recurrenceRepository.save(recurrence);
        return new RecurrenceDataDTO(recurrence);
    }

    public Page<RecurrenceDataDTO> listRecurrence(Pageable pageable, Long userId) {
        return recurrenceRepository.findRecurrences(pageable, userId).map(RecurrenceDataDTO::new);
    }
}
