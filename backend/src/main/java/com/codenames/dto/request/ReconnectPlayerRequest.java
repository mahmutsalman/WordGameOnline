package com.codenames.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for reconnecting an existing player to a WebSocket session.
 * Used when a player joined via REST API and needs to establish a WebSocket session.
 */
@Data
public class ReconnectPlayerRequest {
    @NotBlank(message = "Player ID is required")
    private String playerId;
}
