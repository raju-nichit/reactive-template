package com.example.integration.model;

import org.springframework.http.HttpMethod;
import java.util.Map;

public class StepDefinition {
    private String name;
    private String type; // API_CALL, BUSINESS_LOGIC, SAVE
    private HttpMethod method;
    private String url;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<String, Object> requestBody;
    private String nextCondition;

    public StepDefinition() { }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public HttpMethod getMethod() { return method; }
    public void setMethod(HttpMethod method) { this.method = method; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public Map<String, String> getQueryParams() { return queryParams; }
    public void setQueryParams(Map<String, String> queryParams) { this.queryParams = queryParams; }

    public Map<String, Object> getRequestBody() { return requestBody; }
    public void setRequestBody(Map<String, Object> requestBody) { this.requestBody = requestBody; }

    public String getNextCondition() { return nextCondition; }
    public void setNextCondition(String nextCondition) { this.nextCondition = nextCondition; }
}
