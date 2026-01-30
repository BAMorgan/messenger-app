package com.example.messenger.websocket;

import com.example.messenger.domain.AppUser;
import com.example.messenger.repository.AppUserRepository;
import com.example.messenger.security.JwtTokenProvider;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Validates JWT during WebSocket handshake. Token is read from query parameter {@code token}.
 * On success, stores the authenticated user's ID in handshake attributes for the handler.
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final String USER_ID_ATTR = "userId";

    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserRepository appUserRepository;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider, AppUserRepository appUserRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            if (jwtTokenProvider.isTokenExpired(token)) {
                return false;
            }
            String username = jwtTokenProvider.getUsernameFromToken(token);
            AppUser user = appUserRepository.findByUsername(username)
                    .orElseGet(() -> appUserRepository.findByEmail(username).orElse(null));
            if (user == null) {
                return false;
            }
            attributes.put(USER_ID_ATTR, user.getId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception ex
    ) {}

    public static String getUserIdAttr() {
        return USER_ID_ATTR;
    }
}
