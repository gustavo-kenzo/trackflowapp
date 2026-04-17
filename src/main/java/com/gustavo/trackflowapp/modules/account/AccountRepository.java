package com.gustavo.trackflowapp.modules.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RequestParam;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Page<Account> findByUserNameIgnoreCaseAndActiveTrue(String owner, Pageable pageable);

    Page<Account> findByActiveTrue(Pageable pageable);

    @Modifying
    @Query("delete from Account c where c.id = :id")
    int deleteByIdAndReturnCount(@RequestParam("id") Long id);
}
