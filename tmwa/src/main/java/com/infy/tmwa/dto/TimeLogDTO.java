package com.infy.tmwa.dto;

import lombok.*;
import java.time.LocalDate;

// ── Request body for POST /api/tasks/{taskId}/time-logs (manual entry) ──
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TimeLogDTO {
    private int       durationMinutes; // e.g. 90 = 1h 30m
    private LocalDate logDate;         // date work was performed
    private String    note;            // optional
}