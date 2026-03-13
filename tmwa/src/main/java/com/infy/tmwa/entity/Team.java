package com.infy.tmwa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── Manager owns this team — ON DELETE CASCADE ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ── Members via junction table ──
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamMember> members = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}