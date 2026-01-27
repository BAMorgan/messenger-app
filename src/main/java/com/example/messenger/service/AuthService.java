package com.example.messenger.service;

import com.example.messenger.domain.AppUser;
import com.example.messenger.domain.RefreshToken;
import com.example.messenger.dto.AuthRequest;
import com.example.messenger.dto.AuthResponse;
import com.example.messenger.repository.AppUserRepository;
import com.example.messenger.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            TokenService tokenService,
            AuthenticationManager authenticationManager) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        // Validate email is provided for registration
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required for registration");
        }

        // Check if user already exists
        if (appUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        if (appUserRepository.findByUsername(request.getUsernameOrEmail()).isPresent()) {
            throw new IllegalArgumentException("User with this username already exists");
        }

        // Create new user
        String passwordHash = passwordEncoder.encode(request.getPassword());
        AppUser user = new AppUser(request.getUsernameOrEmail(), request.getEmail(), passwordHash);
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }

        user = appUserRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = tokenService.createRefreshToken(user.getId());
        String refreshTokenString = refreshToken.getToken();

        return new AuthResponse(accessToken, refreshTokenString, user.getId(), user.getUsername(), user.getEmail());
    }

    public AuthResponse login(AuthRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        AppUser user = appUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        RefreshToken refreshToken = tokenService.createRefreshToken(user.getId());
        String refreshTokenString = refreshToken.getToken();

        return new AuthResponse(accessToken, refreshTokenString, user.getId(), user.getUsername(), user.getEmail());
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        // Validate refresh token
        RefreshToken refreshToken = tokenService.validateRefreshToken(refreshTokenString);

        // Get user
        AppUser user = appUserRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Revoke old token (token rotation)
        tokenService.revokeRefreshToken(refreshTokenString);

        // Generate new tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken newRefreshToken = tokenService.createRefreshToken(user.getId());

        return new AuthResponse(accessToken, newRefreshToken.getToken(), user.getId(), user.getUsername(), user.getEmail());
    }
}
