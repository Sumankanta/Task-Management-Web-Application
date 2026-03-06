package com.infy.tmwa.service;

import com.infy.tmwa.dto.TaskDTO;
import com.infy.tmwa.dto.TaskSummaryDTO;
import com.infy.tmwa.entity.Task;
import com.infy.tmwa.entity.TaskPriority;
import com.infy.tmwa.entity.TaskStatus;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.repository.TaskRepository;
import com.infy.tmwa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository   taskRepository;
    private final UserRepository   userRepository;
    private final ActivityService  activityService;   // ← injected

    public List<Task> getTasks(User user) {
        log.info("Fetching all tasks for user: {}", user.getEmail());
        List<Task> tasks = taskRepository.findByUser(user);
        log.info("Found {} task(s) for user: {}", tasks.size(), user.getEmail());
        return tasks;
    }

    public Task getTaskById(Long id, User user) {
        log.info("Fetching task id: {} for user: {}", id, user.getEmail());
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Task id {} not found", id);
                    return new RuntimeException("Task not found");
                });
        if (!task.getUser().getId().equals(user.getId())) {
            log.warn("Access denied for user {} on task {}", user.getEmail(), id);
            throw new RuntimeException("Forbidden");
        }
        return task;
    }

    public Task createTask(TaskDTO dto, User user) {
        log.info("Creating task '{}' for user {}", dto.getTitle(), user.getEmail());

        Task task = Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .status(dto.getStatus() != null ? dto.getStatus() : TaskStatus.TODO)
                .user(user)
                .build();

        // Priority
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : TaskPriority.MEDIUM);
        log.info("Task priority set to {}", task.getPriority());

        // Assignee
        if (dto.getAssignedTo() != null) {
            User assignee = userRepository.findById(dto.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            task.setAssignee(assignee);
            log.info("Task assigned to user id {}", assignee.getId());
        } else {
            task.setAssignee(null);
            log.info("Task created without assignee");
        }

        Task saved = taskRepository.save(task);
        log.info("Task created successfully with id {}", saved.getId());

        // ── F-EXT-05: Log TASK_CREATED ──
        activityService.logActivity(
                user,
                saved.getId(),
                "TASK_CREATED",
                user.getFullName() + " created task \"" + saved.getTitle() + "\""
        );

        return saved;
    }

    public Task updateTask(Long id, TaskDTO dto, User user) {
        log.info("Updating task id {} for user {}", id, user.getEmail());

        Task task = getTaskById(id, user);

        // Capture old values BEFORE updating — needed for change detection
        TaskStatus   oldStatus   = task.getStatus();
        TaskPriority oldPriority = task.getPriority();
        User         oldAssignee = task.getAssignee();

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDueDate(dto.getDueDate());
        task.setStatus(dto.getStatus());

        // Priority
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
            log.info("Task priority updated to {}", dto.getPriority());
        }

        // Assignee
        if (dto.getAssignedTo() != null) {
            User assignee = userRepository.findById(dto.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            task.setAssignee(assignee);
            log.info("Task reassigned to user id {}", assignee.getId());
        } else {
            task.setAssignee(null);
            log.info("Task marked as unassigned");
        }

        Task updated = taskRepository.save(task);
        log.info("Task id {} updated successfully", updated.getId());

        // ── F-EXT-05: Log what actually changed ──

        // Status changed?
        if (!oldStatus.equals(updated.getStatus())) {
            activityService.logActivity(
                    user,
                    updated.getId(),
                    "TASK_STATUS_CHANGED",
                    user.getFullName() + " changed status of \"" + updated.getTitle()
                            + "\" to " + formatStatus(updated.getStatus())
            );
        }

        // Priority changed?
        if (dto.getPriority() != null && !oldPriority.equals(updated.getPriority())) {
            activityService.logActivity(
                    user,
                    updated.getId(),
                    "TASK_PRIORITY_CHANGED",
                    user.getFullName() + " changed priority of \"" + updated.getTitle()
                            + "\" to " + formatPriority(updated.getPriority())
            );
        }

        // Assignee changed?
        Long oldAssigneeId  = oldAssignee  != null ? oldAssignee.getId()           : null;
        Long newAssigneeId  = updated.getAssignee() != null ? updated.getAssignee().getId() : null;
        if (!java.util.Objects.equals(oldAssigneeId, newAssigneeId) && updated.getAssignee() != null) {
            activityService.logActivity(
                    user,
                    updated.getId(),
                    "TASK_ASSIGNED",
                    user.getFullName() + " assigned \"" + updated.getTitle()
                            + "\" to " + updated.getAssignee().getFullName()
            );
        }

        return updated;
    }

    public void deleteTask(Long id, User user) {
        log.info("Deleting task id {} for user {}", id, user.getEmail());

        Task task = getTaskById(id, user);
        String title = task.getTitle();   // capture title before deletion

        taskRepository.delete(task);
        log.info("Task id {} deleted successfully", id);

        // ── F-EXT-05: Log TASK_DELETED (taskId = null — task no longer exists) ──
        activityService.logActivity(
                user,
                null,
                "TASK_DELETED",
                user.getFullName() + " deleted task \"" + title + "\""
        );
    }

    public TaskSummaryDTO getSummary(User user) {
        log.info("Generating analytics summary for user {}", user.getEmail());

        int totalTasks  = taskRepository.countByUser(user);
        int todo        = taskRepository.countByUserAndStatus(user, TaskStatus.TODO);
        int inProgress  = taskRepository.countByUserAndStatus(user, TaskStatus.IN_PROGRESS);
        int done        = taskRepository.countByUserAndStatus(user, TaskStatus.DONE);
        int high        = taskRepository.countByUserAndPriority(user, TaskPriority.HIGH);
        int medium      = taskRepository.countByUserAndPriority(user, TaskPriority.MEDIUM);
        int low         = taskRepository.countByUserAndPriority(user, TaskPriority.LOW);
        int overdue     = taskRepository.countOverdueTasks(user, java.time.LocalDate.now());
        int tasksThisWeek = taskRepository.countTasksThisWeek(user, LocalDateTime.now().minusDays(7));

        double completionRate = 0;
        if (totalTasks > 0) {
            completionRate = Math.round(((double) done / totalTasks) * 1000.0) / 10.0;
        }

        log.info("Summary generated: total={}, done={}, overdue={}", totalTasks, done, overdue);

        return new TaskSummaryDTO(totalTasks, todo, inProgress, done,
                high, medium, low, completionRate, overdue, tasksThisWeek);
    }

    // ── Helpers for human-readable log messages ──

    private String formatStatus(TaskStatus status) {
        return switch (status) {
            case TODO        -> "To-Do";
            case IN_PROGRESS -> "In Progress";
            case DONE        -> "Done";
        };
    }

    private String formatPriority(TaskPriority priority) {
        return switch (priority) {
            case HIGH   -> "High";
            case MEDIUM -> "Medium";
            case LOW    -> "Low";
        };
    }
}