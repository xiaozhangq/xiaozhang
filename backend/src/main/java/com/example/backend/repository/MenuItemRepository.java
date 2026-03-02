package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.domain.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("""
            select m from MenuItem m
            join fetch m.category c
            where m.available = true and c.active = true
            order by c.sortOrder asc, m.id asc
            """)
    List<MenuItem> findAllAvailableItems();

    @Query("""
            select m from MenuItem m
            join fetch m.category c
            where m.available = true and c.active = true and c.id = :categoryId
            order by m.id asc
            """)
    List<MenuItem> findAvailableItemsByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
            select m from MenuItem m
            join fetch m.category c
            order by c.sortOrder asc, m.id asc
            """)
    List<MenuItem> findAllWithCategoryOrdered();

    @Query("""
            select m from MenuItem m
            join fetch m.category c
            where c.id = :categoryId
            order by m.id asc
            """)
    List<MenuItem> findAllByCategoryIdWithCategory(@Param("categoryId") Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}
