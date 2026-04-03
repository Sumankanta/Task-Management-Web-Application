package com.infy.tmwa.controller;

import com.infy.tmwa.config.JwtService;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService  jwtService;

    // ── Helper: extract JTI from the Authorization header ────────────────
    private String extractJti(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try { return jwtService.extractJti(header.substring(7)); }
            catch (Exception ignored) {}
        }
        return "unknown";
    }

    // ── GET /api/users/me/profile ─────────────────────────────────────────
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user));
    }

    // ── PATCH /api/users/me/profile ───────────────────────────────────────
    @PatchMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(userService.updateProfile(user, body));
    }

    // ── PATCH /api/users/me/password ──────────────────────────────────────
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        userService.changePassword(
                user,
                body.get("currentPassword"),
                body.get("newPassword")
        );
        return ResponseEntity.ok().build();
    }

    // ── GET /api/users/me/sessions ────────────────────────────────────────
    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> getSessions(
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                userService.getSessions(user, extractJti(request)));
    }

    // ── DELETE /api/users/me/sessions/{jti} ──────────────────────────────
    @DeleteMapping("/sessions/{jti}")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal User user,
            @PathVariable String jti) {
        userService.revokeSession(user, jti);
        return ResponseEntity.ok().build();
    }

    // ── DELETE /api/users/me/sessions  (revoke all others) ───────────────
    @DeleteMapping("/sessions")
    public ResponseEntity<Void> revokeAllOtherSessions(
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        userService.revokeAllOtherSessions(user, extractJti(request));
        return ResponseEntity.ok().build();
    }

    // ── PATCH /api/users/me/preferences ──────────────────────────────────
    @PatchMapping("/preferences")
    public ResponseEntity<Void> updatePreferences(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        userService.updatePreferences(user, body);
        return ResponseEntity.ok().build();
    }

    // ── DELETE /api/users/me ──────────────────────────────────────────────
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal User user) {
        userService.deleteAccount(user);
        return ResponseEntity.ok().build();
    }
}