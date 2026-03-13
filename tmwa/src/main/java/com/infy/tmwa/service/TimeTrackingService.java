package com.infy.tmwa.service;

import com.infy.tmwa.dto.TimeLogDTO;
import com.infy.tmwa.entity.*;
import com.infy.tmwa.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeTrackingService {

    private final TimeLogRepository    timeLogRepository;
    private final ActiveTimerRepository activeTimerRepository;
    private final TaskRepository       taskRepository;

    // ── START TIMER ──
    // POST /api/tasks/{taskId}/timer/start
    // Returns 409 if timer already running for this task+user
    public ActiveTimer startTimer(Long taskId, User user) {
        Task task = findTask(taskId);

        // 409 check — only one active timer per task per user
        if (activeTimerRepository.findByTaskAndUser(task, user).isPresent()) {
            throw new RuntimeException("TIMER_ALREADY_RUNNING");
        }

        ActiveTimer timer = ActiveTimer.builder()
                .task(task)
                .user(user)
                .startTime(LocalDateTime.now())
                .build();

        ActiveTimer saved = activeTimerRepository.save(timer);
        log.info("Timer started: task={}, user={}, startTime={}", taskId, user.getEmail(), saved.getStartTime());
        return saved;
    }

    // ── STOP TIMER ──
    // POST /api/tasks/{taskId}/timer/stop
    // Computes duration from start_time → now, creates time log, deletes active timer row
    @Transactional
    public TaskTimeLog stopTimer(Long taskId, User user) {
        Task task = findTask(taskId);

        ActiveTimer timer = activeTimerRepository.findByTaskAndUser(task, user)
                .orElseThrow(() -> new RuntimeException("No active timer found for this task"));

        // Compute duration in minutes — minimum 1 minute
        long minutes = ChronoUnit.MINUTES.between(timer.getStartTime(), LocalDateTime.now());
        if (minutes < 1) minutes = 1;

        log.info("Timer stopped: task={}, user={}, duration={}min", taskId, user.getEmail(), minutes);

        // Create time log entry (is_manual = false — timer generated)
        TaskTimeLog log_entry = TaskTimeLog.builder()
                .task(task)
                .loggedBy(user)
                .durationMinutes((int) minutes)
                .logDate(LocalDate.now())
                .isManual(false)
                .build();

        TaskTimeLog saved = timeLogRepository.save(log_entry);

        // Delete the active timer row
        activeTimerRepository.deleteByTaskAndUser(task, user);

        return saved;
    }

    // ── GET active timer for a task+user ──
    // Frontend calls this on page load to resume the counter display
    public Map<String, Object> getActiveTimer(Long taskId, User user) {
        Task task = findTask(taskId);
        return activeTimerRepository.findByTaskAndUser(task, user)
                .map(t -> Map.<String, Object>of(
                        "running",   true,
                        "startTime", t.getStartTime().toString()
                ))
                .orElse(Map.of("running", false));
    }

    // ── MANUAL LOG ENTRY ──
    // POST /api/tasks/{taskId}/time-logs
    public TaskTimeLog logManual(Long taskId, TimeLogDTO dto, User user) {
        Task task = findTask(taskId);

        TaskTimeLog entry = TaskTimeLog.builder()
                .task(task)
                .loggedBy(user)
                .durationMinutes(dto.getDurationMinutes())
                .logDate(dto.getLogDate() != null ? dto.getLogDate() : LocalDate.now())
                .note(dto.getNote())
                .isManual(true)
                .build();

        TaskTimeLog saved = timeLogRepository.save(entry);
        log.info("Manual time log: task={}, user={}, duration={}min", taskId, user.getEmail(), dto.getDurationMinutes());
        return saved;
    }

    // ── GET all time logs ──
    public List<TaskTimeLog> getLogs(Long taskId) {
        Task task = findTask(taskId);
        return timeLogRepository.findByTaskOrderByLogDateDesc(task);
    }

    // ── GET total minutes ──
    // Returns { totalMinutes: int }
    public Map<String, Integer> getTotal(Long taskId) {
        Task task = findTask(taskId);
        int total = timeLogRepository.sumDurationByTask(task);
        return Map.of("totalMinutes", total);
    }

    // ── DELETE manual log entry ──
    // Timer-generated entries (is_manual=false) cannot be deleted per SRS
    public void deleteLog(Long logId, User user) {
        TaskTimeLog entry = timeLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Time log not found: " + logId));

        if (!entry.isManual()) {
            throw new RuntimeException("Timer-generated entries cannot be deleted");
        }

        if (!entry.getLoggedBy().getId().equals(user.getId())
                && user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.MANAGER) {
            throw new RuntimeException("Access denied");
        }

        timeLogRepository.delete(entry);
        log.info("Time log {} deleted by {}", logId, user.getEmail());
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
    }
}