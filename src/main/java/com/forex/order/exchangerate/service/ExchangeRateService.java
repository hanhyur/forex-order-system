package com.forex.order.exchangerate.service;

import com.forex.order.common.Currency;
import com.forex.order.common.exception.RateNotFoundException;
import com.forex.order.exchangerate.client.KoreaEximClient;
import com.forex.order.exchangerate.dto.ExchangeRateResponse;
import com.forex.order.exchangerate.dto.KoreaEximApiResponse;
import com.forex.order.exchangerate.entity.ExchangeRateHistory;
import com.forex.order.exchangerate.repository.ExchangeRateHistoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private static final BigDecimal BUY_SPREAD_RATE = new BigDecimal("1.05");
    private static final BigDecimal SELL_SPREAD_RATE = new BigDecimal("0.95");
    private static final int MAX_RETRY_DAYS = 7;
    private static final Set<Currency> TARGET_CURRENCIES = Set.of(
            Currency.USD, Currency.JPY, Currency.CNY, Currency.EUR
    );
    private static final BigDecimal RANDOM_VARIATION_RANGE = new BigDecimal("0.02");
    private final Random random = new Random();

    private final ExchangeRateHistoryRepository repository;
    private final KoreaEximClient koreaEximClient;

    @org.springframework.beans.factory.annotation.Value("${exchange-rate.random-variation:true}")
    private boolean randomVariationEnabled = true;
    private final ConcurrentHashMap<Currency, ExchangeRateHistory> latestRates = new ConcurrentHashMap<>();

    @PostConstruct
    public void warmUpCache() {
        for (Currency currency : TARGET_CURRENCIES) {
            repository.findTopByCurrencyOrderByDateTimeDesc(currency)
                    .ifPresent(rate -> latestRates.put(currency, rate));
        }
        log.info("캐시 워밍 완료: {}개 통화 로드", latestRates.size());
    }

    public void fetchAndSaveRates() {
        LocalDate date = adjustToBusinessDay(LocalDate.now());

        for (int retry = 0; retry < MAX_RETRY_DAYS; retry++) {
            List<KoreaEximApiResponse> responses = koreaEximClient.fetchRates(date);

            if (!responses.isEmpty()) {
                processAndSave(responses);
                return;
            }

            date = adjustToBusinessDay(date.minusDays(1));
        }

        log.warn("{}일 이내 유효한 환율 데이터를 찾지 못했습니다", MAX_RETRY_DAYS);
    }

    public List<ExchangeRateResponse> getLatestAll() {
        if (latestRates.isEmpty()) {
            throw new RateNotFoundException("수집된 환율 데이터가 없습니다");
        }

        return latestRates.values().stream()
                .map(this::toResponse)
                .toList();
    }

    public ExchangeRateResponse getLatest(Currency currency) {
        ExchangeRateHistory rate = latestRates.get(currency);
        if (rate == null) {
            throw new RateNotFoundException(currency + " 환율 정보를 찾을 수 없습니다");
        }
        return toResponse(rate);
    }

    public ExchangeRateHistory getLatestRate(Currency currency) {
        ExchangeRateHistory rate = latestRates.get(currency);
        if (rate == null) {
            throw new RateNotFoundException(currency + " 환율 정보를 찾을 수 없습니다");
        }
        return rate;
    }

    @Transactional
    protected void processAndSave(List<KoreaEximApiResponse> responses) {
        for (KoreaEximApiResponse response : responses) {
            Currency currency = parseCurrency(response.getCurUnit());
            if (currency == null || !TARGET_CURRENCIES.contains(currency)) {
                continue;
            }

            BigDecimal tradeStanRate = applyRandomVariation(parseRate(response.getDealBasR()));
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

            repository.save(entity);
            latestRates.put(currency, entity);
        }

        log.info("환율 수집 완료: {}개 통화 갱신", latestRates.size());
    }

    private BigDecimal applyRandomVariation(BigDecimal rate) {
        if (!randomVariationEnabled) {
            return rate;
        }
        double variation = (random.nextDouble() * 2 - 1) * RANDOM_VARIATION_RANGE.doubleValue();
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(variation));
        return rate.multiply(factor).setScale(2, RoundingMode.HALF_UP);
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
        String cleaned = rateString.replace(",", "");
        return new BigDecimal(cleaned);
    }

    private LocalDate adjustToBusinessDay(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> date.minusDays(1);
            case SUNDAY -> date.minusDays(2);
            default -> date;
        };
    }

    private ExchangeRateResponse toResponse(ExchangeRateHistory entity) {
        return ExchangeRateResponse.builder()
                .currency(entity.getCurrency().name())
                .tradeStanRate(entity.getTradeStanRate())
                .buyRate(entity.getBuyRate())
                .sellRate(entity.getSellRate())
                .dateTime(entity.getDateTime())
                .build();
    }
}
