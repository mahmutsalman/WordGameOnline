package com.codenames.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for allowing frontend development server access.
 * Follows Single Responsibility Principle by separating CORS concerns
 * from other configuration.
 */
@Configuration
public class CorsConfig {

    /**
     * Configure CORS mappings for REST API endpoints.
     * Allows frontend dev server (port 5173) to access backend.
     *
     * @return WebMvcConfigurer with CORS settings
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns("*")  // Allow all origins for dev
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);  // Cache preflight response for 1 hour
            }
        };
    }
}
