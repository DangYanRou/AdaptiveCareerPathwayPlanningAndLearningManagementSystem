package com.tbm.careerpathlearning.controller;

import com.tbm.careerpathlearning.dto.LoginRequest;
import com.tbm.careerpathlearning.dto.ResetRequest;
import com.tbm.careerpathlearning.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Value("${cookies.expiry.day}")
    private Integer COOKIES_EXPIRY_DAYS;

    @Value("${cookies.secure:true}")
    private boolean cookieSecure;

    @Autowired
    private AuthService authService;

    @Autowired
    private MessageSource messageSource;

    private static final String LOGIN_OK = "login.ok.msg";
    private static final String RESET_PASSWORD_EMAIL_OK = "reset.password.email.ok";
    private static final String RESET_PASSWORD_UPDATE_OK = "reset.password.update.ok";
    private static final String REFRESH_OK = "refresh.ok.msg";
    private static final String FORBIDDEN_ERR_MSG_CODE = "forbidden.request.err.msg";

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse response) throws Exception {
        AuthService.LoginResult result = authService.login(req);
        setRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(LOGIN_OK, null, Locale.getDefault()),
                "userId", result.userId().toString(),
                "roles", result.roles(),
                "accessToken", result.accessToken()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String refreshToken = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "sb-refresh".equals(c.getName()))
                .map(Cookie::getValue).findFirst().orElse(null);

        AuthService.RefreshResult result = authService.refresh(refreshToken);
        setRefreshTokenCookie(response, result.newRefreshToken());
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(REFRESH_OK, null, Locale.getDefault()),
                "userId", result.userId(),
                "roles", result.roles(),
                "accessToken", result.accessToken()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie sbRefreshCookie = ResponseCookie.from("sb-refresh", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, sbRefreshCookie.toString());
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(messageSource.getMessage(FORBIDDEN_ERR_MSG_CODE, null, Locale.getDefault()));
        }
        return ResponseEntity.ok(Map.of(
                "userId", authentication.getPrincipal(),
                "roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) throws Exception {
        authService.forgotPassword(email);
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(RESET_PASSWORD_EMAIL_OK, null, Locale.getDefault())));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetRequest req) throws Exception {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(RESET_PASSWORD_UPDATE_OK, null, Locale.getDefault())));
    }

    @PostMapping("/first-time-login")
    public ResponseEntity<?> firstTimeLogin(@RequestParam String email) throws Exception {
        authService.firstTimeLogin(email);
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(RESET_PASSWORD_EMAIL_OK, null, Locale.getDefault())));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("sb-refresh", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(COOKIES_EXPIRY_DAYS))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
