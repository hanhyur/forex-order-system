package com.forex.order.exchangerate.client;

import com.forex.order.exchangerate.dto.KoreaEximApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KoreaEximClientImpl implements KoreaEximClient {

    private static final String BASE_URL = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestClient restClient;

    @Value("${koreaexim.auth-key}")
    private String authKey;

    @Override
    public List<KoreaEximApiResponse> fetchRates(LocalDate date) {
        String searchDate = date.format(DATE_FORMAT);

        KoreaEximApiResponse[] responses = restClient.get()
                .uri(BASE_URL + "?authkey={key}&searchdate={date}&data=AP01",
                        authKey, searchDate)
                .retrieve()
                .body(KoreaEximApiResponse[].class);

        if (responses == null) {
            return List.of();
        }

        return List.of(responses);
    }
}
