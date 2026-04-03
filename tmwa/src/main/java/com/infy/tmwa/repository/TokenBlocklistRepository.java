package com.infy.tmwa.repository;

import com.infy.tmwa.entity.TokenBlocklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TokenBlocklistRepository extends JpaRepository<TokenBlocklist, Long> {

    boolean existsByJti(String jti);

    List<TokenBlocklist> findAllByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenBlocklist t WHERE t.userId = :userId AND t.jti <> :currentJti")
    void revokeAllExcept(Long userId, String currentJti);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}