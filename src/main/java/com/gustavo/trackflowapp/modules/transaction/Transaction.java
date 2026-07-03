package com.gustavo.trackflowapp.modules.transaction;

import com.gustavo.trackflowapp.modules.account.Account;
import com.gustavo.trackflowapp.modules.category.Category;
import com.gustavo.trackflowapp.modules.recurrence.Recurrence;
import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor
public class Transaction extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "recurrence_id")
    private Recurrence recurrence;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "competence_date", nullable = false)
    private LocalDate competenceDate;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "description")
    private String description;

    public Transaction(User user,
                       Account account,
                       Category category,
                       Recurrence recurrence,
                       BigDecimal amount,
                       TransactionType type,
                       LocalDate competenceDate,
                       LocalDate settlementDate,
                       String description) {
        validateNonNull(List.of(user, account, type, competenceDate));

        validateOwnership(category, user, Category::getUser, "category does not belong to this user");
        validateOwnership(account, user, Account::getUser, "this account does not belong to this user");
        validateOwnership(recurrence, user, Recurrence::getUser, "this recurrence does not belong to this user");


        this.user = user;
        this.account = account;
        this.category = category;
        this.recurrence = recurrence;

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("amount must be positive");
        this.amount = amount;
        this.type = type;
        this.competenceDate = competenceDate;
        this.settlementDate = settlementDate;
        if (settlementDate != null) {
            if (settlementDate.isBefore(competenceDate))
                throw new IllegalArgumentException("settlementDate cannot be before competenceDate");
            if (settlementDate.isAfter(LocalDate.now()))
                throw new IllegalArgumentException("settlementDate shouldn't be future");
        }
        this.description = description;
        this.status = this.settlementDate != null ? TransactionStatus.SETTLED : TransactionStatus.PENDING;
    }

    private <T> void validateNonNull(List<T> obj) {
        obj.forEach(ob -> Objects.requireNonNull(ob, "Required field is missing"));
    }

    private <T> void validateOwnership(T entity, User user, Function<T, User> userExtractor, String errorMessage) {
        if (entity != null) {
            var owner = userExtractor.apply(entity);
            if (owner == null || !owner.getId().equals(user.getId()))
                throw new IllegalArgumentException(errorMessage);
        }
    }

    public void settle(LocalDate settlementDate) {
        if (this.status == TransactionStatus.SETTLED)
            throw new IllegalStateException("transaction already settled");
        var date = settlementDate != null ? settlementDate : LocalDate.now();
        if (date.isBefore(this.competenceDate))
            throw new IllegalStateException("settlementDate cannot be before competenceDate");
        if(date.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("settlementDate shouldn't be future");
        this.status = TransactionStatus.SETTLED;
        this.settlementDate = date;
    }

    public void reopen() {
        if (this.status == TransactionStatus.PENDING) throw new IllegalStateException("transaction already pending");
        this.status = TransactionStatus.PENDING;
        this.settlementDate = null;
    }

    public void update(Account account,
                       Category category,
                       BigDecimal amount,
                       TransactionType type,
                       LocalDate competenceDate,
                       String description) {
        if (account != null)
            this.account = account;
        if (category != null)
            this.category = category;
        if (amount != null)
            this.amount = amount;
        if (type != null)
            this.type = type;
        if (competenceDate != null)
            this.competenceDate = competenceDate;
        if (description != null)
            this.description = description;
    }

}
