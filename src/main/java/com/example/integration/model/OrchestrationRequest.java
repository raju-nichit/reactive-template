package com.example.integration.model;

import java.util.Map;

public class OrchestrationRequest {
    private String workflowName;
    private Map<String, Object> inputData;

    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }

    public Map<String, Object> getInputData() { return inputData; }
    public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }
}
