package com.learnia.gateway.api;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.learnia.gateway.filters.CorrelationIdFilter;

@RestController
public class GatewayFallbackController {

    @RequestMapping("/gateway-fallback")
    ResponseEntity<GatewayErrorResponse> fallback(ServerWebExchange exchange) {
        String correlationId = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            exchange.getResponse().getHeaders().set(CorrelationIdFilter.HEADER_NAME, correlationId);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new GatewayErrorResponse(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "UPSTREAM_UNAVAILABLE",
                        "O serviço solicitado está temporariamente indisponível.",
                        correlationId,
                        Instant.now()));
    }

    record GatewayErrorResponse(
            int status,
            String code,
            String message,
            String correlationId,
            Instant timestamp) {
    }
}
