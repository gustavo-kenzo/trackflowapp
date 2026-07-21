package com.gustavo.trackflowapp.modules.recurrence;

import com.gustavo.trackflowapp.modules.recurrence.dto.RecurrenceDataDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurrenceRepository extends JpaRepository<Recurrence, Long> {
    @Query("""
            SELECT r 
            FROM Recurrence r
            WHERE r.user.id = :userId
            """)
    Page<Recurrence> findRecurrences(Pageable pageable, @Param("userId") Long userId);
}
