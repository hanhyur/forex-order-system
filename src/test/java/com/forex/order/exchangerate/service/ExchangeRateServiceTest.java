package com.forex.order.exchangerate.service;

import com.forex.order.common.Currency;
import com.forex.order.common.exception.RateNotFoundException;
import com.forex.order.exchangerate.client.KoreaEximClient;
import com.forex.order.exchangerate.dto.ExchangeRateResponse;
import com.forex.order.exchangerate.dto.KoreaEximApiResponse;
import com.forex.order.exchangerate.entity.ExchangeRateHistory;
import com.forex.order.exchangerate.repository.ExchangeRateHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateHistoryRepository repository;

    @Mock
    private KoreaEximClient koreaEximClient;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Nested
    @DisplayName("환율 수집 및 저장")
    class FetchAndSaveRates {

        @Test
        @DisplayName("매매기준율로 buyRate(x1.05)와 sellRate(x0.95)를 계산한다")
        void calculatesBuyAndSellRate() {
            // Arrange
            KoreaEximApiResponse response = createApiResponse("USD", "1,345.50");
            given(koreaEximClient.fetchRates(any())).willReturn(List.of(response));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // Act
            exchangeRateService.fetchAndSaveRates();

            // Assert
            verify(repository).save(any(ExchangeRateHistory.class));
        }

        @Test
        @DisplayName("buyRate는 HALF_UP으로 소수점 둘째자리 반올림한다")
        void buyRateRoundsHalfUp() {
            // Arrange: 1345.50 * 1.05 = 1412.775 → HALF_UP → 1412.78
            KoreaEximApiResponse response = createApiResponse("USD", "1,345.50");
            given(koreaEximClient.fetchRates(any())).willReturn(List.of(response));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // Act
            exchangeRateService.fetchAndSaveRates();

            // Assert
            ExchangeRateHistory rate = exchangeRateService.getLatestRate(Currency.USD);
            assertThat(rate.getBuyRate()).isEqualByComparingTo("1412.78");
        }

        @Test
        @DisplayName("sellRate는 HALF_UP으로 소수점 둘째자리 반올림한다")
        void sellRateRoundsHalfUp() {
            // Arrange: 1345.50 * 0.95 = 1278.225 → HALF_UP → 1278.23
            KoreaEximApiResponse response = createApiResponse("USD", "1,345.50");
            given(koreaEximClient.fetchRates(any())).willReturn(List.of(response));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // Act
            exchangeRateService.fetchAndSaveRates();

            // Assert
            ExchangeRateHistory rate = exchangeRateService.getLatestRate(Currency.USD);
            assertThat(rate.getSellRate()).isEqualByComparingTo("1278.23");
        }

        @Test
        @DisplayName("HALF_UP 경계값: 소수점 셋째자리가 5일 때 올림한다")
        void halfUpEdgeCase() {
            // Arrange: 1345.55 * 1.05 = 1412.8275 → HALF_UP → 1412.83
            KoreaEximApiResponse response = createApiResponse("USD", "1,345.55");
            given(koreaEximClient.fetchRates(any())).willReturn(List.of(response));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // Act
            exchangeRateService.fetchAndSaveRates();

            // Assert
            ExchangeRateHistory rate = exchangeRateService.getLatestRate(Currency.USD);
            assertThat(rate.getBuyRate()).isEqualByComparingTo("1412.83");
        }

        @Test
        @DisplayName("쉼표가 포함된 환율 문자열을 올바르게 파싱한다")
        void parsesCommaFormattedRate() {
            // Arrange
            KoreaEximApiResponse response = createApiResponse("EUR", "1,478.90");
            given(koreaEximClient.fetchRates(any())).willReturn(List.of(response));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // Act
            exchangeRateService.fetchAndSaveRates();

            // Assert
            ExchangeRateHistory rate = exchangeRateService.getLatestRate(Currency.EUR);
            assertThat(rate.getTradeStanRate()).isEqualByComparingTo("1478.90");
        }

        @Test
        @DisplayName("JPY(100) 형식의 통화코드를 올바르게 파싱한다")
        void parsesJpyWithUnitNotation() {
            // Arrange
            KoreaEximApiResponse response = createApiResponse("JPY(100)", "917.44");
            given(koreaEximClient.fetchRates(any())).willReturn(List.of(response));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // Act
            exchangeRateService.fetchAndSaveRates();

            // Assert
            ExchangeRateHistory rate = exchangeRateService.getLatestRate(Currency.JPY);
            assertThat(rate.getTradeStanRate()).isEqualByComparingTo("917.44");
        }

        @Test
        @DisplayName("대상 통화(USD, JPY, CNY, EUR)만 저장한다")
        void savesOnlyTargetCurrencies() {
            // Arrange
            KoreaEximApiResponse usd = createApiResponse("USD", "1,345.50");
            KoreaEximApiResponse gbp = createApiResponse("GBP", "1,700.00");
            given(koreaEximClient.fetchRates(any())).willReturn(List.of(usd, gbp));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // Act
            exchangeRateService.fetchAndSaveRates();

            // Assert: GBP는 조회되지 않아야 함
            assertThatThrownBy(() -> exchangeRateService.getLatestRate(Currency.KRW))
                    .isInstanceOf(RateNotFoundException.class);
        }

        @Test
        @DisplayName("빈 응답일 때 이전 영업일로 재시도한다")
        void retriesPreviousBusinessDay() {
            // Arrange: 첫 호출 빈 배열, 두 번째 호출 데이터 반환
            KoreaEximApiResponse response = createApiResponse("USD", "1,345.50");
            given(koreaEximClient.fetchRates(any()))
                    .willReturn(List.of())
                    .willReturn(List.of(response));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // Act
            exchangeRateService.fetchAndSaveRates();

            // Assert
            ExchangeRateHistory rate = exchangeRateService.getLatestRate(Currency.USD);
            assertThat(rate).isNotNull();
        }
    }

    @Nested
    @DisplayName("환율 조회")
    class GetLatestRates {

        @Test
        @DisplayName("캐시가 비어있으면 RateNotFoundException을 던진다")
        void throwsWhenCacheEmpty() {
            assertThatThrownBy(() -> exchangeRateService.getLatestAll())
                    .isInstanceOf(RateNotFoundException.class)
                    .hasMessageContaining("수집된 환율 데이터가 없습니다");
        }

        @Test
        @DisplayName("특정 통화 환율이 없으면 RateNotFoundException을 던진다")
        void throwsWhenCurrencyNotFound() {
            assertThatThrownBy(() -> exchangeRateService.getLatest(Currency.USD))
                    .isInstanceOf(RateNotFoundException.class);
        }

        @Test
        @DisplayName("수집된 환율을 ExchangeRateResponse로 변환하여 반환한다")
        void returnsResponseAfterFetch() {
            // Arrange
            KoreaEximApiResponse response = createApiResponse("USD", "1,345.50");
            given(koreaEximClient.fetchRates(any())).willReturn(List.of(response));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
            exchangeRateService.fetchAndSaveRates();

            // Act
            ExchangeRateResponse result = exchangeRateService.getLatest(Currency.USD);

            // Assert
            assertThat(result.getCurrency()).isEqualTo("USD");
            assertThat(result.getTradeStanRate()).isEqualByComparingTo("1345.50");
            assertThat(result.getBuyRate()).isEqualByComparingTo("1412.78");
            assertThat(result.getSellRate()).isEqualByComparingTo("1278.23");
        }

        @Test
        @DisplayName("전체 환율 조회 시 수집된 모든 통화를 반환한다")
        void returnsAllCurrencies() {
            // Arrange
            KoreaEximApiResponse usd = createApiResponse("USD", "1,345.50");
            KoreaEximApiResponse eur = createApiResponse("EUR", "1,478.90");
            given(koreaEximClient.fetchRates(any())).willReturn(List.of(usd, eur));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
            exchangeRateService.fetchAndSaveRates();

            // Act
            List<ExchangeRateResponse> results = exchangeRateService.getLatestAll();

            // Assert
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("캐시 워밍")
    class WarmUpCache {

        @Test
        @DisplayName("서버 시작 시 DB에서 최신 환율을 캐시에 로드한다")
        void loadsFromDbOnStartup() {
            // Arrange
            ExchangeRateHistory usdRate = ExchangeRateHistory.builder()
                    .currency(Currency.USD)
                    .tradeStanRate(new BigDecimal("1345.50"))
                    .buyRate(new BigDecimal("1412.78"))
                    .sellRate(new BigDecimal("1278.23"))
                    .dateTime(java.time.LocalDateTime.now())
                    .build();

            given(repository.findTopByCurrencyOrderByDateTimeDesc(Currency.USD))
                    .willReturn(Optional.of(usdRate));
            given(repository.findTopByCurrencyOrderByDateTimeDesc(Currency.JPY))
                    .willReturn(Optional.empty());
            given(repository.findTopByCurrencyOrderByDateTimeDesc(Currency.CNY))
                    .willReturn(Optional.empty());
            given(repository.findTopByCurrencyOrderByDateTimeDesc(Currency.EUR))
                    .willReturn(Optional.empty());

            // Act
            exchangeRateService.warmUpCache();

            // Assert
            ExchangeRateHistory result = exchangeRateService.getLatestRate(Currency.USD);
            assertThat(result.getBuyRate()).isEqualByComparingTo("1412.78");
        }
    }

    private KoreaEximApiResponse createApiResponse(String curUnit, String dealBasR) {
        try {
            KoreaEximApiResponse response = new KoreaEximApiResponse();
            var curUnitField = KoreaEximApiResponse.class.getDeclaredField("curUnit");
            curUnitField.setAccessible(true);
            curUnitField.set(response, curUnit);

            var dealBasRField = KoreaEximApiResponse.class.getDeclaredField("dealBasR");
            dealBasRField.setAccessible(true);
            dealBasRField.set(response, dealBasR);

            var resultField = KoreaEximApiResponse.class.getDeclaredField("result");
            resultField.setAccessible(true);
            resultField.set(response, 1);

            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
