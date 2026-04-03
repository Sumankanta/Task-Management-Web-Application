package com.infy.tmwa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String password;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.MEMBER;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ── Profile extras ──────────────────────────────────────────────
    @Column(length = 300)
    private String bio;

    @Column(name = "avatar_color", length = 20)
    @Builder.Default
    private String avatarColor = "#6366f1";

    // ── Preferences stored as JSON strings ──────────────────────────
    @Column(name = "theme", length = 20)
    @Builder.Default
    private String theme = "SYSTEM";

    // Stored as JSON: {"taskAssigned":true,"commentOnTask":true,...}
    @Column(name = "notifications_json", length = 500)
    private String notificationsJson;

    // ────────────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getUsername()              { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return isActive; }
}