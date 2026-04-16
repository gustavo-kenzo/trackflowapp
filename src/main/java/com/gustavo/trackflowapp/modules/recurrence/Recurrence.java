package com.gustavo.trackflowapp.modules.recurrence;

import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "recurrences")
@Getter
@NoArgsConstructor
public class Recurrence extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private RecurrenceFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "installments_total", nullable = false)
    private int installmentsTotal;

    public Recurrence(User user,
                      RecurrenceFrequency frequency,
                      LocalDate startDate,
                      int installmentsTotal) {
        this.user = user;
        this.frequency = frequency;
        this.startDate = startDate != null ? startDate : LocalDate.now();
        if (installmentsTotal <= 0) throw new IllegalArgumentException("installments total must be positive");
        this.installmentsTotal = installmentsTotal;
        this.endDate = defineEndDate();
    }

    // CORREÇÃO: Cálculo de endDate corrigido para usar installmentsTotal - 1
    private LocalDate defineEndDate() {
        return
                switch (this.frequency) {
                    case DAILY -> this.startDate.plusDays(installmentsTotal - 1);
                    case WEEKLY -> this.startDate.plusWeeks(installmentsTotal - 1);
                    case MONTHLY -> this.startDate.plusMonths(installmentsTotal - 1);
                    case YEARLY -> this.startDate.plusYears(installmentsTotal - 1);
                };
    } // FIM CORREÇÃO
}
