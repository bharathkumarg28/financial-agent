package com.financialagent.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Anthropic Claude API.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "anthropic")
public class AnthropicConfig {

    @NotBlank(message = "ANTHROPIC_API_KEY is required")
    private String apiKey;

    private String model = "claude-sonnet-4-20250514";

    private int maxTokens = 1500;

    // Getter methods as workaround for Lombok annotation processing issue
    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public boolean isValid() {
        return apiKey != null && !apiKey.isBlank();
    }
}
