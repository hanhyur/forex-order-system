package com.forex.order.exchangerate.repository;

import com.forex.order.common.Currency;
import com.forex.order.exchangerate.entity.ExchangeRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRateHistoryRepository extends JpaRepository<ExchangeRateHistory, Long> {

    Optional<ExchangeRateHistory> findTopByCurrencyOrderByDateTimeDesc(Currency currency);
}
