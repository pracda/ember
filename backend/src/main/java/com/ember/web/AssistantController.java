package com.ember.web;

import com.ember.service.AssistantConfigService;
import com.ember.service.AssistantService;
import com.ember.web.dto.AssistantChatRequest;
import com.ember.web.dto.AssistantChatResponse;
import com.ember.web.dto.AssistantConfigRequest;
import com.ember.web.dto.AssistantConfigResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The admin AI assistant. Manager-only, enforced by {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistant;
    private final AssistantConfigService config;

    public AssistantController(AssistantService assistant, AssistantConfigService config) {
        this.assistant = assistant;
        this.config = config;
    }

    /** Masked gateway config — whether a key is set, its last 4, base URL and model. */
    @GetMapping("/config")
    public AssistantConfigResponse getConfig() {
        return config.view();
    }

    /** Store the gateway API key / base URL / model. Null apiKey leaves the key unchanged. */
    @PutMapping("/config")
    public AssistantConfigResponse setConfig(@Valid @RequestBody AssistantConfigRequest request) {
        return config.update(request.apiKey(), request.baseUrl(), request.model());
    }

    @PostMapping("/chat")
    public AssistantChatResponse chat(@Valid @RequestBody AssistantChatRequest request) {
        return new AssistantChatResponse(assistant.chat(request.messages()));
    }
}
