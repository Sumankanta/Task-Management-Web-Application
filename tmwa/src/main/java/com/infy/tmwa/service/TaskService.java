package com.infy.tmwa.service;

import com.infy.tmwa.dto.TaskDTO;
import com.infy.tmwa.entity.Task;
import com.infy.tmwa.entity.TaskPriority;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.repository.TaskRepository;
import com.infy.tmwa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

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
                .status(dto.getStatus())
                .user(user)
                .build();

        // 🔹 HANDLE PRIORITY
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        } else {
            task.setPriority(TaskPriority.MEDIUM);
        }

        log.info("Task priority set to {}", task.getPriority());

        // 🔹 HANDLE ASSIGNEE
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

        return saved;
    }

    public Task updateTask(Long id, TaskDTO dto, User user) {

        log.info("Updating task id {} for user {}", id, user.getEmail());

        Task task = getTaskById(id, user);

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDueDate(dto.getDueDate());
        task.setStatus(dto.getStatus());

        // 🔹 UPDATE PRIORITY
        if (dto.getPriority() != null) {

            task.setPriority(dto.getPriority());

            log.info("Task priority updated to {}", dto.getPriority());
        }

        // 🔹 UPDATE ASSIGNEE
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

        return updated;
    }

    public void deleteTask(Long id, User user) {

        log.info("Deleting task id {} for user {}", id, user.getEmail());

        Task task = getTaskById(id, user);

        taskRepository.delete(task);

        log.info("Task id {} deleted successfully", id);
    }
}