package com.forex.order.order.service;

import com.forex.order.common.Currency;
import com.forex.order.common.OrderType;
import com.forex.order.common.exception.InvalidCurrencyException;
import com.forex.order.common.exception.RateNotAvailableException;
import com.forex.order.common.exception.RateNotFoundException;
import com.forex.order.exchangerate.entity.ExchangeRateHistory;
import com.forex.order.exchangerate.service.ExchangeRateService;
import com.forex.order.order.dto.OrderRequest;
import com.forex.order.order.dto.OrderResponse;
import com.forex.order.order.entity.ForexOrder;
import com.forex.order.order.repository.ForexOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ForexOrderRepository orderRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("매수 주문 (BUY: KRW -> 외화)")
    class BuyOrder {

        @Test
        @DisplayName("USD 100 매수 시 buyRate를 적용하여 KRW 금액을 계산한다")
        void calculatesKrwWithBuyRate() {
            // Arrange: 100 USD, buyRate 1412.78 → KRW = 100 * 1412.78 / 1 = 141278
            givenRate(Currency.USD, "1412.78", "1278.23");
            givenSavedOrder();
            OrderRequest request = new OrderRequest(OrderType.BUY, Currency.USD, new BigDecimal("100"));

            // Act
            OrderResponse response = orderService.createOrder(request);

            // Assert
            assertThat(response.getFromCurrency()).isEqualTo("KRW");
            assertThat(response.getFromAmount()).isEqualByComparingTo("141278");
            assertThat(response.getToCurrency()).isEqualTo("USD");
            assertThat(response.getToAmount()).isEqualByComparingTo("100");
            assertThat(response.getTradeRate()).isEqualByComparingTo("1412.78");
        }

        @Test
        @DisplayName("KRW 금액의 소수점을 버림(FLOOR)한다")
        void floorsKrwAmount() {
            // Arrange: 33 USD, buyRate 1412.78 → 33 * 1412.78 = 46621.74 → FLOOR → 46621
            givenRate(Currency.USD, "1412.78", "1278.23");
            givenSavedOrder();
            OrderRequest request = new OrderRequest(OrderType.BUY, Currency.USD, new BigDecimal("33"));

            // Act
            OrderResponse response = orderService.createOrder(request);

            // Assert
            assertThat(response.getFromAmount()).isEqualByComparingTo("46621");
        }

        @Test
        @DisplayName("JPY 매수 시 100엔 단위(unit=100)를 적용한다")
        void appliesJpyUnit() {
            // Arrange: 1000 JPY, buyRate 963.31 → 1000 * 963.31 / 100 = 9633.1 → FLOOR → 9633
            givenRate(Currency.JPY, "963.31", "871.57");
            givenSavedOrder();
            OrderRequest request = new OrderRequest(OrderType.BUY, Currency.JPY, new BigDecimal("1000"));

            // Act
            OrderResponse response = orderService.createOrder(request);

            // Assert
            assertThat(response.getFromAmount()).isEqualByComparingTo("9633");
        }
    }

    @Nested
    @DisplayName("매도 주문 (SELL: 외화 -> KRW)")
    class SellOrder {

        @Test
        @DisplayName("USD 100 매도 시 sellRate를 적용하여 KRW 금액을 계산한다")
        void calculatesKrwWithSellRate() {
            // Arrange: 100 USD, sellRate 1278.23 → KRW = 100 * 1278.23 / 1 = 127823
            givenRate(Currency.USD, "1412.78", "1278.23");
            givenSavedOrder();
            OrderRequest request = new OrderRequest(OrderType.SELL, Currency.USD, new BigDecimal("100"));

            // Act
            OrderResponse response = orderService.createOrder(request);

            // Assert
            assertThat(response.getFromCurrency()).isEqualTo("USD");
            assertThat(response.getFromAmount()).isEqualByComparingTo("100");
            assertThat(response.getToCurrency()).isEqualTo("KRW");
            assertThat(response.getToAmount()).isEqualByComparingTo("127823");
            assertThat(response.getTradeRate()).isEqualByComparingTo("1278.23");
        }

        @Test
        @DisplayName("JPY 매도 시 100엔 단위(unit=100)를 적용한다")
        void appliesJpyUnit() {
            // Arrange: 5000 JPY, sellRate 871.57 → 5000 * 871.57 / 100 = 43578.5 → FLOOR → 43578
            givenRate(Currency.JPY, "963.31", "871.57");
            givenSavedOrder();
            OrderRequest request = new OrderRequest(OrderType.SELL, Currency.JPY, new BigDecimal("5000"));

            // Act
            OrderResponse response = orderService.createOrder(request);

            // Assert
            assertThat(response.getToAmount()).isEqualByComparingTo("43578");
        }
    }

    @Nested
    @DisplayName("주문 검증")
    class Validation {

        @Test
        @DisplayName("KRW 통화로 주문하면 InvalidCurrencyException을 던진다")
        void rejectsKrwOrder() {
            OrderRequest request = new OrderRequest(OrderType.BUY, Currency.KRW, new BigDecimal("100"));

            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(InvalidCurrencyException.class)
                    .hasMessageContaining("KRW");
        }

        @Test
        @DisplayName("환율이 없으면 RateNotAvailableException을 던진다")
        void rejectsWhenNoRate() {
            given(exchangeRateService.getLatestRate(Currency.USD))
                    .willThrow(new RateNotFoundException("USD 환율 없음"));

            OrderRequest request = new OrderRequest(OrderType.BUY, Currency.USD, new BigDecimal("100"));

            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(RateNotAvailableException.class);
        }
    }

    @Nested
    @DisplayName("주문 목록 조회")
    class GetOrders {

        @Test
        @DisplayName("주문이 없으면 빈 목록을 반환한다")
        void returnsEmptyList() {
            given(orderRepository.findAllByOrderByDateTimeDesc()).willReturn(List.of());

            List<OrderResponse> result = orderService.getOrders();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("주문 목록을 OrderResponse로 변환하여 반환한다")
        void returnsOrderResponses() {
            ForexOrder order = ForexOrder.builder()
                    .id(1L)
                    .fromAmount(new BigDecimal("141278"))
                    .fromCurrency(Currency.KRW)
                    .toAmount(new BigDecimal("100"))
                    .toCurrency(Currency.USD)
                    .tradeRate(new BigDecimal("1412.78"))
                    .dateTime(LocalDateTime.now())
                    .build();

            given(orderRepository.findAllByOrderByDateTimeDesc()).willReturn(List.of(order));

            List<OrderResponse> result = orderService.getOrders();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFromCurrency()).isEqualTo("KRW");
            assertThat(result.get(0).getToCurrency()).isEqualTo("USD");
        }
    }

    private void givenRate(Currency currency, String buyRate, String sellRate) {
        ExchangeRateHistory rate = ExchangeRateHistory.builder()
                .currency(currency)
                .tradeStanRate(new BigDecimal("1345.50"))
                .buyRate(new BigDecimal(buyRate))
                .sellRate(new BigDecimal(sellRate))
                .dateTime(LocalDateTime.now())
                .build();

        given(exchangeRateService.getLatestRate(currency)).willReturn(rate);
    }

    private void givenSavedOrder() {
        given(orderRepository.save(any(ForexOrder.class))).willAnswer(invocation -> {
            ForexOrder order = invocation.getArgument(0);
            return ForexOrder.builder()
                    .id(1L)
                    .fromAmount(order.getFromAmount())
                    .fromCurrency(order.getFromCurrency())
                    .toAmount(order.getToAmount())
                    .toCurrency(order.getToCurrency())
                    .tradeRate(order.getTradeRate())
                    .dateTime(order.getDateTime())
                    .build();
        });
    }
}
