package com.forex.order.exchangerate.client;

import com.forex.order.exchangerate.dto.KoreaEximApiResponse;

import java.time.LocalDate;
import java.util.List;

public interface KoreaEximClient {

    List<KoreaEximApiResponse> fetchRates(LocalDate date);
}
