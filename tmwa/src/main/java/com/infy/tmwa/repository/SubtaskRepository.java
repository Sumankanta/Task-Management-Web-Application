package com.infy.tmwa.repository;

import com.infy.tmwa.entity.Subtask;
import com.infy.tmwa.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubtaskRepository extends JpaRepository<Subtask, Long> {

    // ── Ordered by created_at ASC per SRS ──
    List<Subtask> findByTaskOrderByCreatedAtAsc(Task task);

    // ── Count total subtasks for a task — used by summary endpoint ──
    int countByTask(Task task);

    // ── Count completed subtasks — used by summary endpoint ──
    int countByTaskAndIsComplete(Task task, boolean isComplete);
}