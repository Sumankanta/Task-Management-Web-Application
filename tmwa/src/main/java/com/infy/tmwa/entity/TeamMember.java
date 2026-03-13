package com.infy.tmwa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// ── Junction table: users ↔ teams ──
// Composite PK on (team_id, user_id)
@Entity
@Table(name = "team_members")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@IdClass(TeamMemberId.class)
public class TeamMember {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @PrePersist
    public void onJoin() {
        this.joinedAt = LocalDateTime.now();
    }
}