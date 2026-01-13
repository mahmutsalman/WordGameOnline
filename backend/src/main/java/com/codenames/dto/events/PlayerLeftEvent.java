package com.codenames.dto.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerLeftEvent {
    private final String type = "PLAYER_LEFT";
    private String playerId;
}
