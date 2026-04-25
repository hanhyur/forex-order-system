package com.forex.order.order.controller;

import com.forex.order.common.Currency;
import com.forex.order.exchangerate.entity.ExchangeRateHistory;
import com.forex.order.exchangerate.repository.ExchangeRateHistoryRepository;
import com.forex.order.order.repository.ForexOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExchangeRateHistoryRepository rateRepository;

    @Autowired
    private ForexOrderRepository orderRepository;

    @Autowired
    private com.forex.order.exchangerate.service.ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        rateRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /order - KRW→USD 매수 주문이 정상 처리된다")
    void createBuyOrder() throws Exception {
        seedRate(Currency.USD, "1345.50", "1412.78", "1278.23");
        exchangeRateService.warmUpCache();

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"forexAmount": 100, "fromCurrency": "KRW", "toCurrency": "USD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.fromCurrency").value("KRW"))
                .andExpect(jsonPath("$.returnObject.toCurrency").value("USD"))
                .andExpect(jsonPath("$.returnObject.tradeRate").value(1412.78));
    }

    @Test
    @DisplayName("POST /order - USD→KRW 매도 주문이 정상 처리된다")
    void createSellOrder() throws Exception {
        seedRate(Currency.USD, "1345.50", "1412.78", "1278.23");
        exchangeRateService.warmUpCache();

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"forexAmount": 133, "fromCurrency": "USD", "toCurrency": "KRW"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.returnObject.toCurrency").value("KRW"))
                .andExpect(jsonPath("$.returnObject.tradeRate").value(1278.23));
    }

    @Test
    @DisplayName("POST /order - 필수값 누락 시 400을 반환한다")
    void createOrderMissingField() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromCurrency": "KRW", "toCurrency": "USD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("POST /order - 잘못된 통화값 시 400을 반환한다 (500이 아님)")
    void createOrderInvalidCurrency() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"forexAmount": 100, "fromCurrency": "KRW", "toCurrency": "ABC"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("POST /order - 음수 금액 시 400을 반환한다")
    void createOrderNegativeAmount() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"forexAmount": -100, "fromCurrency": "KRW", "toCurrency": "USD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("POST /order - 환율 미준비 시 422를 반환한다")
    void createOrderNoRate() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"forexAmount": 100, "fromCurrency": "KRW", "toCurrency": "USD"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RATE_NOT_AVAILABLE"));
    }

    @Test
    @DisplayName("POST /order - KRW 미포함 통화쌍은 400을 반환한다")
    void createOrderNonKrwPair() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"forexAmount": 100, "fromCurrency": "USD", "toCurrency": "EUR"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENCY"));
    }

    @Test
    @DisplayName("GET /order/list - 빈 주문 목록은 orderList로 감싸서 빈 배열 반환")
    void getOrdersEmpty() throws Exception {
        mockMvc.perform(get("/order/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.orderList").isArray())
                .andExpect(jsonPath("$.returnObject.orderList", hasSize(0)));
    }

    @Test
    @DisplayName("GET /order/list - 주문 후 내역이 조회된다")
    void getOrdersAfterCreation() throws Exception {
        seedRate(Currency.USD, "1345.50", "1412.78", "1278.23");
        exchangeRateService.warmUpCache();

        mockMvc.perform(post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"forexAmount": 100, "fromCurrency": "KRW", "toCurrency": "USD"}
                        """));

        mockMvc.perform(get("/order/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.orderList", hasSize(1)))
                .andExpect(jsonPath("$.returnObject.orderList[0].id").exists());
    }

    private void seedRate(Currency currency, String stanRate, String buyRate, String sellRate) {
        rateRepository.save(ExchangeRateHistory.builder()
                .currency(currency)
                .tradeStanRate(new BigDecimal(stanRate))
                .buyRate(new BigDecimal(buyRate))
                .sellRate(new BigDecimal(sellRate))
                .dateTime(LocalDateTime.now())
                .build());
    }
}
