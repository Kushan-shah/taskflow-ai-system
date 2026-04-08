package com.taskmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.enums.AiStatus;
import com.taskmanager.entity.enums.TaskPriority;
import com.taskmanager.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private final GeminiAiService geminiAiService;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public AiAnalysisService(GeminiAiService geminiAiService,
                             TaskRepository taskRepository,
                             ObjectMapper objectMapper) {
        this.geminiAiService = geminiAiService;
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Asynchronously analyzes a task using Gemini AI.
     * Runs in background thread — does NOT block the API response.
     * On any failure, marks aiStatus = FAILED and stores error message.
     */
    @Async("aiTaskExecutor")
    @Transactional
    public void analyzeTaskAsync(Long taskId) {
        log.info("Starting AI analysis for taskId={}", taskId);

        Optional<Task> optTask = taskRepository.findById(taskId);
        if (optTask.isEmpty()) {
            log.warn("AI analysis skipped — task not found: {}", taskId);
            return;
        }

        Task task = optTask.get();

        // Idempotency check — skip if already successfully analyzed
        if (task.getAiStatus() == AiStatus.DONE) {
            log.info("AI analysis skipped — task {} is already DONE", taskId);
            return;
        }

        task.setAiStatus(AiStatus.PENDING);
        taskRepository.save(task);

        try {
            String aiJson = geminiAiService.analyzeTask(task.getTitle(), task.getDescription());
            parseAndSaveAiResult(task, aiJson);
            log.info("AI analysis DONE for taskId={}", taskId);

        } catch (Exception e) {
            log.error("AI analysis FAILED for taskId={} — {}", taskId, e.getMessage());
            task.setAiStatus(AiStatus.FAILED);
            task.setAiErrorMessage(e.getMessage());
            taskRepository.save(task);
        }
    }

    private void parseAndSaveAiResult(Task task, String aiJson) {
        try {
            JsonNode node = objectMapper.readTree(aiJson);

            String summary = node.path("summary").asText(null);
            String priorityStr = node.path("priority").asText("MEDIUM").toUpperCase();
            List<String> tags = new ArrayList<>();
            node.path("tags").forEach(t -> tags.add(t.asText()));

            // Safely parse priority — default to MEDIUM on invalid value
            TaskPriority aiPriority;
            try {
                aiPriority = TaskPriority.valueOf(priorityStr);
            } catch (IllegalArgumentException ex) {
                aiPriority = TaskPriority.MEDIUM;
            }

            task.setAiSummary(summary);
            task.setAiPriority(aiPriority);
            task.setAiTags(String.join(",", tags));
            task.setAiStatus(AiStatus.DONE);
            task.setAiErrorMessage(null);
            taskRepository.save(task);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI JSON response: " + aiJson, e);
        }
    }
}
