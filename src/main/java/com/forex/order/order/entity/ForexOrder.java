package com.forex.order.order.entity;

import com.forex.order.common.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "forex_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ForexOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal fromAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency fromCurrency;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal toAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency toCurrency;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal tradeRate;

    @Column(nullable = false)
    private LocalDateTime dateTime;
}
