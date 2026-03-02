package com.example.backend.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.domain.CustomerOrder;
import com.example.backend.domain.CustomerOrderItem;
import com.example.backend.domain.MenuItem;
import com.example.backend.domain.OrderStatus;
import com.example.backend.dto.CreateOrderItemRequest;
import com.example.backend.dto.CreateOrderRequest;
import com.example.backend.repository.CustomerOrderRepository;
import com.example.backend.repository.MenuItemRepository;

@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderService(CustomerOrderRepository customerOrderRepository, MenuItemRepository menuItemRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional
    public CustomerOrder createOrder(CreateOrderRequest request) {
        Map<Long, Integer> quantityMap = aggregateQuantity(request.items());
        List<Long> menuItemIds = quantityMap.keySet().stream().toList();
        List<MenuItem> menuItems = menuItemRepository.findAllById(menuItemIds);
        if (menuItems.size() != menuItemIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "存在无效的菜品");
        }

        Map<Long, MenuItem> menuItemMap = menuItems.stream()
                .collect(Collectors.toMap(MenuItem::getId, item -> item));

        CustomerOrder order = new CustomerOrder();
        order.setCustomerName(request.customerName().trim());
        order.setCustomerPhone(request.customerPhone().trim());
        order.setDeliveryAddress(request.deliveryAddress().trim());
        order.setRemark(request.remark());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantityMap.entrySet()) {
            MenuItem menuItem = menuItemMap.get(entry.getKey());
            if (menuItem == null || !Boolean.TRUE.equals(menuItem.getAvailable())
                    || !Boolean.TRUE.equals(menuItem.getCategory().getActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "菜品不可下单: " + entry.getKey());
            }

            int quantity = entry.getValue();
            BigDecimal subtotal = menuItem.getPrice().multiply(BigDecimal.valueOf(quantity));

            CustomerOrderItem orderItem = new CustomerOrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItemId(menuItem.getId());
            orderItem.setMenuItemName(menuItem.getName());
            orderItem.setMenuItemPrice(menuItem.getPrice());
            orderItem.setQuantity(quantity);
            orderItem.setSubtotal(subtotal);
            order.getItems().add(orderItem);

            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        return customerOrderRepository.save(order);
    }

    private Map<Long, Integer> aggregateQuantity(List<CreateOrderItemRequest> items) {
        Map<Long, Integer> quantityMap = new LinkedHashMap<>();
        for (CreateOrderItemRequest item : items) {
            quantityMap.merge(item.menuItemId(), item.quantity(), Integer::sum);
        }
        return quantityMap;
    }
}
