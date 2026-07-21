package com.gustavo.trackflowapp.modules.recurrence;

import com.gustavo.trackflowapp.modules.recurrence.dto.RecurrenceDataDTO;
import com.gustavo.trackflowapp.modules.recurrence.dto.RecurrenceRegisterDTO;
import com.gustavo.trackflowapp.modules.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("recurrences")
@RequiredArgsConstructor
public class RecurrenceController {
    private final RecurrenceService recurrenceService;

    @PostMapping
    public ResponseEntity<RecurrenceDataDTO> createRecurrence(@RequestBody @Valid RecurrenceRegisterDTO dto, @AuthenticationPrincipal User user, UriComponentsBuilder uriBuilder) {
        var recurrenceData = recurrenceService.createRecurrence(dto, user);
        var uri = uriBuilder.path("/recurrences/{id}").buildAndExpand(recurrenceData.id()).toUri();
        return ResponseEntity.created(uri).body(recurrenceData);
    }

    @GetMapping
    public ResponseEntity<Page<RecurrenceDataDTO>> listRecurrences(@AuthenticationPrincipal(expression = "id") Long userId, Pageable pageable) {
        return ResponseEntity.ok(recurrenceService.listRecurrence(pageable, userId));
    }
}
