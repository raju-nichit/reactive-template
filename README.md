# Reactive Orchestration v4

Java 17 + Spring Boot WebFlux example with:
- Workflow files in resources/workflows/*.json
- Per-request correlation id and logging
- Step-level external API request/response logs
- Swagger UI via SpringDoc at /swagger-ui.html

Run:
mvn clean package
java -jar target/reactive-orchestration-0.0.1-SNAPSHOT.jar
"# reactive-template" 

{
  "workflowName": "CustomerOnboarding",
  "inputData": {
    "customerId": "CUST-001"
  }
}