package com.example.integration.model;

import java.util.Map;

public class OrchestrationResponse {
    private String status;
    private Map<String, Object> output;

    public OrchestrationResponse() {}

    public OrchestrationResponse(String status, Map<String, Object> output) {
        this.status = status;
        this.output = output;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }
}
