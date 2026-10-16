package com.spaceora;

import com.spaceora.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class SpaceoraApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpaceoraApplication.class, args);
    }
}
