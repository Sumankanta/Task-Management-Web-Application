package com.infy.tmwa.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @Column(nullable = false, length = 300)
    private String title;

    // ── Force Jackson to serialize as "isComplete" not "complete" ──
    @Column(name = "is_complete", nullable = false)
    @Builder.Default
    private boolean isComplete = false;

    @JsonProperty("isComplete")
    public boolean isComplete() { return isComplete; }

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"subtasks", "comments", "attachments", "timeLogs",
            "user", "assignee", "team", "hibernateLazyInitializer"})
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    @JsonIgnoreProperties({"password", "authorities", "teamMembers",
            "hibernateLazyInitializer", "handler"})
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({"password", "authorities", "teamMembers",
            "hibernateLazyInitializer", "handler"})
    private User createdBy;

    // ── Computed fields for frontend ──
    @JsonProperty("assigneeName")
    public String getAssigneeName() {
        return assignedTo != null ? assignedTo.getFullName() : null;
    }

    @JsonProperty("creatorId")
    public Long getCreatorId() {
        return createdBy != null ? createdBy.getId() : null;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}