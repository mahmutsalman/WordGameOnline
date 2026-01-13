package com.codenames.dto.request;

import com.codenames.model.Role;
import com.codenames.model.Team;
import lombok.Data;

@Data
public class ChangeTeamRequest {
    private Team team;  // null for spectator
    private Role role;
}
