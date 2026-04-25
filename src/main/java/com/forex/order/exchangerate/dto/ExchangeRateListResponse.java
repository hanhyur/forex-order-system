package com.forex.order.exchangerate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExchangeRateListResponse {

    private List<ExchangeRateResponse> exchangeRateList;
}
