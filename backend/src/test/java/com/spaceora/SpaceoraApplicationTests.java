package com.spaceora;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SpaceoraApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context starts with H2 (no PostgreSQL needed)
    }
}
