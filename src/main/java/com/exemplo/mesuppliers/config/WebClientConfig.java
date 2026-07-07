package com.exemplo.mesuppliers.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(MeApiProperties.class)
public class WebClientConfig {

    @Bean
    public WebClient meWebClient(WebClient.Builder builder, MeApiProperties properties) {
        return builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
