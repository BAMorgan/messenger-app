package com.example.messenger.e2e;

import com.example.messenger.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for end-to-end tests.
 * 
 * <p>Provides common configuration for tests that simulate complete user
 * journeys. Uses a real HTTP server for testing actual network interactions.
 * 
 * <p>Note: Unlike integration tests, E2E tests do NOT roll back transactions
 * automatically. Use {@code @DirtiesContext} or clean up manually if needed.
 * 
 * <p>Usage:
 * <pre>{@code
 * class MessageDeliveryE2ETest extends BaseE2ETest {
 *     @Test
 *     void shouldDeliverMessageToRecipient() {
 *         // Register user A
 *         // Register user B  
 *         // User A creates conversation with B
 *         // User A sends message
 *         // Verify user B receives message
 *     }
 * }
 * }</pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig.class)
public abstract class BaseE2ETest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    static {
        Logger log = LoggerFactory.getLogger(BaseE2ETest.class);
        log.info("═══════════════════════════════════════════════════════════");
        log.info("E2E Test Configuration:");
        log.info("  - Spring Boot Test with real HTTP server");
        log.info("  - Active Profile: test");
        log.info("  - Transactional: No (manual cleanup required)");
        log.info("═══════════════════════════════════════════════════════════");
    }

    /**
     * Returns the base URL for the test server.
     * 
     * @return base URL including protocol and port
     */
    protected String getBaseUrl() {
        String baseUrl = "http://localhost:" + port;
        log.debug("Base URL: {}", baseUrl);
        return baseUrl;
    }

    /**
     * Returns the full URL for the given path.
     * 
     * @param path the API path (e.g., "/api/v1/auth/login")
     * @return full URL
     */
    protected String url(String path) {
        String fullUrl = getBaseUrl() + path;
        log.debug("Full URL for path '{}': {}", path, fullUrl);
        return fullUrl;
    }
}
