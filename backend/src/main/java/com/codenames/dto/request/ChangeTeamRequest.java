package com.codenames.dto.request;

import com.codenames.model.Role;
import com.codenames.model.Team;
import lombok.Data;

/**
 * Request DTO for changing a player's team and role.
 * The playerId field is optional and used for validation to prevent player impersonation.
 */
@Data
public class ChangeTeamRequest {
    /**
     * Optional player ID for validation.
     * If provided, must match the playerId stored in the WebSocket session.
     */
    private String playerId;

    /**
     * The team to join (null for spectator).
     */
    private Team team;

    /**
     * The role to take in the team.
     */
    private Role role;
}
