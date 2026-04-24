package com.forex.order.exchangerate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KoreaEximApiResponse {

    @JsonProperty("cur_unit")
    private String curUnit;

    @JsonProperty("cur_nm")
    private String curNm;

    @JsonProperty("deal_bas_r")
    private String dealBasR;

    @JsonProperty("result")
    private Integer result;
}
