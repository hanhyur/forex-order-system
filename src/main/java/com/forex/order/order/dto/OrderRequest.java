package com.forex.order.order.dto;

import com.forex.order.common.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull(message = "금액은 필수입니다")
    @Positive(message = "금액은 양수여야 합니다")
    private BigDecimal forexAmount;

    @NotNull(message = "출발 통화는 필수입니다")
    private Currency fromCurrency;

    @NotNull(message = "도착 통화는 필수입니다")
    private Currency toCurrency;
}
