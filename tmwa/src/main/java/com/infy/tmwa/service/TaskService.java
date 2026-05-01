package com.infy.tmwa.service;

import com.infy.tmwa.dto.TaskDTO;
import com.infy.tmwa.dto.TaskSummaryDTO;
import com.infy.tmwa.entity.*;
import com.infy.tmwa.repository.TaskRepository;
import com.infy.tmwa.repository.TeamRepository;
import com.infy.tmwa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository         taskRepository;
    private final UserRepository         userRepository;
    private final TeamRepository         teamRepository;
    private final ActivityService        activityService;
    private final NotificationService    notificationService;

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','MEMBER','VIEWER')")
    public List<Task> getTasks(User user) {
        log.info("Fetching all tasks for user: {}", user.getEmail());
        List<Task> tasks = taskRepository.findAllVisibleToUser(user);
        log.info("Found {} task(s) for user: {}", tasks.size(), user.getEmail());
        return tasks;
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','MEMBER','VIEWER')")
    public List<Task> getTasksByTeam(Long teamId) {
        return taskRepository.findByTeamId(teamId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','MEMBER','VIEWER')")
    public Task getTaskById(Long id, User user) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        boolean isOwner    = task.getUser().getId().equals(user.getId());
        boolean isAssignee = task.getAssignee() != null
                && task.getAssignee().getId().equals(user.getId());
        if (!isOwner && !isAssignee) throw new RuntimeException("Forbidden");
        return task;
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','MEMBER')")
    public Task createTask(TaskDTO dto, User user) {
        log.info("Creating task '{}' for user {}", dto.getTitle(), user.getEmail());

        Task task = Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .status(dto.getStatus() != null ? dto.getStatus() : TaskStatus.TODO)
                .priority(dto.getPriority() != null ? dto.getPriority() : TaskPriority.MEDIUM)
                .user(user)
                .build();

        if (dto.getAssignedTo() != null) {
            User assignee = userRepository.findById(dto.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            task.setAssignee(assignee);
        }

        if (dto.getTeamId() != null) {
            Team team = teamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not found"));
            task.setTeam(team);
        }

        Task saved = taskRepository.save(task);
        log.info("Task created with id {}", saved.getId());

        activityService.logActivity(user, saved.getId(), "TASK_CREATED",
                user.getFullName() + " created task \"" + saved.getTitle() + "\"");

        // ── Notify assignee (only if different from creator) ──
        if (saved.getAssignee() != null
                && !saved.getAssignee().getId().equals(user.getId())) {
            notificationService.create(
                    saved.getAssignee(),
                    "Task Assigned",
                    user.getFullName() + " assigned you \"" + saved.getTitle() + "\"",
                    "TASK_ASSIGNED"
            );
        }

        return saved;
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','MEMBER')")
    public Task updateTask(Long id, TaskDTO dto, User user) {
        Task task = getTaskById(id, user);

        TaskStatus   oldStatus   = task.getStatus();
        TaskPriority oldPriority = task.getPriority();
        User         oldAssignee = task.getAssignee();

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDueDate(dto.getDueDate());
        task.setStatus(dto.getStatus());
        if (dto.getPriority() != null) task.setPriority(dto.getPriority());

        if (dto.getAssignedTo() != null) {
            User assignee = userRepository.findById(dto.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            task.setAssignee(assignee);
        } else {
            task.setAssignee(null);
        }

        if (dto.getTeamId() != null) {
            Team team = teamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not found"));
            task.setTeam(team);
        } else {
            task.setTeam(null);
        }

        Task updated = taskRepository.save(task);

        // Activity logs
        if (!oldStatus.equals(updated.getStatus())) {
            activityService.logActivity(user, updated.getId(), "TASK_STATUS_CHANGED",
                    user.getFullName() + " changed status of \"" + updated.getTitle()
                            + "\" to " + formatStatus(updated.getStatus()));
        }
        if (dto.getPriority() != null && !oldPriority.equals(updated.getPriority())) {
            activityService.logActivity(user, updated.getId(), "TASK_PRIORITY_CHANGED",
                    user.getFullName() + " changed priority of \"" + updated.getTitle()
                            + "\" to " + formatPriority(updated.getPriority()));
        }

        Long oldId = oldAssignee != null ? oldAssignee.getId() : null;
        Long newId = updated.getAssignee() != null ? updated.getAssignee().getId() : null;
        if (!java.util.Objects.equals(oldId, newId) && updated.getAssignee() != null) {
            activityService.logActivity(user, updated.getId(), "TASK_ASSIGNED",
                    user.getFullName() + " assigned \"" + updated.getTitle()
                            + "\" to " + updated.getAssignee().getFullName());

            // ── Notify new assignee (only if different from updater) ──
            if (!updated.getAssignee().getId().equals(user.getId())) {
                notificationService.create(
                        updated.getAssignee(),
                        "Task Assigned",
                        user.getFullName() + " assigned you \"" + updated.getTitle() + "\"",
                        "TASK_ASSIGNED"
                );
            }
        }

        return updated;
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','MEMBER')")
    public void deleteTask(Long id, User user) {
        Task task = getTaskById(id, user);
        String title = task.getTitle();
        taskRepository.delete(task);
        activityService.logActivity(user, null, "TASK_DELETED",
                user.getFullName() + " deleted task \"" + title + "\"");
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','MEMBER','VIEWER')")
    public TaskSummaryDTO getSummary(User user) {
        int totalTasks    = taskRepository.countByUser(user);
        int todo          = taskRepository.countByUserAndStatus(user, TaskStatus.TODO);
        int inProgress    = taskRepository.countByUserAndStatus(user, TaskStatus.IN_PROGRESS);
        int done          = taskRepository.countByUserAndStatus(user, TaskStatus.DONE);
        int high          = taskRepository.countByUserAndPriority(user, TaskPriority.HIGH);
        int medium        = taskRepository.countByUserAndPriority(user, TaskPriority.MEDIUM);
        int low           = taskRepository.countByUserAndPriority(user, TaskPriority.LOW);
        int overdue       = taskRepository.countOverdueTasks(user, java.time.LocalDate.now());
        int tasksThisWeek = taskRepository.countTasksThisWeek(user, LocalDateTime.now().minusDays(7));

        double completionRate = totalTasks > 0
                ? Math.round(((double) done / totalTasks) * 1000.0) / 10.0 : 0;

        return new TaskSummaryDTO(totalTasks, todo, inProgress, done,
                high, medium, low, completionRate, overdue, tasksThisWeek);
    }

    private String formatStatus(TaskStatus s) {
        return switch (s) { case TODO -> "To-Do"; case IN_PROGRESS -> "In Progress"; case DONE -> "Done"; };
    }
    private String formatPriority(TaskPriority p) {
        return switch (p) { case HIGH -> "High"; case MEDIUM -> "Medium"; case LOW -> "Low"; };
    }
}