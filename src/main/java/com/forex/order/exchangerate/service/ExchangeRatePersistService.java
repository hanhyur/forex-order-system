package com.forex.order.exchangerate.service;

import com.forex.order.common.Currency;
import com.forex.order.exchangerate.dto.KoreaEximApiResponse;
import com.forex.order.exchangerate.entity.ExchangeRateHistory;
import com.forex.order.exchangerate.repository.ExchangeRateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRatePersistService {

    private static final BigDecimal BUY_SPREAD_RATE = new BigDecimal("1.05");
    private static final BigDecimal SELL_SPREAD_RATE = new BigDecimal("0.95");
    private static final Set<Currency> TARGET_CURRENCIES = Set.of(
            Currency.USD, Currency.JPY, Currency.CNY, Currency.EUR
    );

    private final ExchangeRateHistoryRepository repository;

    @Transactional
    public List<ExchangeRateHistory> processAndSave(List<KoreaEximApiResponse> responses) {
        List<ExchangeRateHistory> saved = new ArrayList<>();

        for (KoreaEximApiResponse response : responses) {
            Currency currency = parseCurrency(response.getCurUnit());
            if (currency == null || !TARGET_CURRENCIES.contains(currency)) {
                continue;
            }

            BigDecimal tradeStanRate = parseRate(response.getDealBasR());
            BigDecimal buyRate = tradeStanRate.multiply(BUY_SPREAD_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal sellRate = tradeStanRate.multiply(SELL_SPREAD_RATE)
                    .setScale(2, RoundingMode.HALF_UP);

            ExchangeRateHistory entity = ExchangeRateHistory.builder()
                    .currency(currency)
                    .tradeStanRate(tradeStanRate)
                    .buyRate(buyRate)
                    .sellRate(sellRate)
                    .dateTime(LocalDateTime.now())
                    .build();

            saved.add(repository.save(entity));
        }

        return saved;
    }

    private Currency parseCurrency(String curUnit) {
        if (curUnit == null) {
            return null;
        }
        String code = curUnit.replaceAll("\\(.*\\)", "").trim();
        try {
            return Currency.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BigDecimal parseRate(String rateString) {
        return new BigDecimal(rateString.replace(",", ""));
    }
}
