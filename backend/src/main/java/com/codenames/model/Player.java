package com.codenames.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * JPA Embeddable - stored as part of Room entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Embeddable
public class Player {
    /**
     * Unique identifier for the player.
     * Used for equality comparisons.
     */
    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private String id;

    /**
     * Player's display name (max 20 characters).
     */
    @Column(nullable = false, length = 20)
    private String username;

    /**
     * Team the player belongs to (null for spectators).
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Team team;

    /**
     * Player's role in the game.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * Whether the player is currently connected.
     */
    @Column(nullable = false)
    private boolean connected;

    /**
     * Whether the player is the room administrator.
     */
    @Column(nullable = false)
    private boolean admin;
}
