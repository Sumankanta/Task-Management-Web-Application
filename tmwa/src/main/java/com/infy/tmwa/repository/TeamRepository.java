package com.infy.tmwa.repository;

import com.infy.tmwa.entity.Team;
import com.infy.tmwa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    // ── Manager's own teams ──
    List<Team> findByManager(User manager);

    // ── All teams a user manages ──
    boolean existsByIdAndManager(Long id, User manager);
}