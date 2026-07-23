package com.ember.web.dto;

/** Manager-facing view of the assistant gateway config. The raw key is never included. */
public record AssistantConfigResponse(
        boolean configured,
        String keyPreview,
        String baseUrl,
        String model
) { }
