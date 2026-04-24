package com.forex.order.order.service;

import com.forex.order.common.Currency;
import com.forex.order.common.OrderType;
import com.forex.order.common.exception.InvalidCurrencyException;
import com.forex.order.common.exception.RateNotAvailableException;
import com.forex.order.exchangerate.entity.ExchangeRateHistory;
import com.forex.order.exchangerate.service.ExchangeRateService;
import com.forex.order.order.dto.OrderRequest;
import com.forex.order.order.dto.OrderResponse;
import com.forex.order.order.entity.ForexOrder;
import com.forex.order.order.repository.ForexOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ForexOrderRepository orderRepository;
    private final ExchangeRateService exchangeRateService;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Currency currency = request.getCurrency();
        validateNotKrw(currency);

        ExchangeRateHistory rate;
        try {
            rate = exchangeRateService.getLatestRate(currency);
        } catch (Exception e) {
            throw new RateNotAvailableException(currency + " 통화의 환율이 준비되지 않았습니다");
        }

        BigDecimal tradeRate = (request.getOrderType() == OrderType.BUY)
                ? rate.getBuyRate()
                : rate.getSellRate();

        BigDecimal krwAmount = request.getForexAmount()
                .multiply(tradeRate)
                .divide(currency.getUnit(), 0, RoundingMode.FLOOR);

        ForexOrder order = buildOrder(request, currency, tradeRate, krwAmount);
        ForexOrder saved = orderRepository.save(order);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders() {
        return orderRepository.findAllByOrderByDateTimeDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateNotKrw(Currency currency) {
        if (currency == Currency.KRW) {
            throw new InvalidCurrencyException("KRW는 외화 주문 대상이 아닙니다");
        }
    }

    private ForexOrder buildOrder(OrderRequest request, Currency currency,
                                  BigDecimal tradeRate, BigDecimal krwAmount) {
        if (request.getOrderType() == OrderType.BUY) {
            return ForexOrder.builder()
                    .fromAmount(krwAmount)
                    .fromCurrency(Currency.KRW)
                    .toAmount(request.getForexAmount())
                    .toCurrency(currency)
                    .tradeRate(tradeRate)
                    .dateTime(LocalDateTime.now())
                    .build();
        }

        return ForexOrder.builder()
                .fromAmount(request.getForexAmount())
                .fromCurrency(currency)
                .toAmount(krwAmount)
                .toCurrency(Currency.KRW)
                .tradeRate(tradeRate)
                .dateTime(LocalDateTime.now())
                .build();
    }

    private OrderResponse toResponse(ForexOrder order) {
        return OrderResponse.builder()
                .id(order.getId())
                .fromAmount(order.getFromAmount())
                .fromCurrency(order.getFromCurrency().name())
                .toAmount(order.getToAmount())
                .toCurrency(order.getToCurrency().name())
                .tradeRate(order.getTradeRate())
                .dateTime(order.getDateTime())
                .build();
    }
}
