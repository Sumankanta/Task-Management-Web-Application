package com.infy.tmwa.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Recipient ──────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "authorities", "teamMembers",
            "hibernateLazyInitializer", "handler"})
    private User user;

    // ── Content ────────────────────────────────────────────────────
    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    // Type: TASK_ASSIGNED | TEAM_ADDED | SUBTASK_DONE | COMMENT | GENERAL
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String type = "GENERAL";

    // ── State ──────────────────────────────────────────────────────
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Serialization fixes (Lombok boolean getter stripping) ──────
    @JsonProperty("isRead")
    public boolean isRead() { return isRead; }

    @JsonProperty("userId")
    public Long getUserId() { return user != null ? user.getId() : null; }
}