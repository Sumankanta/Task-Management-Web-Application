package com.infy.tmwa.controller;

import com.infy.tmwa.dto.UserDTO;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class AdminController {

    private final UserRepository userRepository;

    // ── GET /api/admin/users ──
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @AuthenticationPrincipal User admin) {

        log.info("Admin {} fetching all users", admin.getEmail());
        List<UserDTO> users = userRepository.findAll()
                .stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // ── PATCH /api/admin/users/{id}/role ──
    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> changeRole(
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
            throw new RuntimeException("Invalid role: " + roleStr);
        }

        return ResponseEntity.ok(new UserDTO(userRepository.save(user)));
    }

    // ── PATCH /api/admin/users/{id}/status ──
    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> changeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal User admin) {

        boolean isActive = body.get("isActive");
        log.info("Admin {} setting isActive={} for user {}", admin.getEmail(), isActive, id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        if (user.getId().equals(admin.getId()) && !isActive) {
            throw new RuntimeException("Cannot deactivate your own account");
        }

        user.setActive(isActive);
        return ResponseEntity.ok(new UserDTO(userRepository.save(user)));
    }

    // ── DELETE /api/admin/users/{id} ──
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
        return ResponseEntity.noContent().build();
    }
}