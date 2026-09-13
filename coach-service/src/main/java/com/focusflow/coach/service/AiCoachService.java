package com.focusflow.coach.service;

import com.focusflow.coach.client.ClaudeApiClient;
import com.focusflow.coach.client.SessionServiceClient;
import com.focusflow.coach.dto.InsightsResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiCoachService {

    private static final String SYSTEM_PROMPT = """
            You are a personal productivity coach embedded in FocusFlow AI.
            Analyse the session history provided and give:
            1. A warm, encouraging 1-2 sentence opening
            2. 3-5 specific, data-driven insights about focus habits
            3. 2-3 actionable recommendations tailored to the patterns you see
            4. A motivating closing line
            Be specific - reference actual numbers from the data. 200-300 words total.
            """;

    private final ClaudeApiClient claudeClient;
    private final SessionServiceClient sessionClient;
    private InsightsResponse cachedInsights;

    public AiCoachService(ClaudeApiClient claudeClient, SessionServiceClient sessionClient) {
        this.claudeClient = claudeClient;
        this.sessionClient = sessionClient;
    }

    public InsightsResponse generateInsights() {
        String sessionSummary = sessionClient.getSummary();
        String userMessage = "Session history:\n" + sessionSummary + "\nGive me coaching insights.";

        String insights = claudeClient.complete(SYSTEM_PROMPT, userMessage);
        cachedInsights = new InsightsResponse(insights, LocalDateTime.now(), countSessions(sessionSummary));
        return cachedInsights;
    }

    public InsightsResponse getLatestInsights() {
        return cachedInsights;
    }

    private long countSessions(String summary) {
        try {
            for (String line : summary.split("\n")) {
                if (line.startsWith("Total completed sessions:")) {
                    return Long.parseLong(line.split(":")[1].trim());
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
