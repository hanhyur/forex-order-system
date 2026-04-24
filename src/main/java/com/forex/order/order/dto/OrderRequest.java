package com.forex.order.order.dto;

import com.forex.order.common.Currency;
import com.forex.order.common.OrderType;
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

    @NotNull(message = "주문 유형은 필수입니다")
    private OrderType orderType;

    @NotNull(message = "통화는 필수입니다")
    private Currency currency;

    @NotNull(message = "금액은 필수입니다")
    @Positive(message = "금액은 양수여야 합니다")
    private BigDecimal forexAmount;
}
