package com.infy.tmwa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subtasks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Subtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Parent task — ON DELETE CASCADE ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false, length = 300)
    private String title;

    // ── Toggled by PATCH /api/subtasks/{id}/toggle ──
    @Column(name = "is_complete", nullable = false)
    @Builder.Default
    private boolean isComplete = false;

    // ── Optional assignee — ON DELETE SET NULL ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    // ── Creator — for delete permission check ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ── Set when is_complete flipped to true, cleared when flipped back ──
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}