package com.infy.tmwa.entity;

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
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by", nullable = false)
    private User loggedBy;

    // ── Always stored as minutes (e.g. 90 = 1h 30m) ──
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(length = 500)
    private String note;

    // ── FALSE = created by timer stop, TRUE = manual entry ──
    // Timer-generated entries cannot be deleted per SRS
    @Column(name = "is_manual", nullable = false)
    @Builder.Default
    private boolean isManual = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}