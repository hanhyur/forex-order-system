package com.forex.order.common;

import java.math.BigDecimal;

public enum Currency {

    USD(1),
    JPY(100),
    CNY(1),
    EUR(1),
    KRW(1);

    private final int unit;

    Currency(int unit) {
        this.unit = unit;
    }

    public BigDecimal getUnit() {
        return BigDecimal.valueOf(unit);
    }
}
