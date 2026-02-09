package com.example.messenger.e2e;

import com.example.messenger.dto.AuthRequest;
import com.example.messenger.dto.AuthResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CompleteMessageFlowE2ETest extends BaseE2ETest {

    private static final String PASSWORD = "password123";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate http = new RestTemplate();

    @Test
    void shouldCompleteMessageFlowFromAuthToRealtimeDeliveryAndHistory() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserSession sender = registerAndLogin("sender_" + suffix, "sender_" + suffix + "@test.com");
        UserSession recipient = registerAndLogin("recipient_" + suffix, "recipient_" + suffix + "@test.com");

        long conversationId = createOneToOneConversation(sender, recipient);

        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<WebSocketSession> sessionRef = new AtomicReference<>();

        Thread wsThread = connectRecipientWebSocket(recipient.accessToken(), received, connected, sessionRef);
        assertTrue(connected.await(10, TimeUnit.SECONDS), "Recipient WebSocket should connect");

        String idempotencyKey = "idem-" + UUID.randomUUID();
        String body = "Hello from sender to recipient";

        try {
            JsonNode firstSend = sendMessage(conversationId, sender.accessToken(), body, idempotencyKey);
            assertEquals(body, firstSend.get("body").asText());
            assertEquals(sender.userId(), firstSend.get("senderId").asLong());

            String eventRaw = received.poll(15, TimeUnit.SECONDS);
            assertNotNull(eventRaw, "Expected WebSocket event after message send");
            JsonNode eventNode = objectMapper.readTree(eventRaw);
            assertEquals("message", eventNode.get("type").asText());
            assertEquals(conversationId, eventNode.get("conversationId").asLong());

            JsonNode payloadNode = objectMapper.readTree(eventNode.get("payload").asText());
            assertEquals(firstSend.get("id").asLong(), payloadNode.get("id").asLong());
            assertEquals(sender.userId(), payloadNode.get("senderId").asLong());
            assertEquals(sender.username(), payloadNode.get("senderUsername").asText());
            assertEquals(body, payloadNode.get("body").asText());

            JsonNode historyAfterFirstSend = listMessages(conversationId, recipient.accessToken());
            assertEquals(1, historyAfterFirstSend.get("items").size());
            JsonNode firstHistoryItem = historyAfterFirstSend.get("items").get(0);
            assertEquals(firstSend.get("id").asLong(), firstHistoryItem.get("id").asLong());
            assertEquals(body, firstHistoryItem.get("body").asText());
            assertTrue(historyAfterFirstSend.get("nextCursor").isNull(), "Single-message history should not have a next cursor");

            JsonNode secondSend = sendMessage(conversationId, sender.accessToken(), body, idempotencyKey);
            assertEquals(firstSend.get("id").asLong(), secondSend.get("id").asLong(),
                    "Idempotent resend should return the same persisted message");

            JsonNode historyAfterResend = listMessages(conversationId, sender.accessToken());
            assertEquals(1, historyAfterResend.get("items").size(),
                    "Idempotent resend must not create duplicate message rows");
        } finally {
            WebSocketSession session = sessionRef.get();
            if (session != null && session.isOpen()) {
                session.close();
            }
            wsThread.join(2000);
        }
    }

    private UserSession registerAndLogin(String username, String email) throws Exception {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsernameOrEmail(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(PASSWORD);

        ResponseEntity<String> registerResponse = postJson(url("/api/v1/auth/register"), registerRequest, null);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode(), "Register should succeed");
        AuthResponse registered = objectMapper.readValue(registerResponse.getBody(), AuthResponse.class);
        assertNotNull(registered.getAccessToken());

        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword(PASSWORD);

        ResponseEntity<String> loginResponse = postJson(url("/api/v1/auth/login"), loginRequest, null);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode(), "Login should succeed");

        AuthResponse loggedIn = objectMapper.readValue(loginResponse.getBody(), AuthResponse.class);
        return new UserSession(loggedIn.getUserId(), loggedIn.getUsername(), loggedIn.getAccessToken());
    }

    private long createOneToOneConversation(UserSession sender, UserSession recipient) throws Exception {
        String requestBody = "{\"type\":\"ONE_TO_ONE\",\"participantIds\":[" + sender.userId() + "," + recipient.userId() + "]}";
        HttpHeaders headers = authorizedJsonHeaders(sender.accessToken());

        ResponseEntity<String> response = http.exchange(
                url("/api/v1/conversations"),
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Conversation creation should succeed");
        JsonNode conversation = objectMapper.readTree(response.getBody());
        return conversation.get("id").asLong();
    }

    private JsonNode sendMessage(long conversationId, String accessToken, String body, String idempotencyKey) throws Exception {
        String escapedBody = body.replace("\"", "\\\"");
        String requestBody = "{\"body\":\"" + escapedBody + "\",\"idempotencyKey\":\"" + idempotencyKey + "\"}";

        ResponseEntity<String> response = http.exchange(
                url("/api/v1/conversations/" + conversationId + "/messages"),
                HttpMethod.POST,
                new HttpEntity<>(requestBody, authorizedJsonHeaders(accessToken)),
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Send message should succeed");
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode listMessages(long conversationId, String accessToken) throws Exception {
        ResponseEntity<String> response = http.exchange(
                url("/api/v1/conversations/" + conversationId + "/messages?limit=10"),
                HttpMethod.GET,
                new HttpEntity<>(authorizedJsonHeaders(accessToken)),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(), "List messages should succeed");
        return objectMapper.readTree(response.getBody());
    }

    private Thread connectRecipientWebSocket(
            String accessToken,
            BlockingQueue<String> received,
            CountDownLatch connected,
            AtomicReference<WebSocketSession> sessionRef
    ) {
        StandardWebSocketClient client = new StandardWebSocketClient();
        URI uri = URI.create("ws://localhost:" + port + "/api/v1/events?token=" + accessToken);

        Thread wsThread = new Thread(() -> {
            try {
                client.execute(new TextWebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) {
                        sessionRef.set(session);
                        connected.countDown();
                    }

                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                        received.add(message.getPayload());
                    }
                }, new WebSocketHttpHeaders(), uri);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        wsThread.start();
        return wsThread;
    }

    private ResponseEntity<String> postJson(String endpointUrl, Object body, String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }

        return http.exchange(
                endpointUrl,
                HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                String.class
        );
    }

    private HttpHeaders authorizedJsonHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private record UserSession(Long userId, String username, String accessToken) {}
}
