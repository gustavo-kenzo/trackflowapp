package com.gustavo.trackflowapp.modules.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
