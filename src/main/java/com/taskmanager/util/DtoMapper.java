package com.taskmanager.util;

import com.taskmanager.dto.AuthResponse;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.User;

public class DtoMapper {

    private DtoMapper() {
        // Utility class — prevent instantiation
    }

    public static TaskResponse toTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .fileUrl(task.getFileUrl())
                .createdAt(task.getCreatedAt())
                .build();
    }

    public static AuthResponse toAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
