package com.infy.tmwa.controller;

import com.infy.tmwa.dto.TaskDTO;
import com.infy.tmwa.dto.TaskSummaryDTO;
import com.infy.tmwa.entity.Task;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Task APIs", description = "Manage user tasks")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<Task>> getTasks(@AuthenticationPrincipal User user) {
        log.info("Fetching all tasks for user: {}", user.getEmail());
        List<Task> tasks = taskService.getTasks(user);
        log.info("Returned {} task(s) for user: {}", tasks.size(), user.getEmail());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/summary")
    public TaskSummaryDTO getSummary(@AuthenticationPrincipal User user) {
        return taskService.getSummary(user);
    }

    // ── NEW: GET tasks for a specific team ──
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Task>> getTasksByTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal User user) {
        log.info("Fetching tasks for team id: {} by user: {}", teamId, user.getEmail());
        return ResponseEntity.ok(taskService.getTasksByTeam(teamId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.getTaskById(id, user));
    }

    @Operation(summary = "Create new task")
    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestBody TaskDTO dto,
            @AuthenticationPrincipal User user) {
        log.info("Creating task for user: {} — title: '{}'", user.getEmail(), dto.getTitle());
        Task createdTask = taskService.createTask(dto, user);
        log.info("Task created — id: {}", createdTask.getId());
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @RequestBody TaskDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.updateTask(id, dto, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        taskService.deleteTask(id, user);
        return ResponseEntity.noContent().build();
    }
}