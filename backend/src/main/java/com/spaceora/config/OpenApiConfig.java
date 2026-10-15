package com.spaceora.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI spaceoraOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Spaceora API")
                .version("1.0")
                .description("Spaceora workspace booking platform API"));
    }
}
