package com.codenames;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Codenames multiplayer game.
 * This application provides a real-time WebSocket-based implementation
 * of the Codenames board game.
 */
@SpringBootApplication
public class CodenamesApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CodenamesApplication.class, args);
    }
}
