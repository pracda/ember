package com.ember.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** The running conversation; the last message must be from the user. */
public record AssistantChatRequest(
        @NotEmpty @Valid List<AssistantMessage> messages
) { }
