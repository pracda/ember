package com.ember.service;

import com.ember.domain.AppSetting;
import com.ember.repository.AppSettingRepository;
import com.ember.web.dto.AssistantConfigResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores and reads the AI-assistant's LLM-gateway configuration (base URL, API key,
 * model) in the {@code app_setting} table so a manager can set it from the admin panel.
 * The raw key is never returned to clients — only a masked preview.
 */
@Service
public class AssistantConfigService {

    static final String KEY_API_KEY = "assistant.apiKey";
    static final String KEY_BASE_URL = "assistant.baseUrl";
    static final String KEY_MODEL = "assistant.model";

    // The assistant routes through the Secure LLM Gateway; there is no sensible default URL
    // (blank => not configured). The live gateway currently allows only this model.
    static final String DEFAULT_BASE_URL = "";
    static final String DEFAULT_MODEL = "claude-haiku-4-5-20251001";

    private final AppSettingRepository settings;

    public AssistantConfigService(AppSettingRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public boolean isConfigured() {
        return !apiKey().isBlank() && !baseUrl().isBlank();
    }

    @Transactional(readOnly = true)
    public String apiKey() {
        return get(KEY_API_KEY, "");
    }

    @Transactional(readOnly = true)
    public String baseUrl() {
        return get(KEY_BASE_URL, DEFAULT_BASE_URL);
    }

    @Transactional(readOnly = true)
    public String model() {
        String v = get(KEY_MODEL, DEFAULT_MODEL);
        return v.isBlank() ? DEFAULT_MODEL : v;
    }

    /** Manager-facing view: whether a key is set + a masked preview, base URL and model. */
    @Transactional(readOnly = true)
    public AssistantConfigResponse view() {
        String key = apiKey();
        return new AssistantConfigResponse(!key.isBlank(), mask(key), baseUrl(), model());
    }

    /**
     * Update the config. A null {@code apiKey} leaves the stored key unchanged; a blank
     * one clears it. Null base URL / model reset to the defaults.
     */
    @Transactional
    public AssistantConfigResponse update(String apiKey, String baseUrl, String model) {
        if (apiKey != null) {
            put(KEY_API_KEY, apiKey.trim());
        }
        put(KEY_BASE_URL, baseUrl == null ? "" : baseUrl.trim());
        put(KEY_MODEL, model == null || model.isBlank() ? DEFAULT_MODEL : model.trim());
        return view();
    }

    private String get(String key, String fallback) {
        return settings.findById(key).map(AppSetting::getValue)
                .filter(v -> v != null)
                .orElse(fallback);
    }

    private void put(String key, String value) {
        AppSetting setting = settings.findById(key).orElseGet(() -> new AppSetting(key, value));
        setting.setValue(value);
        settings.save(setting);
    }

    private static String mask(String key) {
        if (key.isBlank()) return null;
        return key.length() <= 4 ? "••••" : "••••••" + key.substring(key.length() - 4);
    }
}
