package com.codenames.dto.events;

import com.codenames.model.Role;
import com.codenames.model.Team;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerUpdatedEvent {
    private final String type = "PLAYER_UPDATED";
    private String playerId;
    private Team team;
    private Role role;
}
