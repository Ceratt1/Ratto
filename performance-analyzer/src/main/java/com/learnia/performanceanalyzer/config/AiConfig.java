package com.learnia.performanceanalyzer.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiConfig {

    @Bean
    public WebClient aiWebClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.api-key}") String apiKey) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey == null ? "" : apiKey)
                .build();
    }

    @Bean
    @Qualifier("openRouterWebClient")
    public WebClient openRouterWebClient(
            @Value("${openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${openrouter.api-key:}") String apiKey) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + (apiKey == null ? "" : apiKey))
                .defaultHeader("HTTP-Referer", "https://ratto.app")
                .defaultHeader("X-Title", "Ratto")
                .build();
    }
}
