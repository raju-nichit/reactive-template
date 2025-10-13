package com.example.integration.service;

import com.example.integration.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrchestrationService {
    private static final Logger log = LoggerFactory.getLogger(OrchestrationService.class);
    private final StepProcessor stepProcessor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrchestrationService(StepProcessor stepProcessor) {
        this.stepProcessor = stepProcessor;
    }

    public Mono<OrchestrationResponse>  executeLastStepResult(OrchestrationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> context = new HashMap<>();
        context.put("_correlationId", correlationId);
        if (request.getInputData() != null) { context.putAll(request.getInputData()); }

        Map<String, Object> outputs = new HashMap<>();

        log.info("[{}] Starting workflow: {}", correlationId, request.getWorkflowName());

        return Mono.fromCallable(() -> loadWorkflowSteps(request.getWorkflowName()))
                .flatMapMany(Flux::fromIterable)
                .concatMap(step -> stepProcessor.executeStep(step, context)
                        .doOnNext(result -> {
                            Object cleanData = sanitizeData(result.getData());
                            outputs.put(result.getStepName(), cleanData);
                            outputs.put("previousStep", result.getStepName());
                            context.put(result.getStepName(), cleanData);
                            context.put("lastStep", result.getStepName());
                            log.info("[{}][workflow={}] step completed: {} success={}", correlationId, request.getWorkflowName(), result.getStepName(), result.isSuccess());
                        }))
                .then(Mono.fromCallable(() -> outputs))
                .map(safeOutput -> new OrchestrationResponse("SUCCESS", safeOutput))
                .doOnNext(resp -> log.info("[{}] Workflow {} finished. Output keys={}", correlationId, request.getWorkflowName(), resp.getOutput().keySet()))
                .onErrorResume(ex -> {
                    log.error("[{}] Workflow {} failed: {}", correlationId, request.getWorkflowName(), ex.getMessage());
                    return Mono.just(new OrchestrationResponse("FAILED", Map.of("error", ex.getMessage())));
                });
    }

        public Mono<OrchestrationResponse> executeWorkflowAndLastStepResult(OrchestrationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> context = new HashMap<>();
        context.put("_correlationId", correlationId);
        if (request.getInputData() != null) {
            context.putAll(request.getInputData());
        }

        log.info("[{}] Starting workflow: {}", correlationId, request.getWorkflowName());

        return Mono.fromCallable(() -> loadWorkflowSteps(request.getWorkflowName()))
                .flatMapMany(Flux::fromIterable)
                .concatMap(step -> stepProcessor.executeStep(step, context)
                        .doOnNext(result -> {
                            Object cleanData = sanitizeData(result.getData());
                            context.put(result.getStepName(), cleanData);
                            context.put("previousStep", result.getStepName());
                            context.put("lastStep", result.getStepName());
                            log.info("[{}][workflow={}] step completed: {}", correlationId, request.getWorkflowName(), result.getStepName());
                        }))
                .last()  // ✅ Only take the last StepResult
                .map(lastResult -> new OrchestrationResponse("SUCCESS", (Map<String, Object>) sanitizeData(lastResult.getData())))
                .doOnNext(resp -> log.info("[{}] Workflow {} finished successfully. Returning last step result.", correlationId, request.getWorkflowName()))
                .onErrorResume(ex -> {
                    log.error("[{}] Workflow {} failed: {}", correlationId, request.getWorkflowName(), ex.getMessage());
                    return Mono.just(new OrchestrationResponse("FAILED", Map.of("error", ex.getMessage())));
                });
    }

    public Mono<OrchestrationResponse> executeWorkflowAndLastStepResultV1(OrchestrationRequest request,String parentCorrelationId) {
        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> context = new HashMap<>();
        context.put("_correlationId", correlationId);
        context.put("_parentCorrelationId", parentCorrelationId == null ? correlationId : parentCorrelationId);

        if (request.getInputData() != null) {
            context.putAll(request.getInputData());
        }

        log.info("[{}][{}] Starting workflow: {}", parentCorrelationId,correlationId, request.getWorkflowName());

        return Mono.fromCallable(() -> loadWorkflowSteps(request.getWorkflowName()))
                .flatMapMany(Flux::fromIterable)
                .concatMap(step -> stepProcessor.executeStepV1(step, context,parentCorrelationId)
                        .doOnNext(result -> {
                            Object cleanData = sanitizeData(result.getData());
                            context.put(result.getStepName(), cleanData);
                            context.put("previousStep", result.getStepName());
                            context.put("lastStep", result.getStepName());
                            log.info("[{}][{}][workflow={}] step completed: {}",parentCorrelationId, correlationId, request.getWorkflowName(), result.getStepName());
                        }))
                .last()  // ✅ Only take the last StepResult
                .map(lastResult -> new OrchestrationResponse("SUCCESS", (Map<String, Object>) sanitizeData(lastResult.getData())))
                .doOnNext(resp -> log.info("[{}] Workflow {} finished successfully. Returning last step result.", correlationId, request.getWorkflowName()))
                .onErrorResume(ex -> {
                    log.error("[{}] Workflow {} failed: {}", correlationId, request.getWorkflowName(), ex.getMessage());
                    return Mono.just(new OrchestrationResponse("FAILED", Map.of("error", ex.getMessage())));
                });
    }

    private List<StepDefinition> loadWorkflowSteps(String workflowName) throws IOException {
        String path = "workflows/" + workflowName + ".json";
        var resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IOException("Workflow file not found: " + path);
        }

        Map<?, ?> json = objectMapper.readValue(resource.getInputStream(), Map.class);
        List<Map<String, Object>> stepsData = (List<Map<String, Object>>) json.get("steps");
        return stepsData.stream()
                .map(stepMap -> objectMapper.convertValue(stepMap, StepDefinition.class))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Object sanitizeData(Object data) {
        if (data == null) return null;

        if (data instanceof Map<?, ?> rawMap) {
            Map<String, Object> safeCopy = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                Object key = entry.getKey();
                if (key instanceof String strKey && !"context".equalsIgnoreCase(strKey)) {
                    Object value = entry.getValue();
                    if (value instanceof Map<?, ?> nestedMap) {
                        safeCopy.put(strKey, sanitizeData(nestedMap));
                    } else if (value instanceof reactor.core.publisher.Mono || value instanceof reactor.core.publisher.Flux) {
                        safeCopy.put(strKey, "ReactiveTypeValue");
                    } else {
                        safeCopy.put(strKey, value);
                    }
                }
            }
            return safeCopy;
        }

        if (data instanceof reactor.core.publisher.Mono || data instanceof reactor.core.publisher.Flux) {
            return "ReactiveTypeValue";
        }

        return data;
    }

    public Mono<OrchestrationResponse> executeWorkflow(OrchestrationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> context = new HashMap<>();
        context.put("_correlationId", correlationId);
        if (request.getInputData() != null) {
            context.putAll(request.getInputData());
        }

        Map<String, Object> outputs = new HashMap<>();

        log.info("[{}] Starting workflow: {}", correlationId, request.getWorkflowName());

        return Mono.fromCallable(() -> loadWorkflowSteps(request.getWorkflowName()))
                .flatMapMany(Flux::fromIterable)
                .concatMap(step -> stepProcessor.executeStep(step, context)
                        .doOnNext(result -> {
                            Object cleanData = sanitizeData(result.getData());
                            outputs.put(result.getStepName(), cleanData);
                            context.put(result.getStepName(), cleanData);
                            context.put("lastStep", result.getStepName());
                            log.info("[{}][workflow={}] Step completed: {} success={}",
                                    correlationId, request.getWorkflowName(), result.getStepName(), result.isSuccess());
                        }))
                .then(Mono.fromCallable(() -> {
                    // add metadata
                    outputs.put("_lastStep", context.get("lastStep"));
                    outputs.put("_correlationId", correlationId);
                    return outputs;
                }))
                .map(allOutputs -> new OrchestrationResponse("SUCCESS", allOutputs))
                .doOnNext(resp -> log.info("[{}] Workflow {} finished. Steps executed: {}",
                        correlationId, request.getWorkflowName(), outputs.keySet()))
                .onErrorResume(ex -> {
                    log.error("[{}] Workflow {} failed: {}", correlationId, request.getWorkflowName(), ex.getMessage(), ex);
                    return Mono.just(new OrchestrationResponse("FAILED", Map.of("error", ex.getMessage())));
                });
    }


}
