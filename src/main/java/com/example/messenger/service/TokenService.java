package com.example.messenger.service;

import com.example.messenger.config.JwtConfig;
import com.example.messenger.domain.RefreshToken;
import com.example.messenger.repository.RefreshTokenRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDetailsService userDetailsService;
    private final JwtConfig jwtConfig;

    public TokenService(RefreshTokenRepository refreshTokenRepository, UserDetailsService userDetailsService, JwtConfig jwtConfig) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userDetailsService = userDetailsService;
        this.jwtConfig = jwtConfig;
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        // Revoke existing tokens for this user (token rotation)
        refreshTokenRepository.deleteByUserId(userId);

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(jwtConfig.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = new RefreshToken(userId, token, expiresAt);
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
