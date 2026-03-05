package com.infy.tmwa.controller;

import com.infy.tmwa.dto.TaskDTO;
import com.infy.tmwa.entity.Task;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

//    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;

    // ✅ GET ALL TASKS
    @GetMapping
    public ResponseEntity<List<Task>> getTasks(
            @AuthenticationPrincipal User user) {

        log.info("Fetching all tasks for user: {}", user.getEmail());
        try {
            List<Task> tasks = taskService.getTasks(user);
            log.info("Returned {} task(s) for user: {}", tasks.size(), user.getEmail());
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Failed to fetch tasks for user: {} — {}", user.getEmail(), e.getMessage());
            throw e;
        }
    }

    // ✅ GET TASK BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("Fetching task id: {} for user: {}", id, user.getEmail());
        try {
            Task task = taskService.getTaskById(id, user);
            log.info("Task id: {} fetched successfully for user: {}", id, user.getEmail());
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            log.error("Failed to fetch task id: {} for user: {} — {}", id, user.getEmail(), e.getMessage());
            throw e;
        }
    }

    // ✅ CREATE TASK
    @Operation(summary = "Create new task")
    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestBody TaskDTO dto,
            @AuthenticationPrincipal User user) {

        log.info("Creating task for user: {} — title: '{}'", user.getEmail(), dto.getTitle());
        try {
            Task createdTask = taskService.createTask(dto, user);
            log.info("Task created successfully — id: {}, title: '{}', user: {}",
                    createdTask.getId(), createdTask.getTitle(), user.getEmail());
            return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Failed to create task for user: {} — {}", user.getEmail(), e.getMessage());
            throw e;
        }
    }

    // ✅ UPDATE TASK
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @RequestBody TaskDTO dto,
            @AuthenticationPrincipal User user) {

        log.info("Updating task id: {} for user: {} — new title: '{}'",
                id, user.getEmail(), dto.getTitle());
        try {
            Task updatedTask = taskService.updateTask(id, dto, user);
            log.info("Task id: {} updated successfully for user: {}", id, user.getEmail());
            return ResponseEntity.ok(updatedTask);
        } catch (Exception e) {
            log.error("Failed to update task id: {} for user: {} — {}", id, user.getEmail(), e.getMessage());
            throw e;
        }
    }

    // ✅ DELETE TASK
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("Deleting task id: {} for user: {}", id, user.getEmail());
        try {
            taskService.deleteTask(id, user);
            log.info("Task id: {} deleted successfully for user: {}", id, user.getEmail());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete task id: {} for user: {} — {}", id, user.getEmail(), e.getMessage());
            throw e;
        }
    }
}