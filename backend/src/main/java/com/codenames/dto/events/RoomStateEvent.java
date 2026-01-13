package com.codenames.dto.events;

import com.codenames.dto.response.PlayerResponse;
import com.codenames.model.GameSettings;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoomStateEvent {
    private final String type = "ROOM_STATE";
    private List<PlayerResponse> players;
    private GameSettings settings;
    private boolean canStart;
}
