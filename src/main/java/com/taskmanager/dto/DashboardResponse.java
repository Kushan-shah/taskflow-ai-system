package com.taskmanager.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private long totalTasks;
    private long todoCount;
    private long inProgressCount;
    private long doneCount;
    private long overdueCount;
}
