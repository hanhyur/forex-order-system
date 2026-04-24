package com.forex.order.order.service;

import com.forex.order.exchangerate.service.ExchangeRateService;
import com.forex.order.order.dto.OrderRequest;
import com.forex.order.order.dto.OrderResponse;
import com.forex.order.order.repository.ForexOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ForexOrderRepository orderRepository;
    private final ExchangeRateService exchangeRateService;

    public OrderResponse createOrder(OrderRequest request) {
        // TODO: 구현 예정
        return null;
    }

    public List<OrderResponse> getOrders() {
        // TODO: 구현 예정
        return List.of();
    }
}
