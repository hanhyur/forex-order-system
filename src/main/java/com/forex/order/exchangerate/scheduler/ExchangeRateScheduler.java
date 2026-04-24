package com.forex.order.exchangerate.scheduler;

import com.forex.order.exchangerate.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateScheduler {

    private final ExchangeRateService exchangeRateService;

    @Scheduled(fixedDelay = 60000)
    public void fetchExchangeRates() {
        try {
            exchangeRateService.fetchAndSaveRates();
        } catch (Exception e) {
            log.warn("환율 수집 실패: {}", e.getMessage());
        }
    }
}
