package com.gustavo.trackflowapp.modules.bill;

import com.gustavo.trackflowapp.modules.transaction.Transaction;
import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "bills")
@Getter
@NoArgsConstructor
public class Bill extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BillType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BillStatus status;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    public Bill(Transaction transaction,
                User user,
                BillType type,
                LocalDate dueDate) {
        this.transaction = transaction;
        this.user = user;
        this.type = type;
        this.status = BillStatus.PENDING;
        this.dueDate = dueDate != null ? dueDate : LocalDate.now();
    }

    public void settle(LocalDate paidDate) {
        if (this.status == BillStatus.SETTLED) throw new IllegalStateException("bill already settled");
        this.status = BillStatus.SETTLED;
        this.paidDate = paidDate != null ? paidDate : LocalDate.now();
    }

    public void reopen() {
        if (this.status == BillStatus.PENDING) throw new IllegalStateException("bill already pending");
        this.status = BillStatus.PENDING;
        this.paidDate = null;
    }

}
