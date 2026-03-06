package com.infy.tmwa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskSummaryDTO {

    private int totalTasks;
    private int todo;
    private int inProgress;
    private int done;
    private int high;
    private int medium;
    private int low;
    private double completionRate;
    private int overdueCount;
    private int tasksThisWeek;
}