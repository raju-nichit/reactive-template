package com.example.integration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reactive Orchestration API")
                        .description("Generic reactive step-based orchestration engine with workflow file support.")
                        .version("v4.0.0")
                        .contact(new Contact().name("Integration Team").email("team@example.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}
