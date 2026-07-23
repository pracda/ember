package com.ember.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Update the assistant gateway config. {@code apiKey} null = leave unchanged, blank = clear.
 */
public record AssistantConfigRequest(
        @Size(max = 400) String apiKey,
        @Size(max = 400) String baseUrl,
        @Size(max = 120) String model
) { }
