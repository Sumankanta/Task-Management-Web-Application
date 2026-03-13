package com.infy.tmwa.service;

import com.infy.tmwa.dto.SubtaskDTO;
import com.infy.tmwa.dto.SubtaskSummaryDTO;
import com.infy.tmwa.entity.*;
import com.infy.tmwa.repository.SubtaskRepository;
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
public class SubtaskService {

    private final SubtaskRepository subtaskRepository;
    private final TaskRepository    taskRepository;
    private final UserRepository    userRepository;

    // ── GET all subtasks for a task ──
    public List<Subtask> getSubtasks(Long taskId) {
        Task task = findTask(taskId);
        return subtaskRepository.findByTaskOrderByCreatedAtAsc(task);
    }

    // ── CREATE subtask ──
    public Subtask createSubtask(Long taskId, SubtaskDTO dto, User creator) {
        Task task = findTask(taskId);

        Subtask subtask = Subtask.builder()
                .task(task)
                .title(dto.getTitle())
                .isComplete(false)
                .createdBy(creator)
                .build();

        // Optional assignee
        if (dto.getAssignedTo() != null) {
            User assignee = userRepository.findById(dto.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            subtask.setAssignedTo(assignee);
        }

        Subtask saved = subtaskRepository.save(subtask);
        log.info("Subtask '{}' created for task {} by {}", saved.getTitle(), taskId, creator.getEmail());
        return saved;
    }

    // ── TOGGLE is_complete ──
    // PATCH /api/subtasks/{id}/toggle
    // Flips boolean, sets/clears completedAt
    public Subtask toggle(Long subtaskId) {
        Subtask subtask = findSubtask(subtaskId);

        boolean nowComplete = !subtask.isComplete();
        subtask.setComplete(nowComplete);
        subtask.setCompletedAt(nowComplete ? LocalDateTime.now() : null);

        Subtask updated = subtaskRepository.save(subtask);
        log.info("Subtask {} toggled to isComplete={}", subtaskId, nowComplete);
        return updated;
    }

    // ── UPDATE title/assignee ──
    public Subtask update(Long subtaskId, SubtaskDTO dto, User user) {
        Subtask subtask = findSubtask(subtaskId);

        checkEditPermission(subtask, user);

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            subtask.setTitle(dto.getTitle());
        }

        if (dto.getAssignedTo() != null) {
            User assignee = userRepository.findById(dto.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            subtask.setAssignedTo(assignee);
        } else {
            subtask.setAssignedTo(null);
        }

        return subtaskRepository.save(subtask);
    }

    // ── DELETE ──
    public void delete(Long subtaskId, User user) {
        Subtask subtask = findSubtask(subtaskId);
        checkEditPermission(subtask, user);
        subtaskRepository.delete(subtask);
        log.info("Subtask {} deleted by {}", subtaskId, user.getEmail());
    }

    // ── SUMMARY — lightweight for dashboard cards ──
    // GET /api/tasks/{taskId}/subtasks/summary
    public SubtaskSummaryDTO getSummary(Long taskId) {
        Task task      = findTask(taskId);
        int total      = subtaskRepository.countByTask(task);
        int completed  = subtaskRepository.countByTaskAndIsComplete(task, true);
        return new SubtaskSummaryDTO(total, completed);
    }

    // ── Helpers ──

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
    }

    private Subtask findSubtask(Long id) {
        return subtaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subtask not found: " + id));
    }

    // Admin/Manager OR the subtask creator can edit/delete
    private void checkEditPermission(Subtask subtask, User user) {
        boolean isCreator    = subtask.getCreatedBy().getId().equals(user.getId());
        boolean isPrivileged = user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.MANAGER;
        if (!isCreator && !isPrivileged) {
            throw new RuntimeException("Access denied: cannot modify this subtask");
        }
    }
}