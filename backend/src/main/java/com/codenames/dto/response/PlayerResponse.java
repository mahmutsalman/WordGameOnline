package com.codenames.dto.response;

import com.codenames.model.Role;
import com.codenames.model.Team;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for player data in API responses.
 * Represents player information sent to clients.
 */
@Data
@Builder
public class PlayerResponse {
    /**
     * Unique player identifier.
     */
    private String id;

    /**
     * Player's display name.
     */
    private String username;

    /**
     * Team the player belongs to (null for spectators).
     */
    private Team team;

    /**
     * Player's role in the game.
     */
    private Role role;

    /**
     * Whether the player is currently connected.
     */
    private boolean connected;

    /**
     * Whether the player is the room administrator.
     */
    private boolean admin;
}
