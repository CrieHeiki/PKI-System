package com.ct556.certification_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient vaultRestClient(VaultProperties vaultProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) vaultProperties.connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) vaultProperties.readTimeout().toMillis());

        return RestClient.builder()
                .baseUrl(vaultProperties.url())
                .requestFactory(requestFactory)
                .build();
    }
}