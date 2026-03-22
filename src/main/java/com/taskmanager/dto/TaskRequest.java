package com.taskmanager.dto;

import com.taskmanager.entity.enums.TaskPriority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private TaskPriority priority;

    @Future(message = "Due date must be in the future")
    private LocalDate dueDate;
}
