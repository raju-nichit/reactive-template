package com.example.integration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class ExternalApiService {
    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);
    private final WebClient webClient;

    public ExternalApiService(WebClient webClient) {
        this.webClient = webClient;
    }

    public <T> Mono<T> callExternalApi(
            HttpMethod method,
            String url,
            Map<String, String> headers,
            Map<String, String> queryParams,
            Object body,
            Class<T> responseType,
            String parenId,
            String correlationId,
            String stepName
    ) {
        log.info("[{}][{}][{}] -> OUTGOING {} {}",parenId, correlationId, stepName, method, url);
        if (body != null) {
            log.debug("[{}][{}] -> OUTGOING BODY: {}", correlationId, stepName, body);
        }

        WebClient.RequestBodySpec spec = webClient.method(method)
                .uri(uriBuilder -> {
                    uriBuilder.path(url);
                    if (queryParams != null) queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .accept(MediaType.APPLICATION_JSON);

        if (headers != null) headers.forEach(spec::header);

        WebClient.RequestHeadersSpec<?> request = (method == HttpMethod.POST || method == HttpMethod.PUT)
                ? spec.contentType(MediaType.APPLICATION_JSON).bodyValue(body != null ? body : Map.of())
                : spec;

        return request.retrieve()
                .bodyToMono(responseType)
                .doOnNext(resp -> log.info("[{}][{}] <- INCOMING RESPONSE: {}", correlationId, stepName, resp))
                .doOnError(err -> log.error("[{}][{}] <- ERROR response: {}", correlationId, stepName, err.getMessage()))
                .onErrorResume(ex -> Mono.empty());
    }

    public <T> Mono<T> get(String url, Map<String, String> headers, Map<String, String> queryParams, Class<T> responseType, String parentId,String correlationId, String stepName) {
        return callExternalApi(HttpMethod.GET, url, headers, queryParams, null, responseType, parentId,correlationId, stepName);
    }

    public <T> Mono<T> post(String url, Map<String, String> headers, Object body, Class<T> responseType, String parentId,String correlationId, String stepName) {
        return callExternalApi(HttpMethod.POST, url, headers, null, body, responseType, parentId,correlationId, stepName);
    }
}
