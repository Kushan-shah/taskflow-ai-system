package com.taskmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GeminiAiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKeyConfig;

    private List<String> apiKeys;
    private final AtomicInteger currentKeyIndex = new AtomicInteger(0);

    public GeminiAiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @PostConstruct
    public void init() {
        if (apiKeyConfig == null || apiKeyConfig.isBlank()) {
            apiKeys = List.of();
            log.warn("No GEMINI_API_KEY configured. AI features will be disabled.");
            return;
        }
        apiKeys = List.of(apiKeyConfig.split(","));
        log.info("Loaded {} Gemini API key(s) with auto-failover", apiKeys.size());
    }

    /**
     * Calls Gemini API with automatic key rotation on rate limit (429) or server error (5xx).
     * Tries all available keys before giving up.
     */
    public String analyzeTask(String title, String description) {
        if (apiKeys.isEmpty()) {
            throw new RuntimeException("GEMINI_API_KEY is not configured.");
        }

        String prompt = buildPrompt(title, description);
        String requestBody = buildRequestBody(prompt);

        // Try each key — rotate on rate limit
        for (int attempt = 0; attempt < apiKeys.size(); attempt++) {
            int keyIdx = (currentKeyIndex.get() + attempt) % apiKeys.size();
            String key = apiKeys.get(keyIdx);

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GEMINI_URL + key))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(Duration.ofSeconds(15))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 429 || response.statusCode() >= 500) {
                    log.warn("Gemini key #{} hit rate limit or error (HTTP {}). Switching to next key...",
                            keyIdx + 1, response.statusCode());
                    // Rotate to next key for future requests
                    currentKeyIndex.set((keyIdx + 1) % apiKeys.size());
                    continue; // try next key
                }

                if (response.statusCode() != 200) {
                    throw new RuntimeException("Gemini API error: HTTP " + response.statusCode() + " — " + response.body());
                }

                // Success — remember this key as the active one
                currentKeyIndex.set(keyIdx);
                return extractTextFromGeminiResponse(response.body());

            } catch (RuntimeException e) {
                throw e; // re-throw our own exceptions
            } catch (Exception e) {
                log.error("GeminiAiService error with key #{}: {}", keyIdx + 1, e.getMessage());
                if (attempt == apiKeys.size() - 1) {
                    throw new RuntimeException("AI processing failed after trying all keys: " + e.getMessage(), e);
                }
            }
        }

        throw new RuntimeException("AI processing failed: all API keys exhausted (rate limited).");
    }

    private String buildPrompt(String title, String description) {
        return String.format("""
            You are an AI assistant for a task management system.
            Analyze the following task and return structured insights.

            Title: "%s"
            Description: "%s"

            Return ONLY valid JSON. Do not include explanations, markdown, or backticks.
            If unsure about priority, choose MEDIUM.

            {
              "summary": "A concise 1-sentence summary of the task",
              "priority": "Must be exactly HIGH, MEDIUM, or LOW",
              "tags": ["tag1", "tag2"]
            }
            """, title, description != null ? description : "No description provided");
    }

    private String buildRequestBody(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "contents", new Object[]{
                            Map.of("parts", new Object[]{
                                    Map.of("text", prompt)
                            })
                    },
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "maxOutputTokens", 256
                    )
            );
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Gemini request body", e);
        }
    }

    private String extractTextFromGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String rawText = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            rawText = rawText.trim();
            if (rawText.startsWith("```")) {
                rawText = rawText.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            }
            return rawText;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + responseBody, e);
        }
    }
}
