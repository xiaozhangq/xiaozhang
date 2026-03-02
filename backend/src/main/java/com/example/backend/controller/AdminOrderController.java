package com.example.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.domain.CustomerOrder;
import com.example.backend.dto.OrderDto;
import com.example.backend.dto.UpdateOrderStatusRequest;
import com.example.backend.repository.CustomerOrderRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final CustomerOrderRepository customerOrderRepository;

    public AdminOrderController(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    @GetMapping
    public List<OrderDto> listOrders() {
        return customerOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(OrderDto::from)
                .toList();
    }

    @PutMapping("/{id}/status")
    public OrderDto updateOrderStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        order.setStatus(request.status());
        return OrderDto.from(customerOrderRepository.save(order));
    }
}
