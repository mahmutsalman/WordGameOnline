package com.codenames.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time game communication.
 * Implements STOMP protocol over SockJS for reliable WebSocket fallback.
 * Follows the Observer pattern for broadcasting game events to subscribed clients.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker for pub/sub messaging.
     * Sets up broker destinations and application prefixes.
     *
     * @param config message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for broadcasting
        // /topic - for room-wide broadcasts (all players)
        // /queue - for user-specific messages
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages FROM client TO server
        config.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints with SockJS fallback support.
     * Allows all origins for development (restrict in production).
     *
     * @param registry endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint with SockJS fallback
        // Accessible at ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Allow all origins for dev
                .withSockJS();
    }
}
