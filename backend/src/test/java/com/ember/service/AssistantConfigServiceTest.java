package com.ember.service;

import com.ember.repository.AppSettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AssistantConfigServiceTest {

    @Autowired
    private AppSettingRepository settings;

    private AssistantConfigService service() {
        return new AssistantConfigService(settings);
    }

    @Test
    void defaultsWhenUnset() {
        var view = service().view();
        assertThat(view.configured()).isFalse();
        assertThat(view.keyPreview()).isNull();
        assertThat(view.baseUrl()).isEqualTo("https://api.anthropic.com");
        assertThat(view.model()).isEqualTo("claude-opus-4-8");
        assertThat(service().isConfigured()).isFalse();
    }

    @Test
    void storesKeyMaskedAndKeepsItWhenApiKeyIsNull() {
        service().update("sk-secret-1234", "https://gw.example.com", "claude-opus-4-8");

        var view = service().view();
        assertThat(view.configured()).isTrue();
        assertThat(view.keyPreview()).endsWith("1234");
        assertThat(view.keyPreview()).doesNotContain("secret");
        assertThat(view.baseUrl()).isEqualTo("https://gw.example.com");

        // A null apiKey updates the other fields but leaves the stored key intact.
        service().update(null, "https://gw2.example.com", null);
        assertThat(service().apiKey()).isEqualTo("sk-secret-1234");
        assertThat(service().baseUrl()).isEqualTo("https://gw2.example.com");
        assertThat(service().model()).isEqualTo("claude-opus-4-8");
    }
}
