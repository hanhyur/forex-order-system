package com.forex.order.exchangerate.client;

import com.forex.order.exchangerate.dto.KoreaEximApiResponse;

import java.time.LocalDate;
import java.util.List;

public class MockKoreaEximClient implements KoreaEximClient {

    @Override
    public List<KoreaEximApiResponse> fetchRates(LocalDate date) {
        // TODO: 구현 예정
        return List.of();
    }
}
