package com.example.integration.controller;

import com.example.integration.model.*;
import com.example.integration.service.OrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/workflows")
public class OrchestrationController {
    private static final Logger log = LoggerFactory.getLogger(OrchestrationController.class);
    private final OrchestrationService orchestrationService;

    public OrchestrationController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/execute")
    public Mono<OrchestrationResponse> executeWorkflow(@RequestBody OrchestrationRequest request) {
        log.info("[incoming] workflowName={}, inputData={}", request.getWorkflowName(), request.getInputData());
        return orchestrationService.executeWorkflow(request)
                .doOnNext(resp -> log.info("[outgoing] status={}, outputKeys={}", resp.getStatus(), resp.getOutput() == null ? 0 : resp.getOutput().keySet()));
    }

//    @PostMapping("/execute/v1")
//    public Mono<OrchestrationResponse> executeV1(@RequestBody OrchestrationRequest request) {
//        return orchestrationService.executeWorkflowAndLastStepResult(request);
//    }

    @PostMapping("/execute/v1")
    public Mono<ResponseEntity<OrchestrationResponse>> executeWorkflowLastStep(
            @RequestBody OrchestrationRequest request,
            ServerWebExchange exchange) {

        String correlationId = (String) exchange.getAttribute("correlationId");
        String parentCorrelationId = (String) exchange.getAttribute("parentCorrelationId");

        return orchestrationService.executeWorkflowAndLastStepResultV1(request, parentCorrelationId)
                .map(resp -> ResponseEntity.ok()
                        .header("X-Correlation-Id", correlationId)
                        .header("X-Parent-Correlation-Id", parentCorrelationId)
                        .body(resp));
    }
}
