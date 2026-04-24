package com.forex.order.exchangerate.repository;

import com.forex.order.common.Currency;
import com.forex.order.exchangerate.entity.ExchangeRateHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ExchangeRateHistoryRepositoryTest {

    @Autowired
    private ExchangeRateHistoryRepository repository;

    @Test
    @DisplayName("같은 통화의 여러 환율 중 가장 최신 1건만 반환한다")
    void findsLatestByCurrency() {
        // Arrange
        ExchangeRateHistory older = ExchangeRateHistory.builder()
                .currency(Currency.USD)
                .tradeStanRate(new BigDecimal("1340.00"))
                .buyRate(new BigDecimal("1407.00"))
                .sellRate(new BigDecimal("1273.00"))
                .dateTime(LocalDateTime.of(2026, 4, 24, 10, 0))
                .build();

        ExchangeRateHistory newer = ExchangeRateHistory.builder()
                .currency(Currency.USD)
                .tradeStanRate(new BigDecimal("1345.50"))
                .buyRate(new BigDecimal("1412.78"))
                .sellRate(new BigDecimal("1278.23"))
                .dateTime(LocalDateTime.of(2026, 4, 24, 11, 0))
                .build();

        repository.save(older);
        repository.save(newer);

        // Act
        Optional<ExchangeRateHistory> result =
                repository.findTopByCurrencyOrderByDateTimeDesc(Currency.USD);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getTradeStanRate()).isEqualByComparingTo("1345.50");
    }

    @Test
    @DisplayName("통화별로 독립적으로 최신 환율을 조회한다")
    void findsByCurrencyIndependently() {
        // Arrange
        ExchangeRateHistory usd = ExchangeRateHistory.builder()
                .currency(Currency.USD)
                .tradeStanRate(new BigDecimal("1345.50"))
                .buyRate(new BigDecimal("1412.78"))
                .sellRate(new BigDecimal("1278.23"))
                .dateTime(LocalDateTime.now())
                .build();

        ExchangeRateHistory jpy = ExchangeRateHistory.builder()
                .currency(Currency.JPY)
                .tradeStanRate(new BigDecimal("917.44"))
                .buyRate(new BigDecimal("963.31"))
                .sellRate(new BigDecimal("871.57"))
                .dateTime(LocalDateTime.now())
                .build();

        repository.save(usd);
        repository.save(jpy);

        // Act
        Optional<ExchangeRateHistory> usdResult =
                repository.findTopByCurrencyOrderByDateTimeDesc(Currency.USD);
        Optional<ExchangeRateHistory> jpyResult =
                repository.findTopByCurrencyOrderByDateTimeDesc(Currency.JPY);

        // Assert
        assertThat(usdResult.get().getCurrency()).isEqualTo(Currency.USD);
        assertThat(jpyResult.get().getCurrency()).isEqualTo(Currency.JPY);
    }

    @Test
    @DisplayName("데이터가 없으면 빈 Optional을 반환한다")
    void returnsEmptyWhenNone() {
        // Act
        Optional<ExchangeRateHistory> result =
                repository.findTopByCurrencyOrderByDateTimeDesc(Currency.EUR);

        // Assert
        assertThat(result).isEmpty();
    }
}
