package com.infy.tmwa.dto;

import com.infy.tmwa.entity.TaskPriority;
import com.infy.tmwa.entity.TaskStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskDTO {

    private Long id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private TaskStatus status;
    private TaskPriority priority;
    private Long assignedTo;
}
