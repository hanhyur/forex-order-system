package com.forex.order.order.service;

import com.forex.order.common.Currency;
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
        validateCurrencyPair(request.getFromCurrency(), request.getToCurrency());

        boolean isBuy = request.getFromCurrency() == Currency.KRW;
        Currency foreignCurrency = isBuy ? request.getToCurrency() : request.getFromCurrency();

        ExchangeRateHistory rate;
        try {
            rate = exchangeRateService.getLatestRate(foreignCurrency);
        } catch (Exception e) {
            throw new RateNotAvailableException(foreignCurrency + " 통화의 환율이 준비되지 않았습니다");
        }

        BigDecimal tradeRate = isBuy ? rate.getBuyRate() : rate.getSellRate();

        BigDecimal krwAmount = request.getForexAmount()
                .multiply(tradeRate)
                .divide(foreignCurrency.getUnit(), 0, RoundingMode.FLOOR);

        ForexOrder order = buildOrder(request, tradeRate, krwAmount, isBuy);
        ForexOrder saved = orderRepository.save(order);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders() {
        return orderRepository.findAllByOrderByDateTimeDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateCurrencyPair(Currency from, Currency to) {
        if (from == to) {
            throw new InvalidCurrencyException("출발 통화와 도착 통화가 같을 수 없습니다");
        }

        boolean hasKrw = from == Currency.KRW || to == Currency.KRW;
        if (!hasKrw) {
            throw new InvalidCurrencyException("모든 주문은 KRW를 기준으로 진행해야 합니다");
        }

        Currency foreignCurrency = (from == Currency.KRW) ? to : from;
        if (foreignCurrency == Currency.KRW) {
            throw new InvalidCurrencyException("외화 통화를 지정해야 합니다");
        }
    }

    private ForexOrder buildOrder(OrderRequest request, BigDecimal tradeRate,
                                  BigDecimal krwAmount, boolean isBuy) {
        if (isBuy) {
            return ForexOrder.builder()
                    .fromAmount(krwAmount)
                    .fromCurrency(Currency.KRW)
                    .toAmount(request.getForexAmount())
                    .toCurrency(request.getToCurrency())
                    .tradeRate(tradeRate)
                    .dateTime(LocalDateTime.now())
                    .build();
        }

        return ForexOrder.builder()
                .fromAmount(request.getForexAmount())
                .fromCurrency(request.getFromCurrency())
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
