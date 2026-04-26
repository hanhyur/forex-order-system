package com.forex.order.exchangerate.controller;

import com.forex.order.common.Currency;
import com.forex.order.exchangerate.entity.ExchangeRateHistory;
import com.forex.order.exchangerate.repository.ExchangeRateHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExchangeRateHistoryRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        exchangeRateService.warmUpCache();
    }

    @Test
    @DisplayName("GET /exchange-rate/latest - 전체 환율 조회 시 exchangeRateList로 감싸서 반환한다")
    void getLatestAll() throws Exception {
        seedRate(Currency.USD, "1345.50", "1412.78", "1278.23");
        seedRate(Currency.EUR, "1478.90", "1552.85", "1404.96");

        // ExchangeRateService 캐시를 갱신하기 위해 warmUpCache 호출 필요
        // SpringBootTest에서는 @PostConstruct가 이미 실행됨 → 데이터 삽입 후 재워밍
        warmUpServiceCache();

        mockMvc.perform(get("/exchange-rate/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.exchangeRateList").isArray())
                .andExpect(jsonPath("$.returnObject.exchangeRateList", hasSize(2)));
    }

    @Test
    @DisplayName("GET /exchange-rate/latest/{currency} - 특정 통화 환율을 반환한다")
    void getLatestByCurrency() throws Exception {
        seedRate(Currency.USD, "1345.50", "1412.78", "1278.23");
        warmUpServiceCache();

        mockMvc.perform(get("/exchange-rate/latest/USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.currency").value("USD"))
                .andExpect(jsonPath("$.returnObject.tradeStanRate").value(1345.50))
                .andExpect(jsonPath("$.returnObject.buyRate").value(1412.78))
                .andExpect(jsonPath("$.returnObject.sellRate").value(1278.23));
    }

    @Test
    @DisplayName("GET /exchange-rate/latest/ABC - 잘못된 통화 코드는 400을 반환한다")
    void getLatestInvalidCurrency() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest/ABC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("GET /exchange-rate/latest/KRW - 기준 통화(KRW)는 조회 대상이 아니므로 400을 반환한다")
    void getLatestKrwReturns400() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest/KRW"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENCY"));
    }

    @Test
    @DisplayName("GET /exchange-rate/latest - 데이터 없으면 404를 반환한다")
    void getLatestEmpty() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Autowired
    private com.forex.order.exchangerate.service.ExchangeRateService exchangeRateService;

    private void warmUpServiceCache() {
        exchangeRateService.warmUpCache();
    }

    private void seedRate(Currency currency, String stanRate, String buyRate, String sellRate) {
        repository.save(ExchangeRateHistory.builder()
                .currency(currency)
                .tradeStanRate(new BigDecimal(stanRate))
                .buyRate(new BigDecimal(buyRate))
                .sellRate(new BigDecimal(sellRate))
                .dateTime(LocalDateTime.now())
                .build());
    }
}
