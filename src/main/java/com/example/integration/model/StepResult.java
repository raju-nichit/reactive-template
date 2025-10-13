package com.example.integration.model;

import java.util.Map;

public class StepResult {
    private String stepName;
    private boolean success;
    private Map<String, Object> data;

    public StepResult() { }

    public StepResult(String stepName, boolean success, Map<String, Object> data) {
        this.stepName = stepName;
        this.success = success;
        this.data = data;
    }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
