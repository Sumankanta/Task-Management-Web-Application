package com.infy.tmwa.repository;

import com.infy.tmwa.entity.Task;
import com.infy.tmwa.entity.TaskPriority;
import com.infy.tmwa.entity.TaskStatus;
import com.infy.tmwa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);

    int countByUser(User user);

    int countByUserAndStatus(User user, TaskStatus status);

    int countByUserAndPriority(User user, TaskPriority priority);

    // ── FIX: compare LocalDate field with a LocalDate parameter ──
    @Query("""
            SELECT COUNT(t)
            FROM Task t
            WHERE t.user = :user
            AND t.dueDate < :today
            AND t.status <> com.infy.tmwa.entity.TaskStatus.DONE
           """)
    int countOverdueTasks(
            @Param("user") User user,
            @Param("today") LocalDate today   // ← pass today's date explicitly
    );

    // ── FIX: @Param added so JPQL can bind :date correctly ──
    @Query("""
           SELECT COUNT(t)
           FROM Task t
           WHERE t.user = :user
           AND t.createdAt >= :date
           """)
    int countTasksThisWeek(
            @Param("user") User user,
            @Param("date") LocalDateTime date
    );
}