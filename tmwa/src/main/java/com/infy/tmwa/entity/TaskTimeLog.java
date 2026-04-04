package com.infy.tmwa.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_time_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TaskTimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"subtasks", "comments", "attachments", "timeLogs",
            "user", "assignee", "team", "hibernateLazyInitializer"})
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by", nullable = false)
    @JsonIgnoreProperties({"password", "authorities", "teamMembers",
            "hibernateLazyInitializer", "handler"})
    private User loggedBy;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(length = 500)
    private String note;

    @Column(name = "is_manual", nullable = false)
    @Builder.Default
    private boolean isManual = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Computed fields so frontend gets loggedByName directly ──────────
    @JsonProperty("loggedByName")
    public String getLoggedByName() {
        return loggedBy != null ? loggedBy.getFullName() : null;
    }

    @JsonProperty("loggedById")
    public Long getLoggedById() {
        return loggedBy != null ? loggedBy.getId() : null;
    }
}