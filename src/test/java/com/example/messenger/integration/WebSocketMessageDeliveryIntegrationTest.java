package com.example.messenger.integration;

import com.example.messenger.config.JwtConfig;
import com.example.messenger.dto.AuthRequest;
import com.example.messenger.dto.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WebSocket real-time delivery.
 *
 * <ul>
 *   <li>Valid JWT: WebSocket handshake succeeds and client can connect.</li>
 *   <li>Invalid/missing token: handshake is rejected.</li>
 *   <li>REST send message: an event is created and (when a client is connected to the same
 *       server) the client receives exactly one event with the same message payload.
 *       Full delivery is validated here using StandardWebSocketClient; unit tests cover
 *       EventService.publish and MessageService event emission.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(com.example.messenger.TestConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WebSocketMessageDeliveryIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtConfig jwtConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void websocket_connectWithValidJwt_succeeds() throws Exception {
        AuthResponse auth = registerViaServer("wsuser");
        WebSocketSession session = connectWebSocket(auth.getAccessToken(), new LinkedBlockingQueue<>());
        try {
            assertTrue(session.isOpen(), "WebSocket should connect with valid JWT");
        } finally {
            session.close();
        }
    }

    @Test
    void websocket_connectWithoutToken_rejected() {
        assertHandshakeRejected("ws://localhost:" + port + "/api/v1/events");
    }

    @Test
    void websocket_connectWithInvalidToken_rejected() {
        assertHandshakeRejected("ws://localhost:" + port + "/api/v1/events?token=not-a-jwt");
    }

    @Test
    void websocket_connectWithExpiredToken_rejected() throws Exception {
        AuthResponse auth = registerViaServer("wsexpired");
        String expiredToken = createExpiredAccessToken(auth.getUsername());
        assertHandshakeRejected("ws://localhost:" + port + "/api/v1/events?token=" + expiredToken);
    }

    @Test
    void whenMessageSentViaRest_recipientReceivesOneEventOverWebSocket() throws Exception {
        AuthResponse senderAuth = registerViaServer("wsender");
        AuthResponse recipientAuth = registerViaServer("wrecipient");

        HttpHeaders convHeaders = new HttpHeaders();
        convHeaders.setBearerAuth(senderAuth.getAccessToken());
        convHeaders.setContentType(MediaType.APPLICATION_JSON);
        String convBody = "{\"type\":\"ONE_TO_ONE\",\"participantIds\":[" + senderAuth.getUserId() + "," + recipientAuth.getUserId() + "]}";
        String convUrl = "http://localhost:" + port + "/api/v1/conversations";
        ResponseEntity<String> convResp = restTemplate.exchange(convUrl, org.springframework.http.HttpMethod.POST, new HttpEntity<>(convBody, convHeaders), String.class);
        assertTrue(convResp.getStatusCode().is2xxSuccessful(), "Create conversation: " + convResp.getBody());
        Long conversationId = objectMapper.readTree(convResp.getBody()).get("id").asLong();

        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        WebSocketSession session = connectWebSocket(recipientAuth.getAccessToken(), received);
        assertTrue(session.isOpen(), "WebSocket should connect");

        String body = "Hello over WebSocket!";
        HttpHeaders msgHeaders = new HttpHeaders();
        msgHeaders.setBearerAuth(senderAuth.getAccessToken());
        msgHeaders.setContentType(MediaType.APPLICATION_JSON);
        String msgBody = "{\"body\":\"" + body.replace("\"", "\\\"") + "\"}";
        String msgUrl = "http://localhost:" + port + "/api/v1/conversations/" + conversationId + "/messages";
        ResponseEntity<String> msgResp = restTemplate.exchange(msgUrl, org.springframework.http.HttpMethod.POST, new HttpEntity<>(msgBody, msgHeaders), String.class);
        assertTrue(msgResp.getStatusCode().is2xxSuccessful(), "Send message: " + msgResp.getBody());

        String raw = received.poll(15, TimeUnit.SECONDS);
        assertNotNull(raw, "Expected one WebSocket message event after REST send");
        var eventNode = objectMapper.readTree(raw);
        assertEquals("message", eventNode.get("type").asText());
        assertEquals(conversationId, eventNode.get("conversationId").asLong());
        var payload = objectMapper.readTree(eventNode.get("payload").asText());
        assertEquals(body, payload.get("body").asText());
        assertEquals(senderAuth.getUserId(), payload.get("senderId").asLong());
        assertEquals(senderAuth.getUsername(), payload.get("senderUsername").asText());

        if (session.isOpen()) {
            session.close();
        }
    }

    private AuthResponse registerViaServer(String usernamePrefix) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = usernamePrefix + "_" + suffix;
        String email = username + "@test.com";

        AuthRequest request = new AuthRequest();
        request.setUsernameOrEmail(username);
        request.setEmail(email);
        request.setPassword("password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/auth/register",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), headers),
                String.class);
        assertTrue(resp.getStatusCode().is2xxSuccessful(), "Register: " + resp.getBody());
        return objectMapper.readValue(resp.getBody(), AuthResponse.class);
    }

    private WebSocketSession connectWebSocket(String token, BlockingQueue<String> received)
            throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        String wsUrl = "ws://localhost:" + port + "/api/v1/events?token=" + token;
        return client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                received.add(message.getPayload());
            }
        }, new WebSocketHttpHeaders(), URI.create(wsUrl)).get(10, TimeUnit.SECONDS);
    }

    private void assertHandshakeRejected(String wsUrl) {
        StandardWebSocketClient client = new StandardWebSocketClient();
        assertThrows(Exception.class, () ->
                client.execute(new TextWebSocketHandler() {}, new WebSocketHttpHeaders(), URI.create(wsUrl))
                        .get(10, TimeUnit.SECONDS));
    }

    private String createExpiredAccessToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date issuedAt = new Date(now.getTime() - 120_000);
        Date expiredAt = new Date(now.getTime() - 60_000);
        return Jwts.builder()
                .subject(username)
                .issuedAt(issuedAt)
                .expiration(expiredAt)
                .signWith(key)
                .compact();
    }
}
