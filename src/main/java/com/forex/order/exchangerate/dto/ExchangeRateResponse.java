package com.forex.order.exchangerate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ExchangeRateResponse {

    private String currency;
    private BigDecimal tradeStanRate;
    private BigDecimal buyRate;
    private BigDecimal sellRate;
    private LocalDateTime dateTime;
}
