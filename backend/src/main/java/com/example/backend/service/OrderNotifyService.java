package com.example.backend.service;

import com.example.backend.dto.OrderDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderNotifyService {

    public static final String TOPIC_ORDERS = "/topic/orders";

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private WeChatNotifyService weChatNotifyService;

    public OrderNotifyService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyNewOrder(OrderDto order) {
        messagingTemplate.convertAndSend(TOPIC_ORDERS, new OrderMessage("NEW", order));
        if (weChatNotifyService != null) {
            weChatNotifyService.sendOrderNotify(order);
        }
    }

    public void notifyStatusChanged(OrderDto order) {
        messagingTemplate.convertAndSend(TOPIC_ORDERS, new OrderMessage("STATUS", order));
    }

    public record OrderMessage(String type, OrderDto order) {
    }
}
