package com.codenames.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for room join requests.
 * Contains validated user input for joining an existing game room.
 */
@Data
public class JoinRoomRequest {
    /**
     * Username of the player joining the room.
     * Required, cannot be blank, max 20 characters.
     */
    @NotBlank(message = "Username is required")
    @Size(max = 20, message = "Username too long")
    private String username;
}
