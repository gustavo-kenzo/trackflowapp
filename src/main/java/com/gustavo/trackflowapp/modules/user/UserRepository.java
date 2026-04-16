package com.gustavo.trackflowapp.modules.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findAll(Pageable pageable);

    @Modifying
    @Query("delete from User u where u.id = :id")
    int deleteByIdAndReturnCount(@Param("id") Long id);

    User findByEmail(String email);
}
