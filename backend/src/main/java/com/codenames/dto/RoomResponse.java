package com.codenames.dto;

import com.codenames.model.GameSettings;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for room data in API responses.
 * Represents complete room state sent to clients.
 */
@Data
@Builder
public class RoomResponse {
    /**
     * Unique room identifier.
     */
    private String roomId;

    /**
     * List of players in the room.
     */
    private List<PlayerResponse> players;

    /**
     * Game configuration settings.
     */
    private GameSettings settings;

    /**
     * Whether the game can be started (all required roles filled).
     */
    private boolean canStart;

    /**
     * ID of the room administrator.
     */
    private String adminId;
}
