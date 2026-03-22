package com.taskmanager.dto;

import com.taskmanager.entity.enums.TaskPriority;
import com.taskmanager.entity.enums.TaskStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private String fileUrl;
    private LocalDateTime createdAt;
}
