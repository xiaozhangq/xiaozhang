package com.example.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.domain.CustomerOrder;
import com.example.backend.dto.CreateOrderRequest;
import com.example.backend.dto.CreateOrderResponse;
import com.example.backend.dto.OrderDto;
import com.example.backend.service.OrderNotifyService;
import com.example.backend.service.OrderService;

import jakarta.validation.Valid;

/**
 * 已登录用户下单，未登录不允许下单。
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerOrderController {

    private final OrderService orderService;
    private final OrderNotifyService orderNotifyService;

    public CustomerOrderController(OrderService orderService, OrderNotifyService orderNotifyService) {
        this.orderService = orderService;
        this.orderNotifyService = orderNotifyService;
    }

    @PostMapping("/orders")
    public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CustomerOrder order = orderService.createOrder(request);
        orderNotifyService.notifyNewOrder(OrderDto.from(order));
        return new CreateOrderResponse(order.getId(), order.getTotalAmount(), "下单成功");
    }
}
