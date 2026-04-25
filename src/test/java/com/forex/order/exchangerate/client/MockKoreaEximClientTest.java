package com.forex.order.exchangerate.client;

import com.forex.order.exchangerate.dto.KoreaEximApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MockKoreaEximClientTest {

    private final MockKoreaEximClient mockClient = new MockKoreaEximClient();

    @Test
    @DisplayName("4개 대상 통화(USD, JPY, CNY, EUR)의 환율을 반환한다")
    void returnsFourCurrencies() {
        List<KoreaEximApiResponse> responses = mockClient.fetchRates(LocalDate.now());

        assertThat(responses).hasSize(4);

        Set<String> currencies = Set.of("USD", "JPY(100)", "CNY", "EUR");
        for (KoreaEximApiResponse response : responses) {
            assertThat(currencies).contains(response.getCurUnit());
        }
    }

    @Test
    @DisplayName("USD 환율이 현실적인 범위(1300~1500) 내에 있다")
    void usdRateInRealisticRange() {
        List<KoreaEximApiResponse> responses = mockClient.fetchRates(LocalDate.now());

        KoreaEximApiResponse usd = responses.stream()
                .filter(r -> "USD".equals(r.getCurUnit()))
                .findFirst()
                .orElseThrow();

        BigDecimal rate = new BigDecimal(usd.getDealBasR().replace(",", ""));
        assertThat(rate).isBetween(new BigDecimal("1300"), new BigDecimal("1500"));
    }

    @Test
    @DisplayName("JPY(100) 환율이 현실적인 범위(850~1000) 내에 있다")
    void jpyRateInRealisticRange() {
        List<KoreaEximApiResponse> responses = mockClient.fetchRates(LocalDate.now());

        KoreaEximApiResponse jpy = responses.stream()
                .filter(r -> "JPY(100)".equals(r.getCurUnit()))
                .findFirst()
                .orElseThrow();

        BigDecimal rate = new BigDecimal(jpy.getDealBasR().replace(",", ""));
        assertThat(rate).isBetween(new BigDecimal("850"), new BigDecimal("1000"));
    }

    @Test
    @DisplayName("CNY 환율이 현실적인 범위(180~210) 내에 있다")
    void cnyRateInRealisticRange() {
        List<KoreaEximApiResponse> responses = mockClient.fetchRates(LocalDate.now());

        KoreaEximApiResponse cny = responses.stream()
                .filter(r -> "CNY".equals(r.getCurUnit()))
                .findFirst()
                .orElseThrow();

        BigDecimal rate = new BigDecimal(cny.getDealBasR().replace(",", ""));
        assertThat(rate).isBetween(new BigDecimal("180"), new BigDecimal("210"));
    }

    @Test
    @DisplayName("EUR 환율이 현실적인 범위(1400~1600) 내에 있다")
    void eurRateInRealisticRange() {
        List<KoreaEximApiResponse> responses = mockClient.fetchRates(LocalDate.now());

        KoreaEximApiResponse eur = responses.stream()
                .filter(r -> "EUR".equals(r.getCurUnit()))
                .findFirst()
                .orElseThrow();

        BigDecimal rate = new BigDecimal(eur.getDealBasR().replace(",", ""));
        assertThat(rate).isBetween(new BigDecimal("1400"), new BigDecimal("1600"));
    }

    @Test
    @DisplayName("호출할 때마다 미세하게 다른 환율을 반환한다")
    void returnsSlightlyDifferentRatesEachCall() {
        List<KoreaEximApiResponse> first = mockClient.fetchRates(LocalDate.now());
        List<KoreaEximApiResponse> second = mockClient.fetchRates(LocalDate.now());

        String firstUsdRate = first.stream()
                .filter(r -> "USD".equals(r.getCurUnit()))
                .map(KoreaEximApiResponse::getDealBasR)
                .findFirst().orElseThrow();

        String secondUsdRate = second.stream()
                .filter(r -> "USD".equals(r.getCurUnit()))
                .map(KoreaEximApiResponse::getDealBasR)
                .findFirst().orElseThrow();

        // 랜덤 변동이 적용되므로 대부분 다르지만, 극히 드물게 같을 수 있음
        // 여러 통화를 합쳐서 전체가 동일할 확률은 거의 0
        boolean anyDifferent = false;
        for (int i = 0; i < first.size(); i++) {
            if (!first.get(i).getDealBasR().equals(second.get(i).getDealBasR())) {
                anyDifferent = true;
                break;
            }
        }
        assertThat(anyDifferent).isTrue();
    }

    @Test
    @DisplayName("result 필드가 1(성공)을 반환한다")
    void returnsSuccessResult() {
        List<KoreaEximApiResponse> responses = mockClient.fetchRates(LocalDate.now());

        for (KoreaEximApiResponse response : responses) {
            assertThat(response.getResult()).isEqualTo(1);
        }
    }
}
