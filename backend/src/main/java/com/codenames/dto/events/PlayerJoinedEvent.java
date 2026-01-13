package com.codenames.dto.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerJoinedEvent {
    private final String type = "PLAYER_JOINED";
    private String playerId;
    private String username;
}
