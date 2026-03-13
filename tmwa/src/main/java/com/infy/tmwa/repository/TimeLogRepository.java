package com.infy.tmwa.repository;

import com.infy.tmwa.entity.Task;
import com.infy.tmwa.entity.TaskTimeLog;
import com.infy.tmwa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeLogRepository extends JpaRepository<TaskTimeLog, Long> {

    // ── Ordered by log_date DESC per SRS ──
    List<TaskTimeLog> findByTaskOrderByLogDateDesc(Task task);

    // ── SUM all duration_minutes for a task — used by total endpoint ──
    @Query("SELECT COALESCE(SUM(t.durationMinutes), 0) FROM TaskTimeLog t WHERE t.task = :task")
    int sumDurationByTask(@Param("task") Task task);
}