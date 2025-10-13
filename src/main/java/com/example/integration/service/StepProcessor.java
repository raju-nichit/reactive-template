package com.example.integration.service;

import com.example.integration.model.StepDefinition;
import com.example.integration.model.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class StepProcessor {
    private static final Logger log = LoggerFactory.getLogger(StepProcessor.class);
    private final ExternalApiService apiService;

    public StepProcessor(ExternalApiService apiService) {
        this.apiService = apiService;
    }

    public Mono<StepResult> executeStep(StepDefinition step, Map<String, Object> context) {
        String stepName = step.getName();
        String correlationId = (String) context.getOrDefault("_correlationId", "no-cid");
        log.info("[{}][step={}] -> Executing type={}", correlationId, stepName, step.getType());

        if (step.getType() == null) {
            return Mono.just(new StepResult(stepName, false, Map.of("error", "missing step type")));
        }

        switch (step.getType().toUpperCase()) {
            case "API_CALL" -> {
                HttpMethod method = step.getMethod() != null ? step.getMethod() : HttpMethod.GET;
                return apiService.callExternalApi(method, step.getUrl(), step.getHeaders(), step.getQueryParams(), step.getRequestBody(), Map.class,null, correlationId, stepName)
                        .map(resp -> new StepResult(stepName, true, resp))
                        .defaultIfEmpty(new StepResult(stepName, false, Map.of("error", "empty response")));
            }
            case "BUSINESS_LOGIC" -> {
                Map<String, Object> data = Map.of(
                        "processedData", "Processed_" + context.getOrDefault("lastStep", "N/A"),
                        "echoInput", context
                );
                log.info("[{}][step={}] -> BUSINESS_LOGIC result: {}", correlationId, stepName, data);
                return Mono.just(new StepResult(stepName, true, data));
            }
            case "SAVE" -> {
                return apiService.post(step.getUrl(), step.getHeaders(), context, Map.class,null, correlationId, stepName)
                        .map(resp -> new StepResult(stepName, true, Map.of("saved", true)))
                        .defaultIfEmpty(new StepResult(stepName, false, Map.of("error", "save failed")));
            }
            default -> {
                log.error("[{}][step={}] Unknown step type: {}", correlationId, stepName, step.getType());
                return Mono.just(new StepResult(stepName, false, Map.of("error", "Unknown step type")));
            }
        }
    }

    public Mono<StepResult> executeStepV1(StepDefinition step, Map<String, Object> context,String parentId) {
        String stepName = step.getName();
        String correlationId = (String) context.getOrDefault("_correlationId", "no-cid");
        log.info("[{}][{}][step={}] -> Executing type={}",parentId, correlationId, stepName, step.getType());

        if (step.getType() == null) {
            return Mono.just(new StepResult(stepName, false, Map.of("error", "missing step type")));
        }

        switch (step.getType().toUpperCase()) {
            case "API_CALL" -> {
                HttpMethod method = step.getMethod() != null ? step.getMethod() : HttpMethod.GET;
                return apiService.callExternalApi(method, step.getUrl(), step.getHeaders(), step.getQueryParams(), step.getRequestBody(), Map.class,parentId, correlationId, stepName)
                        .map(resp -> new StepResult(stepName, true, resp))
                        .defaultIfEmpty(new StepResult(stepName, false, Map.of("error", "empty response")));
            }
            case "BUSINESS_LOGIC" -> {
                Map<String, Object> data = Map.of(
                        "processedData", "Processed_" + context.getOrDefault("lastStep", "N/A"),
                        "echoInput", context
                );
                log.info("[{}][step={}] -> BUSINESS_LOGIC result: {}", correlationId, stepName, data);
                return Mono.just(new StepResult(stepName, true, data));
            }
            case "SAVE" -> {
                return apiService.post(step.getUrl(), step.getHeaders(), context, Map.class, parentId,correlationId, stepName)
                        .map(resp -> new StepResult(stepName, true, Map.of("saved", true)))
                        .defaultIfEmpty(new StepResult(stepName, false, Map.of("error", "save failed")));
            }
            default -> {
                log.error("[{}][step={}] Unknown step type: {}", correlationId, stepName, step.getType());
                return Mono.just(new StepResult(stepName, false, Map.of("error", "Unknown step type")));
            }
        }
    }
}
