package com.example.messenger.integration;

import com.example.messenger.dto.AuthRequest;
import com.example.messenger.dto.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(com.example.messenger.TestConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MessagingAuthorizationIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void participantCanSendAndListMessages() throws Exception {
        AuthResponse userA = registerAndGetAuth("authz_a");
        AuthResponse userB = registerAndGetAuth("authz_b");
        long conversationId = createOneToOneConversation(userA, userB);

        ResponseEntity<String> sendResp = restTemplate.exchange(
                url("/api/v1/conversations/" + conversationId + "/messages"),
                HttpMethod.POST,
                new HttpEntity<>("{\"body\":\"hello\"}", authorizedJsonHeaders(userA.getAccessToken())),
                String.class
        );
        assertEquals(201, sendResp.getStatusCode().value());

        ResponseEntity<String> listResp = restTemplate.exchange(
                url("/api/v1/conversations/" + conversationId + "/messages"),
                HttpMethod.GET,
                new HttpEntity<>(authorizedJsonHeaders(userB.getAccessToken())),
                String.class
        );
        assertEquals(200, listResp.getStatusCode().value());
        var tree = objectMapper.readTree(listResp.getBody());
        assertTrue(tree.get("items").isArray());
        assertEquals(1, tree.get("items").size());
    }

    @Test
    void outsiderCannotSendMessage_returns403() throws Exception {
        AuthResponse userA = registerAndGetAuth("authz_send_a");
        AuthResponse userB = registerAndGetAuth("authz_send_b");
        AuthResponse outsider = registerAndGetAuth("authz_send_outsider");
        long conversationId = createOneToOneConversation(userA, userB);

        HttpClientErrorException.Forbidden ex = assertThrows(HttpClientErrorException.Forbidden.class, () ->
                restTemplate.exchange(
                        url("/api/v1/conversations/" + conversationId + "/messages"),
                        HttpMethod.POST,
                        new HttpEntity<>("{\"body\":\"not allowed\"}", authorizedJsonHeaders(outsider.getAccessToken())),
                        String.class
                )
        );

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getResponseBodyAsString().contains("Forbidden"));
    }

    @Test
    void outsiderCannotListMessages_returns403() throws Exception {
        AuthResponse userA = registerAndGetAuth("authz_list_a");
        AuthResponse userB = registerAndGetAuth("authz_list_b");
        AuthResponse outsider = registerAndGetAuth("authz_list_outsider");
        long conversationId = createOneToOneConversation(userA, userB);

        HttpClientErrorException.Forbidden ex = assertThrows(HttpClientErrorException.Forbidden.class, () ->
                restTemplate.exchange(
                        url("/api/v1/conversations/" + conversationId + "/messages"),
                        HttpMethod.GET,
                        new HttpEntity<>(authorizedJsonHeaders(outsider.getAccessToken())),
                        String.class
                )
        );

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getResponseBodyAsString().contains("Forbidden"));
    }

    @Test
    void unauthenticatedMessageEndpoints_return401() throws Exception {
        AuthResponse userA = registerAndGetAuth("authz_unauth_a");
        AuthResponse userB = registerAndGetAuth("authz_unauth_b");
        long conversationId = createOneToOneConversation(userA, userB);

        HttpClientErrorException.Unauthorized listEx = assertThrows(HttpClientErrorException.Unauthorized.class, () ->
                restTemplate.exchange(
                        url("/api/v1/conversations/" + conversationId + "/messages"),
                        HttpMethod.GET,
                        HttpEntity.EMPTY,
                        String.class
                )
        );
        assertEquals(401, listEx.getStatusCode().value());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpClientErrorException.Unauthorized sendEx = assertThrows(HttpClientErrorException.Unauthorized.class, () ->
                restTemplate.exchange(
                        url("/api/v1/conversations/" + conversationId + "/messages"),
                        HttpMethod.POST,
                        new HttpEntity<>("{\"body\":\"hello\"}", headers),
                        String.class
                )
        );
        assertEquals(401, sendEx.getStatusCode().value());
    }

    private AuthResponse registerAndGetAuth(String prefix) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + "_" + suffix;
        String email = username + "@test.com";

        AuthRequest request = new AuthRequest();
        request.setUsernameOrEmail(username);
        request.setEmail(email);
        request.setPassword("password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/register"),
                HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), headers),
                String.class
        );
        assertTrue(response.getStatusCode().is2xxSuccessful(), "register failed: " + response.getBody());
        return objectMapper.readValue(response.getBody(), AuthResponse.class);
    }

    private long createOneToOneConversation(AuthResponse userA, AuthResponse userB) throws Exception {
        HttpHeaders headers = authorizedJsonHeaders(userA.getAccessToken());
        String body = "{\"type\":\"ONE_TO_ONE\",\"participantIds\":[" + userA.getUserId() + "," + userB.getUserId() + "]}";

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/conversations"),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
        assertEquals(201, response.getStatusCode().value());
        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }

    private HttpHeaders authorizedJsonHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
