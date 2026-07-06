package com.gustavo.trackflowapp.modules.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;


public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("""
            SELECT t
            FROM Transaction t
            WHERE t.user.id = :userId
            AND t.id = :transactionId
            """)
    Optional<Transaction> findMyTransactionById(Long transactionId, Long userId);

    @Query("""
            SELECT t
            FROM Transaction t
            WHERE t.user.id = :userId
            AND t.account.active = true
            AND (:categoryId IS NULL OR t.category.id = :categoryId)
            AND (:type IS NULL OR t.type = :type)
            AND (:status IS NULL OR t.status = :status)
            """)
    Page<Transaction> findMyTransactions(Long userId, Long categoryId, TransactionType type, TransactionStatus status, Pageable pageable);

    @Query("""
            SELECT t
            FROM Transaction t
            WHERE t.user.id = :userId
            AND t.account.active = false
            AND (:categoryId IS NULL OR t.category.id = :categoryId)
            AND (:type IS NULL OR t.type = :type)
            AND (:status IS NULL OR t.status = :status)
            """)
    Page<Transaction> findMyTransactionsInactive(Long userId, Long categoryId, TransactionType type, TransactionStatus status, Pageable pageable);

    @Modifying
    @Query("""
            DELETE FROM Transaction t
            WHERE t.id = :transactionId
            AND t.user.id = :userId
            """)
    int deleteMyTransaction(Long transactionId, Long userId);
}
