package com.codenames.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Model representing a player in the Codenames game.
 * Players can be on either team (BLUE or RED) or be spectators.
 * Each player has a role (SPYMASTER, OPERATIVE, or SPECTATOR).
 * Equality and hashCode are based solely on the player ID.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Player {
    /**
     * Unique identifier for the player.
     * Used for equality comparisons.
     */
    @EqualsAndHashCode.Include
    private String id;

    /**
     * Player's display name (max 20 characters).
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
