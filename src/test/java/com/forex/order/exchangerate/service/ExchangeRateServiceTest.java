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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateHistoryRepository repository;

    @Mock
    private KoreaEximClient koreaEximClient;

    @Mock
    private ExchangeRatePersistService persistService;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Nested
    @DisplayName("환율 수집")
    class FetchAndSaveRates {

        @Test
        @DisplayName("API 응답이 있으면 PersistService를 호출하고 캐시를 갱신한다")
        void callsPersistServiceAndUpdatesCache() {
            // Arrange
            KoreaEximApiResponse response = createApiResponse("USD", "1,345.50");
            given(koreaEximClient.fetchRates(any())).willReturn(List.of(response));

            ExchangeRateHistory saved = createRate(Currency.USD, "1345.50", "1412.78", "1278.23");
            given(persistService.processAndSave(any())).willReturn(List.of(saved));

            // Act
            exchangeRateService.fetchAndSaveRates();

            // Assert
            verify(persistService).processAndSave(any());
            ExchangeRateHistory cached = exchangeRateService.getLatestRate(Currency.USD);
            assertThat(cached.getBuyRate()).isEqualByComparingTo("1412.78");
        }

        @Test
        @DisplayName("빈 응답일 때 이전 영업일로 재시도한다")
        void retriesPreviousBusinessDay() {
            // Arrange
            KoreaEximApiResponse response = createApiResponse("USD", "1,345.50");
            given(koreaEximClient.fetchRates(any()))
                    .willReturn(List.of())
                    .willReturn(List.of(response));

            ExchangeRateHistory saved = createRate(Currency.USD, "1345.50", "1412.78", "1278.23");
            given(persistService.processAndSave(any())).willReturn(List.of(saved));

            // Act
            exchangeRateService.fetchAndSaveRates();

            // Assert: fetchRates가 2번 호출됨
            verify(koreaEximClient, times(2)).fetchRates(any());
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
            ExchangeRateHistory saved = createRate(Currency.USD, "1345.50", "1412.78", "1278.23");
            given(koreaEximClient.fetchRates(any())).willReturn(List.of(createApiResponse("USD", "1,345.50")));
            given(persistService.processAndSave(any())).willReturn(List.of(saved));
            exchangeRateService.fetchAndSaveRates();

            // Act
            ExchangeRateResponse result = exchangeRateService.getLatest(Currency.USD);

            // Assert
            assertThat(result.getCurrency()).isEqualTo("USD");
            assertThat(result.getBuyRate()).isEqualByComparingTo("1412.78");
            assertThat(result.getSellRate()).isEqualByComparingTo("1278.23");
        }

        @Test
        @DisplayName("전체 환율 조회 시 통화 코드 순으로 정렬한다")
        void returnsAllCurrenciesSorted() {
            // Arrange
            ExchangeRateHistory usd = createRate(Currency.USD, "1345.50", "1412.78", "1278.23");
            ExchangeRateHistory eur = createRate(Currency.EUR, "1478.90", "1552.85", "1404.96");
            given(koreaEximClient.fetchRates(any()))
                    .willReturn(List.of(createApiResponse("USD", "1,345.50"), createApiResponse("EUR", "1,478.90")));
            given(persistService.processAndSave(any())).willReturn(List.of(usd, eur));
            exchangeRateService.fetchAndSaveRates();

            // Act
            List<ExchangeRateResponse> results = exchangeRateService.getLatestAll();

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getCurrency()).isEqualTo("EUR");
            assertThat(results.get(1).getCurrency()).isEqualTo("USD");
        }
    }

    @Nested
    @DisplayName("캐시 워밍")
    class WarmUpCache {

        @Test
        @DisplayName("서버 시작 시 DB에서 최신 환율을 캐시에 로드한다")
        void loadsFromDbOnStartup() {
            // Arrange
            ExchangeRateHistory usdRate = createRate(Currency.USD, "1345.50", "1412.78", "1278.23");
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
        return new KoreaEximApiResponse(curUnit, null, dealBasR, 1);
    }

    private ExchangeRateHistory createRate(Currency currency, String stanRate, String buyRate, String sellRate) {
        return ExchangeRateHistory.builder()
                .currency(currency)
                .tradeStanRate(new BigDecimal(stanRate))
                .buyRate(new BigDecimal(buyRate))
                .sellRate(new BigDecimal(sellRate))
                .dateTime(LocalDateTime.now())
                .build();
    }
}
