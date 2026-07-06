package com.gustavo.trackflowapp.modules.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Page<Account> findByUserNameIgnoreCaseAndActiveTrue(String owner, Pageable pageable);

    @Query("""
            SELECT c 
            FROM Account c
            JOIN c.user u
            WHERE u.id = :id
            """)
    Page<Account> findMyAccounts(@Param("id") Long id, Pageable pageable);

    @Query("""
            SELECT c 
            FROM Account c
            JOIN c.user u
            WHERE u.id = :userId
            AND c.id = :accountId
            """)
    Optional<Account> findMyAccount(@Param("accountId") Long accountId, @Param("userId") Long userId);

    @Modifying
    @Query("""
            DELETE FROM Account c
            WHERE c.id = :accountId
            AND c.user.id = :userId
            """)
    int deleteMyAccountAndReturnCount(@Param("accountId") Long accountId, @Param("userId") Long userId);
}
