package com.forex.order.exchangerate.service;

import com.forex.order.common.Currency;
import com.forex.order.exchangerate.dto.KoreaEximApiResponse;
import com.forex.order.exchangerate.entity.ExchangeRateHistory;
import com.forex.order.exchangerate.repository.ExchangeRateHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ExchangeRatePersistServiceTest {

    @Mock
    private ExchangeRateHistoryRepository repository;

    @InjectMocks
    private ExchangeRatePersistService persistService;

    @Test
    @DisplayName("buyRate = 매매기준율 x 1.05 (HALF_UP, 소수점 둘째자리)")
    void calculatesBuyRate() {
        // 1345.50 * 1.05 = 1412.775 → HALF_UP → 1412.78
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        List<ExchangeRateHistory> result = persistService.processAndSave(
                List.of(new KoreaEximApiResponse("USD", null, "1,345.50", 1)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBuyRate()).isEqualByComparingTo("1412.78");
    }

    @Test
    @DisplayName("sellRate = 매매기준율 x 0.95 (HALF_UP, 소수점 둘째자리)")
    void calculatesSellRate() {
        // 1345.50 * 0.95 = 1278.225 → HALF_UP → 1278.23
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        List<ExchangeRateHistory> result = persistService.processAndSave(
                List.of(new KoreaEximApiResponse("USD", null, "1,345.50", 1)));

        assertThat(result.get(0).getSellRate()).isEqualByComparingTo("1278.23");
    }

    @Test
    @DisplayName("HALF_UP 경계값: 소수점 셋째자리가 5일 때 올림한다")
    void halfUpEdgeCase() {
        // 1345.55 * 1.05 = 1412.8275 → HALF_UP → 1412.83
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        List<ExchangeRateHistory> result = persistService.processAndSave(
                List.of(new KoreaEximApiResponse("USD", null, "1,345.55", 1)));

        assertThat(result.get(0).getBuyRate()).isEqualByComparingTo("1412.83");
    }

    @Test
    @DisplayName("쉼표가 포함된 환율 문자열을 올바르게 파싱한다")
    void parsesCommaFormattedRate() {
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        List<ExchangeRateHistory> result = persistService.processAndSave(
                List.of(new KoreaEximApiResponse("EUR", null, "1,478.90", 1)));

        assertThat(result.get(0).getTradeStanRate()).isEqualByComparingTo("1478.90");
    }

    @Test
    @DisplayName("JPY(100) 형식의 통화코드를 올바르게 파싱한다")
    void parsesJpyWithUnitNotation() {
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        List<ExchangeRateHistory> result = persistService.processAndSave(
                List.of(new KoreaEximApiResponse("JPY(100)", null, "917.44", 1)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrency()).isEqualTo(Currency.JPY);
        assertThat(result.get(0).getTradeStanRate()).isEqualByComparingTo("917.44");
    }

    @Test
    @DisplayName("한국수출입은행 API의 CNH를 CNY로 매핑하여 저장한다")
    void mapsCnhToCny() {
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        List<ExchangeRateHistory> result = persistService.processAndSave(
                List.of(new KoreaEximApiResponse("CNH", null, "195.50", 1)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrency()).isEqualTo(Currency.CNY);
        assertThat(result.get(0).getTradeStanRate()).isEqualByComparingTo("195.50");
    }

    @Test
    @DisplayName("대상 통화(USD, JPY, CNY, EUR)만 저장한다")
    void savesOnlyTargetCurrencies() {
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        List<ExchangeRateHistory> result = persistService.processAndSave(List.of(
                new KoreaEximApiResponse("USD", null, "1,345.50", 1),
                new KoreaEximApiResponse("GBP", null, "1,700.00", 1)
        ));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrency()).isEqualTo(Currency.USD);
    }
}
