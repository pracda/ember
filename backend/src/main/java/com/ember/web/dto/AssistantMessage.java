package com.ember.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** One chat turn. Role is "user" or "assistant". */
public record AssistantMessage(
        @NotNull @Pattern(regexp = "user|assistant") String role,
        @NotBlank String content
) { }
