package com.example.backend.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.domain.CustomerOrder;
import com.example.backend.domain.OrderStatus;
import com.example.backend.dto.OrderDto;
import com.example.backend.dto.UpdateOrderStatusRequest;
import com.example.backend.repository.CustomerOrderRepository;
import com.example.backend.service.OrderNotifyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderNotifyService orderNotifyService;

    public AdminOrderController(CustomerOrderRepository customerOrderRepository,
                                OrderNotifyService orderNotifyService) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderNotifyService = orderNotifyService;
    }

    @GetMapping
    public List<OrderDto> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);
        return customerOrderRepository.findByStatusAndCreatedAtBetween(status, startTime, endTime).stream()
                .map(OrderDto::from)
                .toList();
    }

    @PutMapping("/{id}/status")
    public OrderDto updateOrderStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        order.setStatus(request.status());
        OrderDto dto = OrderDto.from(customerOrderRepository.save(order));
        orderNotifyService.notifyStatusChanged(dto);
        return dto;
    }
}
