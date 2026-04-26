package com.forex.order.common;

import java.math.BigDecimal;

public enum Currency {

    USD(1, true),
    JPY(100, true),
    CNY(1, true),
    EUR(1, true),
    KRW(1, false);

    private final int unit;
    private final boolean forex;

    Currency(int unit, boolean forex) {
        this.unit = unit;
        this.forex = forex;
    }

    public BigDecimal getUnit() {
        return BigDecimal.valueOf(unit);
    }

    public boolean isForex() {
        return forex;
    }
}
