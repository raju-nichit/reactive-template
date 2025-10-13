package com.example.integration.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class LoggingFilter implements WebFilter {
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String requestId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
//        String requestId = request.getHeaders().getFirst("X-Correlation-Id");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        String correlationId = requestId;
        // Parent correlation id support
        String parentCorrelationId = request.getHeaders().getFirst("X-Parent-Correlation-Id");
        if (parentCorrelationId == null) {
            parentCorrelationId = correlationId; // if not provided, parent = self
        }

        exchange.getAttributes().put("correlationId", correlationId);
        exchange.getAttributes().put("parentCorrelationId", parentCorrelationId);

        log.info("[{}][parent={}] -> Incoming Request: {} {}", correlationId, parentCorrelationId,
                request.getMethod(), request.getURI());

        return chain.filter(exchange)
                .doOnError(ex -> {
                    ServerHttpResponse response = exchange.getResponse();
                    log.error("[{}] <- Error Response: status={}, error={}",
                            correlationId, response.getStatusCode(), ex.getMessage());
                })
                .doOnSuccess(done -> {
                    ServerHttpResponse response = exchange.getResponse();
                    log.info("[{}] <- Completed Response: status={}", correlationId, response.getStatusCode());
                })
                .doFinally(signalType -> {
                    // Optional cleanup or correlation context removal
                    log.debug("[{}] Request completed with signal: {}", correlationId, signalType);
                });
    }
}
