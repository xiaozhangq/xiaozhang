package com.example.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.domain.CustomerOrder;
import com.example.backend.domain.MenuItem;
import com.example.backend.dto.CreateOrderRequest;
import com.example.backend.dto.CreateOrderResponse;
import com.example.backend.dto.MenuCategoryDto;
import com.example.backend.dto.MenuItemDto;
import com.example.backend.repository.MenuCategoryRepository;
import com.example.backend.repository.MenuItemRepository;
import com.example.backend.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderService orderService;

    public PublicController(
            MenuCategoryRepository menuCategoryRepository,
            MenuItemRepository menuItemRepository,
            OrderService orderService) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderService = orderService;
    }

    @GetMapping("/categories")
    public List<MenuCategoryDto> listActiveCategories() {
        return menuCategoryRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(MenuCategoryDto::from)
                .toList();
    }

    @GetMapping("/menu-items")
    public List<MenuItemDto> listAvailableMenuItems(@RequestParam(required = false) Long categoryId) {
        List<MenuItem> menuItems = categoryId == null
                ? menuItemRepository.findAllAvailableItems()
                : menuItemRepository.findAvailableItemsByCategoryId(categoryId);
        return menuItems.stream().map(MenuItemDto::from).toList();
    }

    @PostMapping("/orders")
    public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CustomerOrder order = orderService.createOrder(request);
        return new CreateOrderResponse(order.getId(), order.getTotalAmount(), "下单成功");
    }
}
