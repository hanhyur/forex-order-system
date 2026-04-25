package com.forex.order.exchangerate.client;

import com.forex.order.exchangerate.dto.KoreaEximApiResponse;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Slf4j
public class MockKoreaEximClient implements KoreaEximClient {

    private static final BigDecimal USD_BASE = new BigDecimal("1400.00");
    private static final BigDecimal JPY_BASE = new BigDecimal("925.00");
    private static final BigDecimal CNY_BASE = new BigDecimal("195.00");
    private static final BigDecimal EUR_BASE = new BigDecimal("1500.00");
    private static final BigDecimal VARIATION_RANGE = new BigDecimal("0.03");

    private final Random random = new Random();

    @Override
    public List<KoreaEximApiResponse> fetchRates(LocalDate date) {
        log.info("Mock 환율 데이터 생성 (date: {})", date);

        return List.of(
                createResponse("USD", applyVariation(USD_BASE)),
                createResponse("JPY(100)", applyVariation(JPY_BASE)),
                createResponse("CNY", applyVariation(CNY_BASE)),
                createResponse("EUR", applyVariation(EUR_BASE))
        );
    }

    private BigDecimal applyVariation(BigDecimal base) {
        double variation = (random.nextDouble() * 2 - 1) * VARIATION_RANGE.doubleValue();
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(variation));
        return base.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private KoreaEximApiResponse createResponse(String curUnit, BigDecimal rate) {
        try {
            KoreaEximApiResponse response = new KoreaEximApiResponse();

            setField(response, "curUnit", curUnit);
            setField(response, "dealBasR", formatRate(rate));
            setField(response, "result", 1);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Mock 응답 생성 실패", e);
        }
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private String formatRate(BigDecimal rate) {
        return rate.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
