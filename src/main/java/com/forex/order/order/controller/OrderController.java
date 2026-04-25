package com.forex.order.order.controller;

import com.forex.order.common.ApiResponse;
import com.forex.order.order.dto.OrderListResponse;
import com.forex.order.order.dto.OrderRequest;
import com.forex.order.order.dto.OrderResponse;
import com.forex.order.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrders() {
        List<OrderResponse> orders = orderService.getOrders();
        return ResponseEntity.ok(ApiResponse.ok(new OrderListResponse(orders)));
    }
}
