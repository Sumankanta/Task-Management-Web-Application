package com.infy.tmwa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// ── One row = one running timer ──
// start_time is stored in DB so the frontend can resume the counter
// after a page refresh by computing (now - start_time)
@Entity
@Table(
        name = "active_timers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "user_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ActiveTimer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── UNIQUE(task_id, user_id) — only one active timer per task per user ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── Set when timer started — used to compute duration on stop ──
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
}