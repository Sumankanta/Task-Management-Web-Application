package com.infy.tmwa.controller;

import com.infy.tmwa.dto.TimeLogDTO;
import com.infy.tmwa.entity.ActiveTimer;
import com.infy.tmwa.entity.TaskTimeLog;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.service.TimeTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class TimeTrackingController {

    private final TimeTrackingService timeTrackingService;

    // ── POST /api/tasks/{taskId}/timer/start ──
    // Returns 409 if timer already running
    @PostMapping("/api/tasks/{taskId}/timer/start")
    public ResponseEntity<?> startTimer(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {

        log.info("Start timer: task={}, user={}", taskId, user.getEmail());
        try {
            ActiveTimer timer = timeTrackingService.startTimer(taskId, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(timer);
        } catch (RuntimeException e) {
            if ("TIMER_ALREADY_RUNNING".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "A timer is already running for this task"));
            }
            throw e;
        }
    }

    // ── POST /api/tasks/{taskId}/timer/stop ──
    // Computes duration, creates log entry, deletes active timer row
    @PostMapping("/api/tasks/{taskId}/timer/stop")
    public ResponseEntity<TaskTimeLog> stopTimer(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {

        log.info("Stop timer: task={}, user={}", taskId, user.getEmail());
        return ResponseEntity.ok(timeTrackingService.stopTimer(taskId, user));
    }

    // ── GET /api/tasks/{taskId}/timer/status ──
    // Frontend calls on page load — resumes live counter from DB start_time
    @GetMapping("/api/tasks/{taskId}/timer/status")
    public ResponseEntity<Map<String, Object>> timerStatus(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(timeTrackingService.getActiveTimer(taskId, user));
    }

    // ── POST /api/tasks/{taskId}/time-logs ──
    // Manual entry: durationMinutes, logDate, note
    @PostMapping("/api/tasks/{taskId}/time-logs")
    public ResponseEntity<TaskTimeLog> logManual(
            @PathVariable Long taskId,
            @RequestBody TimeLogDTO dto,
            @AuthenticationPrincipal User user) {

        log.info("Manual log: task={}, minutes={}, user={}", taskId, dto.getDurationMinutes(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timeTrackingService.logManual(taskId, dto, user));
    }

    // ── GET /api/tasks/{taskId}/time-logs ──
    // Returns all entries ordered by log_date DESC
    @GetMapping("/api/tasks/{taskId}/time-logs")
    public ResponseEntity<List<TaskTimeLog>> getLogs(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(timeTrackingService.getLogs(taskId));
    }

    // ── GET /api/tasks/{taskId}/time-logs/total ──
    // Returns { totalMinutes: int } — SUM of all entries
    @GetMapping("/api/tasks/{taskId}/time-logs/total")
    public ResponseEntity<Map<String, Integer>> getTotal(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(timeTrackingService.getTotal(taskId));
    }

    // ── DELETE /api/time-logs/{id} ──
    // Manual entries only — timer-generated entries return 403
    @DeleteMapping("/api/time-logs/{id}")
    public ResponseEntity<?> deleteLog(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("Delete time log: id={}, user={}", id, user.getEmail());
        try {
            timeTrackingService.deleteLog(id, user);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("cannot be deleted")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            }
            throw e;
        }
    }
}