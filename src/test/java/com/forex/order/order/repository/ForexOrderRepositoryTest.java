package com.forex.order.order.repository;

import com.forex.order.common.Currency;
import com.forex.order.order.entity.ForexOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ForexOrderRepositoryTest {

    @Autowired
    private ForexOrderRepository forexOrderRepository;

    @Test
    @DisplayName("주문 목록을 최신순(dateTime DESC)으로 반환한다")
    void returnsOrdersByDateTimeDesc() {
        // Arrange
        ForexOrder older = ForexOrder.builder()
                .fromAmount(new BigDecimal("141278"))
                .fromCurrency(Currency.KRW)
                .toAmount(new BigDecimal("100"))
                .toCurrency(Currency.USD)
                .tradeRate(new BigDecimal("1412.78"))
                .dateTime(LocalDateTime.of(2026, 4, 24, 10, 0))
                .build();

        ForexOrder newer = ForexOrder.builder()
                .fromAmount(new BigDecimal("127823"))
                .fromCurrency(Currency.USD)
                .toAmount(new BigDecimal("100"))
                .toCurrency(Currency.KRW)
                .tradeRate(new BigDecimal("1278.23"))
                .dateTime(LocalDateTime.of(2026, 4, 24, 14, 0))
                .build();

        forexOrderRepository.save(older);
        forexOrderRepository.save(newer);

        // Act
        List<ForexOrder> result = forexOrderRepository.findAllByOrderByDateTimeDesc();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDateTime()).isAfter(result.get(1).getDateTime());
    }

    @Test
    @DisplayName("주문이 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenNoOrders() {
        List<ForexOrder> result = forexOrderRepository.findAllByOrderByDateTimeDesc();

        assertThat(result).isEmpty();
    }
}
