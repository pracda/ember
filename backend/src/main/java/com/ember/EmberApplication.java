package com.ember;

import com.ember.config.EmberProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(EmberProperties.class)
public class EmberApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmberApplication.class, args);
    }
}
