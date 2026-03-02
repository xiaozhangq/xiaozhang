package com.example.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.domain.MenuCategory;
import com.example.backend.dto.AdminCategoryRequest;
import com.example.backend.dto.MenuCategoryDto;
import com.example.backend.repository.MenuCategoryRepository;
import com.example.backend.repository.MenuItemRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    public AdminCategoryController(MenuCategoryRepository menuCategoryRepository, MenuItemRepository menuItemRepository) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @GetMapping
    public List<MenuCategoryDto> listCategories() {
        return menuCategoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(MenuCategoryDto::from)
                .toList();
    }

    @PostMapping
    public MenuCategoryDto createCategory(@Valid @RequestBody AdminCategoryRequest request) {
        MenuCategory category = new MenuCategory();
        applyRequest(category, request);
        return MenuCategoryDto.from(menuCategoryRepository.save(category));
    }

    @PutMapping("/{id}")
    public MenuCategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody AdminCategoryRequest request) {
        MenuCategory category = menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "分类不存在"));
        applyRequest(category, request);
        return MenuCategoryDto.from(menuCategoryRepository.save(category));
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        if (!menuCategoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "分类不存在");
        }
        if (menuItemRepository.existsByCategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分类下还有菜品，无法删除");
        }
        menuCategoryRepository.deleteById(id);
    }

    private void applyRequest(MenuCategory category, AdminCategoryRequest request) {
        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.active());
    }
}
