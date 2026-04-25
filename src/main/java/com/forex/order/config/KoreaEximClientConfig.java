package com.forex.order.config;

import com.forex.order.exchangerate.client.KoreaEximClient;
import com.forex.order.exchangerate.client.KoreaEximClientImpl;
import com.forex.order.exchangerate.client.MockKoreaEximClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class KoreaEximClientConfig {

    @Value("${koreaexim.auth-key:}")
    private String authKey;

    @Bean
    public KoreaEximClient koreaEximClient(RestClient restClient) {
        if (authKey == null || authKey.isBlank()) {
            log.info("한국수출입은행 API 키가 설정되지 않아 Mock 클라이언트를 사용합니다.");
            return new MockKoreaEximClient();
        }

        log.info("한국수출입은행 실제 API 클라이언트를 사용합니다.");
        return new KoreaEximClientImpl(restClient, authKey);
    }
}
