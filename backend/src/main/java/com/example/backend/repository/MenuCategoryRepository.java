package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.domain.MenuCategory;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    List<MenuCategory> findByActiveTrueOrderBySortOrderAscIdAsc();

    List<MenuCategory> findAllByOrderBySortOrderAscIdAsc();
}
