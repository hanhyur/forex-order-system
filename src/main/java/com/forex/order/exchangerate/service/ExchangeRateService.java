package com.forex.order.exchangerate.service;

import com.forex.order.common.Currency;
import com.forex.order.exchangerate.client.KoreaEximClient;
import com.forex.order.exchangerate.dto.ExchangeRateResponse;
import com.forex.order.exchangerate.entity.ExchangeRateHistory;
import com.forex.order.exchangerate.repository.ExchangeRateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final ExchangeRateHistoryRepository repository;
    private final KoreaEximClient koreaEximClient;

    public void fetchAndSaveRates() {
        // TODO: 구현 예정
    }

    public List<ExchangeRateResponse> getLatestAll() {
        // TODO: 구현 예정
        return List.of();
    }

    public ExchangeRateResponse getLatest(Currency currency) {
        // TODO: 구현 예정
        return null;
    }

    public ExchangeRateHistory getLatestRate(Currency currency) {
        // TODO: 구현 예정
        return null;
    }

    public void warmUpCache() {
        // TODO: 구현 예정
    }
}
