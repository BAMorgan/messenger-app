package com.example.messenger.websocket;

import com.example.messenger.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageWebSocketHandlerTest {

    @Mock
    private EventService eventService;

    @Mock
    private WebSocketSession session;

    private MessageWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MessageWebSocketHandler(eventService);
    }

    @Test
    void afterConnectionEstablished_registersSessionWhenUserIdPresent() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebSocketAuthInterceptor.getUserIdAttr(), 7L);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn("s1");

        handler.afterConnectionEstablished(session);

        verify(eventService).registerSession(7L, session);
        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void afterConnectionEstablished_closesSessionWhenUserIdMissing() throws Exception {
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.getId()).thenReturn("s2");

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(eventService, never()).registerSession(anyLong(), any(WebSocketSession.class));
    }

    @Test
    void afterConnectionEstablished_swallowsCloseExceptionWhenUserIdMissing() throws Exception {
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.getId()).thenReturn("s3");
        doThrow(new IOException("close failed")).when(session).close(CloseStatus.POLICY_VIOLATION);

        assertDoesNotThrow(() -> handler.afterConnectionEstablished(session));

        verify(eventService, never()).registerSession(anyLong(), any(WebSocketSession.class));
    }

    @Test
    void afterConnectionClosed_unregistersSessionWhenUserIdPresent() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebSocketAuthInterceptor.getUserIdAttr(), 9L);
        when(session.getAttributes()).thenReturn(attributes);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(eventService).unregisterSession(9L, session);
    }

    @Test
    void afterConnectionClosed_doesNothingWhenUserIdMissing() {
        when(session.getAttributes()).thenReturn(new HashMap<>());

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(eventService, never()).unregisterSession(anyLong(), any(WebSocketSession.class));
    }
}
