package com.infy.tmwa.dto;

import lombok.*;

// ── Lightweight response for GET /api/tasks/{taskId}/subtasks/summary ──
// Used by dashboard task cards to show "3 / 5 subtasks done"
// without loading full subtask objects
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SubtaskSummaryDTO {
    private int total;
    private int completed;
}