package com.forex.order.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private BigDecimal fromAmount;
    private String fromCurrency;
    private BigDecimal toAmount;
    private String toCurrency;
    private BigDecimal tradeRate;
    private LocalDateTime dateTime;
}
