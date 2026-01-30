package com.example.messenger.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the WebSocket endpoint at /api/v1/events. Authentication is performed
 * via query parameter {@code token} (JWT) in the handshake.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final MessageWebSocketHandler messageWebSocketHandler;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor, MessageWebSocketHandler messageWebSocketHandler) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.messageWebSocketHandler = messageWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageWebSocketHandler, "/api/v1/events")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("*");
    }
}
