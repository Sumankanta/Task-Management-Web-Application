package com.infy.tmwa.controller;

import com.infy.tmwa.entity.User;
import com.infy.tmwa.entity.UserRole;
import com.infy.tmwa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// ── F-W2-01: Admin Panel endpoints ──
// All methods here are Admin-only — also enforced at route level in SecurityConfig
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class AdminController {

    private final UserRepository userRepository;

    // ── GET /api/admin/users ──
    // Returns all users with role, isActive, email, fullName
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers(
            @AuthenticationPrincipal User admin) {

        log.info("Admin {} fetching all users", admin.getEmail());
        return ResponseEntity.ok(userRepository.findAll());
    }

    // ── PATCH /api/admin/users/{id}/role ──
    // Changes a user's role. Takes effect on user's next login (new JWT issued).
    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> changeRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User admin) {

        String roleStr = body.get("role");
        log.info("Admin {} changing role of user {} to {}", admin.getEmail(), id, roleStr);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        try {
            user.setRole(UserRole.valueOf(roleStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + roleStr
                    + ". Must be one of: ADMIN, MANAGER, MEMBER, VIEWER");
        }

        User updated = userRepository.save(user);
        log.info("Role updated: user={}, newRole={}", user.getEmail(), updated.getRole());
        return ResponseEntity.ok(updated);
    }

    // ── PATCH /api/admin/users/{id}/status ──
    // Activates or deactivates a user account.
    // Deactivated users: isActive=false, blocked in JwtAuthFilter via isEnabled()
    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> changeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal User admin) {

        boolean isActive = body.get("isActive");
        log.info("Admin {} setting isActive={} for user {}", admin.getEmail(), isActive, id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        // Prevent admin from deactivating themselves
        if (user.getId().equals(admin.getId()) && !isActive) {
            throw new RuntimeException("Cannot deactivate your own account");
        }

        user.setActive(isActive);
        User updated = userRepository.save(user);
        log.info("Status updated: user={}, isActive={}", user.getEmail(), updated.isActive());
        return ResponseEntity.ok(updated);
    }

    // ── DELETE /api/admin/users/{id} ──
    // Permanently deletes a user account
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {

        log.info("Admin {} deleting user {}", admin.getEmail(), id);

        if (id.equals(admin.getId())) {
            throw new RuntimeException("Cannot delete your own account via admin panel");
        }

        userRepository.deleteById(id);
        log.info("User {} deleted by admin {}", id, admin.getEmail());
        return ResponseEntity.noContent().build();
    }
}