package com.infy.tmwa.repository;

import com.infy.tmwa.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);
    List<Task> findByAssignee(User assignee);

    /**
     * All tasks visible to a user:
     *   1. Tasks they created
     *   2. Tasks directly assigned to them
     *   3. Tasks assigned to a team they belong to
     */
    @Query("""
        SELECT DISTINCT t FROM Task t
        LEFT JOIN TeamMember tm ON tm.team = t.team AND tm.user = :user
        WHERE t.user = :user
           OR t.assignee = :user
           OR (t.team IS NOT NULL AND tm.user = :user)
        """)
    List<Task> findAllVisibleToUser(@Param("user") User user);

    // Tasks for a specific team
    @Query("SELECT t FROM Task t WHERE t.team.id = :teamId")
    List<Task> findByTeamId(@Param("teamId") Long teamId);

    List<Task> findByTeam(Team team);

    // Analytics
    int countByUser(User user);
    int countByUserAndStatus(User user, TaskStatus status);
    int countByUserAndPriority(User user, TaskPriority priority);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.user = :user AND t.dueDate < :today AND t.status != 'DONE'")
    int countOverdueTasks(@Param("user") User user, @Param("today") LocalDate today);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.user = :user AND t.createdAt >= :since")
    int countTasksThisWeek(@Param("user") User user, @Param("since") LocalDateTime since);
}