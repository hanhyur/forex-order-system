package com.forex.order.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OrderListResponse {

    private List<OrderResponse> orderList;
}
