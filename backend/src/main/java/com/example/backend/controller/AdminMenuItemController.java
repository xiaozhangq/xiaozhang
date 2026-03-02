package com.example.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.domain.MenuCategory;
import com.example.backend.domain.MenuItem;
import com.example.backend.dto.AdminMenuItemRequest;
import com.example.backend.dto.MenuItemDto;
import com.example.backend.repository.MenuCategoryRepository;
import com.example.backend.repository.MenuItemRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/menu-items")
public class AdminMenuItemController {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    public AdminMenuItemController(MenuItemRepository menuItemRepository, MenuCategoryRepository menuCategoryRepository) {
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
    }

    @GetMapping
    public List<MenuItemDto> listMenuItems(@RequestParam(required = false) Long categoryId) {
        List<MenuItem> menuItems = categoryId == null
                ? menuItemRepository.findAllWithCategoryOrdered()
                : menuItemRepository.findAllByCategoryIdWithCategory(categoryId);
        return menuItems.stream().map(MenuItemDto::from).toList();
    }

    @PostMapping
    @Transactional
    public MenuItemDto createMenuItem(@Valid @RequestBody AdminMenuItemRequest request) {
        MenuItem menuItem = new MenuItem();
        applyRequest(menuItem, request);
        return MenuItemDto.from(menuItemRepository.save(menuItem));
    }

    @PutMapping("/{id}")
    @Transactional
    public MenuItemDto updateMenuItem(@PathVariable Long id, @Valid @RequestBody AdminMenuItemRequest request) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "菜品不存在"));
        applyRequest(menuItem, request);
        return MenuItemDto.from(menuItemRepository.save(menuItem));
    }

    @DeleteMapping("/{id}")
    public void deleteMenuItem(@PathVariable Long id) {
        if (!menuItemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "菜品不存在");
        }
        menuItemRepository.deleteById(id);
    }

    private void applyRequest(MenuItem menuItem, AdminMenuItemRequest request) {
        MenuCategory category = menuCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "分类不存在"));
        menuItem.setCategory(category);
        menuItem.setName(request.name().trim());
        menuItem.setDescription(request.description() != null && !request.description().isBlank()
                ? request.description().trim() : null);
        menuItem.setPrice(request.price());
        menuItem.setImageUrl(request.imageUrl() != null && !request.imageUrl().isBlank()
                ? request.imageUrl().trim() : null);
        menuItem.setAvailable(request.available());
    }
}
