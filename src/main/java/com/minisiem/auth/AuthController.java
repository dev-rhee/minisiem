package com.minisiem.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepo;

    @Transactional
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse res) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "아이디 또는 비밀번호가 틀렸습니다"));
        }

        String accessToken  = jwtProvider.createAccessToken(req.getUsername());
        String refreshToken = jwtProvider.createRefreshToken(req.getUsername());
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshExpiryMs() / 1000);

        RefreshToken rt = refreshTokenRepo.findByUsername(req.getUsername())
                .map(existing -> { existing.rotate(refreshToken, expiresAt); return existing; })
                .orElseGet(() -> RefreshToken.builder()
                        .username(req.getUsername())
                        .token(refreshToken)
                        .expiresAt(expiresAt)
                        .build());
        refreshTokenRepo.save(rt);

        res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken).toString());
        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }

    @Transactional
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(value = "refreshToken", required = false) String token,
            HttpServletResponse res) {

        if (token == null || !jwtProvider.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = jwtProvider.getUsername(token);
        RefreshToken stored = refreshTokenRepo.findByUsername(username).orElse(null);

        if (stored == null || !stored.getToken().equals(token)
                || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String newAccess   = jwtProvider.createAccessToken(username);
        String newRefresh  = jwtProvider.createRefreshToken(username);
        LocalDateTime newExpiry = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshExpiryMs() / 1000);
        stored.rotate(newRefresh, newExpiry);
        refreshTokenRepo.save(stored);

        res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(newRefresh).toString());
        return ResponseEntity.ok(Map.of("accessToken", newAccess));
    }

    @Transactional
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(value = "refreshToken", required = false) String token,
            HttpServletResponse res) {
        if (token != null) {
            try {
                String username = jwtProvider.getUsername(token);
                refreshTokenRepo.findByUsername(username).ifPresent(refreshTokenRepo::delete);
            } catch (Exception ignored) {}
        }
        res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("").toString());
        return ResponseEntity.ok().build();
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(false)   // 운영(HTTPS) 환경에서는 true
                .sameSite("Lax")
                .maxAge(value.isEmpty() ? Duration.ZERO : Duration.ofMillis(jwtProvider.getRefreshExpiryMs()))
                .path("/auth")
                .build();
    }
}
