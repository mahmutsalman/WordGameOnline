package com.codenames.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for room creation requests.
 * Contains validated user input for creating a new game room.
 */
@Data
public class CreateRoomRequest {
    /**
     * Username of the player creating the room.
     * Required, cannot be blank, max 20 characters.
     */
    @NotBlank(message = "Username is required")
    @Size(max = 20, message = "Username too long")
    private String username;
}
