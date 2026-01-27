package com.example.messenger.integration;

import com.example.messenger.dto.AuthRequest;
import com.example.messenger.dto.AuthResponse;
import com.example.messenger.dto.TokenRefreshRequest;
import com.example.messenger.repository.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AuthIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("Setting up test: {}", getClass().getSimpleName());
    }

    @AfterEach
    void tearDown() {
        log.info("Test completed: {}", getClass().getSimpleName());
        log.info("═══════════════════════════════════════════════════════════");
    }

    @Test
    void shouldRegisterNewUser() throws Exception {
        log.info("▶ Starting test: shouldRegisterNewUser");
        
        AuthRequest request = new AuthRequest();
        request.setUsernameOrEmail("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setDisplayName("Test User");
        
        log.info("📤 Registering user - Username: {}, Email: {}", request.getUsernameOrEmail(), request.getEmail());

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        log.info("✅ Registration successful - Response received");
        log.debug("📥 Response body: {}", response);

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        log.info("🔍 Validating response data...");
        assertNotNull(authResponse.getAccessToken(), "Access token should not be null");
        log.info("✓ Access token validated");
        assertNotNull(authResponse.getRefreshToken(), "Refresh token should not be null");
        log.info("✓ Refresh token validated");
        assertTrue(appUserRepository.findByEmail("test@example.com").isPresent(), "User should exist in database");
        log.info("✓ User found in database");
        log.info("✅ Test passed: shouldRegisterNewUser");
    }

    @Test
    void shouldNotRegisterUserWithDuplicateEmail() throws Exception {
        log.info("▶ Starting test: shouldNotRegisterUserWithDuplicateEmail");
        
        // Register first user
        AuthRequest request1 = new AuthRequest();
        request1.setUsernameOrEmail("user1");
        request1.setEmail("duplicate@example.com");
        request1.setPassword("password123");

        log.info("📤 Registering first user - Username: {}, Email: {}", request1.getUsernameOrEmail(), request1.getEmail());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());
        log.info("✅ First user registered successfully");

        // Try to register with same email
        AuthRequest request2 = new AuthRequest();
        request2.setUsernameOrEmail("user2");
        request2.setEmail("duplicate@example.com");
        request2.setPassword("password123");

        log.info("📤 Attempting duplicate registration - Username: {}, Email: {}", request2.getUsernameOrEmail(), request2.getEmail());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest());
        log.info("✅ Duplicate registration correctly rejected with BadRequest status");
        log.info("✅ Test passed: shouldNotRegisterUserWithDuplicateEmail");
    }

    @Test
    void shouldLoginWithUsername() throws Exception {
        log.info("▶ Starting test: shouldLoginWithUsername");
        
        // Register user first
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsernameOrEmail("loginuser");
        registerRequest.setEmail("login@example.com");
        registerRequest.setPassword("password123");

        log.info("📤 Registering user for login test - Username: {}, Email: {}", 
                registerRequest.getUsernameOrEmail(), registerRequest.getEmail());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());
        log.info("✅ User registered successfully");

        // Login with username
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsernameOrEmail("loginuser");
        loginRequest.setPassword("password123");

        log.info("🔐 Attempting login with username: {}", loginRequest.getUsernameOrEmail());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
        log.info("✅ Login successful - Access token and refresh token received");
        log.info("✅ Test passed: shouldLoginWithUsername");
    }

    @Test
    void shouldLoginWithEmail() throws Exception {
        log.info("▶ Starting test: shouldLoginWithEmail");
        
        // Register user first
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsernameOrEmail("emailuser");
        registerRequest.setEmail("emaillogin@example.com");
        registerRequest.setPassword("password123");

        log.info("📤 Registering user for email login test - Username: {}, Email: {}", 
                registerRequest.getUsernameOrEmail(), registerRequest.getEmail());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());
        log.info("✅ User registered successfully");

        // Login with email
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsernameOrEmail("emaillogin@example.com");
        loginRequest.setPassword("password123");

        log.info("🔐 Attempting login with email: {}", loginRequest.getUsernameOrEmail());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
        log.info("✅ Login successful - Access token and refresh token received");
        log.info("✅ Test passed: shouldLoginWithEmail");
    }

    @Test
    void shouldNotLoginWithInvalidPassword() throws Exception {
        log.info("▶ Starting test: shouldNotLoginWithInvalidPassword");
        
        // Register user first
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsernameOrEmail("invaliduser");
        registerRequest.setEmail("invalid@example.com");
        registerRequest.setPassword("password123");

        log.info("📤 Registering user for invalid password test - Username: {}, Email: {}", 
                registerRequest.getUsernameOrEmail(), registerRequest.getEmail());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());
        log.info("✅ User registered successfully");

        // Try to login with wrong password
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsernameOrEmail("invaliduser");
        loginRequest.setPassword("wrongpassword");

        log.info("🔐 Attempting login with invalid password - Username: {}", loginRequest.getUsernameOrEmail());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
        log.info("✅ Login correctly rejected with Unauthorized status");
        log.info("✅ Test passed: shouldNotLoginWithInvalidPassword");
    }

    @Test
    void shouldRefreshAccessToken() throws Exception {
        log.info("▶ Starting test: shouldRefreshAccessToken");
        
        // Register and login to get tokens
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsernameOrEmail("refreshtest");
        registerRequest.setEmail("refresh@example.com");
        registerRequest.setPassword("password123");

        log.info("📤 Registering user for token refresh test - Username: {}, Email: {}", 
                registerRequest.getUsernameOrEmail(), registerRequest.getEmail());
        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        log.info("✅ User registered successfully");

        AuthResponse authResponse = objectMapper.readValue(registerResponse, AuthResponse.class);
        String refreshToken = authResponse.getRefreshToken();
        log.info("🔑 Retrieved refresh token (length: {})", refreshToken != null ? refreshToken.length() : 0);

        // Refresh token
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(refreshToken);
        log.info("🔄 Attempting to refresh access token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
        log.info("✅ Token refresh successful - New access token and refresh token received");
        log.info("✅ Test passed: shouldRefreshAccessToken");
    }

    @Test
    void shouldNotRefreshWithInvalidToken() throws Exception {
        log.info("▶ Starting test: shouldNotRefreshWithInvalidToken");
        
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest("invalid-token");
        log.info("🔄 Attempting to refresh with invalid token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
        log.info("✅ Invalid token refresh correctly rejected with Unauthorized status");
        log.info("✅ Test passed: shouldNotRefreshWithInvalidToken");
    }
}
