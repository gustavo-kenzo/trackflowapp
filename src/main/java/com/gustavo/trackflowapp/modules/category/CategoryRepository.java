package com.gustavo.trackflowapp.modules.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("""
            SELECT c 
            FROM Category c
            WHERE c.user.id = :id
            AND c.active = true
            """)
    Page<Category> findCategoriesActive(@Param("id") Long id, Pageable pageable);

    @Query("""
            SELECT c 
            FROM Category c
            WHERE c.user.id = :id
            AND c.active = false
            """)
    Page<Category> findCategoriesInactive(@Param("id") Long id, Pageable pageable);

    @Query("""
            SELECT c 
            FROM Category c 
            WHERE c.user.id = :userId
            AND c.id = :categoryId
            """)
    Optional<Category> findCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId);


    @Modifying
    @Query("""
            DELETE FROM Category c
            WHERE c.id = :categoryId
            AND c.user.id = :userId
            """)
    int hardDelete(Long categoryId, Long userId);
}
