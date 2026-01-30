package com.example.messenger.integration;

import com.example.messenger.TestConfig;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for integration tests.
 * 
 * <p>Provides common configuration for tests that need the full Spring context
 * with database access. Each test runs in a transaction that is rolled back
 * after completion to ensure test isolation.
 * 
 * <p>Usage:
 * <pre>{@code
 * class AuthFlowIntegrationTest extends BaseIntegrationTest {
 *     @Autowired
 *     private MockMvc mockMvc;
 *     
 *     @Test
 *     void shouldRegisterAndLoginUser() {
 *         // test implementation
 *     }
 * }
 * }</pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public abstract class BaseIntegrationTest {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());

    static {
        Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);
        log.info("═══════════════════════════════════════════════════════════");
        log.info("Integration Test Configuration:");
        log.info("  - Spring Boot Test with MockMvc");
        log.info("  - Active Profile: test");
        log.info("  - Transactional: Yes (rollback after each test)");
        log.info("═══════════════════════════════════════════════════════════");
    }
}
