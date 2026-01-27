package com.example.messenger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MessengerApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(MessengerApplicationTests.class);

    @BeforeEach
    void setUp() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("Setting up Spring Boot context test");
    }

    @Test
    void contextLoads() {
        log.info("▶ Starting test: contextLoads");
        log.info("🔍 Verifying Spring application context loads successfully...");
        log.info("✓ Spring context loaded - All beans initialized");
        log.info("✅ Test passed: contextLoads");
    }

    @AfterEach
    void tearDown() {
        log.info("Test completed: {}", getClass().getSimpleName());
        log.info("═══════════════════════════════════════════════════════════");
    }

}
