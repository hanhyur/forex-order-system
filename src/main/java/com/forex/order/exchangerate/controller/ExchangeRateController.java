package com.forex.order.exchangerate.controller;

import com.forex.order.common.ApiResponse;
import com.forex.order.common.Currency;
import com.forex.order.exchangerate.dto.ExchangeRateResponse;
import com.forex.order.exchangerate.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/exchange-rate")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> getLatestAll() {
        List<ExchangeRateResponse> rates = exchangeRateService.getLatestAll();
        return ResponseEntity.ok(ApiResponse.ok(rates));
    }

    @GetMapping("/latest/{currency}")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> getLatest(
            @PathVariable Currency currency) {
        ExchangeRateResponse rate = exchangeRateService.getLatest(currency);
        return ResponseEntity.ok(ApiResponse.ok(rate));
    }
}
