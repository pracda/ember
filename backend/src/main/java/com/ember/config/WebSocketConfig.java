package com.ember.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time transport for the stations. Each of the POS, kitchen display and
 * pickup board opens a STOMP connection to {@code /ws} and subscribes to
 * {@code /topic/orders}; the server pushes an event on every order change.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final EmberProperties props;

    public WebSocketConfig(EmberProperties props) {
        this.props = props;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(props.getAllowedOrigins().toArray(String[]::new))
                .withSockJS()
                // We authenticate with a JWT, not a session cookie. Telling SockJS no
                // cookie is needed keeps its requests non-credentialed, so they pass the
                // stateless CORS rules the security filter now applies to /ws too.
                .setSessionCookieNeeded(false);
    }
}
