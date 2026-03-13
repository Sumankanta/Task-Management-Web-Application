package com.infy.tmwa.repository;

import com.infy.tmwa.entity.ActiveTimer;
import com.infy.tmwa.entity.Task;
import com.infy.tmwa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActiveTimerRepository extends JpaRepository<ActiveTimer, Long> {

    // ── Check if timer already running for this task+user ──
    // Used by start endpoint to return 409 if already active
    Optional<ActiveTimer> findByTaskAndUser(Task task, User user);

    // ── Used by stop endpoint to find and delete the active timer row ──
    void deleteByTaskAndUser(Task task, User user);
}