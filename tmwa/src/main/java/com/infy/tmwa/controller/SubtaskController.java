package com.infy.tmwa.controller;

import com.infy.tmwa.dto.SubtaskDTO;
import com.infy.tmwa.dto.SubtaskSummaryDTO;
import com.infy.tmwa.entity.Subtask;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.service.SubtaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class SubtaskController {

    private final SubtaskService subtaskService;

    // ── GET /api/tasks/{taskId}/subtasks ──
    @GetMapping("/api/tasks/{taskId}/subtasks")
    public ResponseEntity<List<Subtask>> getSubtasks(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {

        log.info("Get subtasks: task={}, user={}", taskId, user.getEmail());
        return ResponseEntity.ok(subtaskService.getSubtasks(taskId));
    }

    // ── POST /api/tasks/{taskId}/subtasks ──
    @PostMapping("/api/tasks/{taskId}/subtasks")
    public ResponseEntity<Subtask> create(
            @PathVariable Long taskId,
            @RequestBody SubtaskDTO dto,
            @AuthenticationPrincipal User user) {

        log.info("Create subtask: task={}, title='{}', user={}", taskId, dto.getTitle(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subtaskService.createSubtask(taskId, dto, user));
    }

    // ── PATCH /api/subtasks/{id}/toggle ──
    // Flips is_complete true↔false
    @PatchMapping("/api/subtasks/{id}/toggle")
    public ResponseEntity<Subtask> toggle(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("Toggle subtask: id={}, user={}", id, user.getEmail());
        return ResponseEntity.ok(subtaskService.toggle(id));
    }

    // ── PUT /api/subtasks/{id} ──
    @PutMapping("/api/subtasks/{id}")
    public ResponseEntity<Subtask> update(
            @PathVariable Long id,
            @RequestBody SubtaskDTO dto,
            @AuthenticationPrincipal User user) {

        log.info("Update subtask: id={}, user={}", id, user.getEmail());
        return ResponseEntity.ok(subtaskService.update(id, dto, user));
    }

    // ── DELETE /api/subtasks/{id} ──
    @DeleteMapping("/api/subtasks/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("Delete subtask: id={}, user={}", id, user.getEmail());
        subtaskService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/tasks/{taskId}/subtasks/summary ──
    // Lightweight — returns { total, completed } for progress bar on dashboard cards
    @GetMapping("/api/tasks/{taskId}/subtasks/summary")
    public ResponseEntity<SubtaskSummaryDTO> summary(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {

        log.info("Subtask summary: task={}, user={}", taskId, user.getEmail());
        return ResponseEntity.ok(subtaskService.getSummary(taskId));
    }
}