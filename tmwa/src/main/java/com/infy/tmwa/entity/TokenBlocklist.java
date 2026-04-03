package com.infy.tmwa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_blocklist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlocklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** JWT ID claim — unique per token */
    @Column(nullable = false, unique = true, length = 100)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "revoked_at", nullable = false)
    @Builder.Default
    private LocalDateTime revokedAt = LocalDateTime.now();

    /** When the original JWT expires — lets you purge old rows safely */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}