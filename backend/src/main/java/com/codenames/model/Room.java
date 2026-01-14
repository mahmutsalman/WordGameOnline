package com.codenames.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Model representing a game room in Codenames.
 * A room contains players, game settings, and can transition from lobby to active game.
 * JPA Entity - persisted to H2 database for durability across restarts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms")
public class Room {
    /**
     * Unique room identifier (format: XXXXX-XXXXX).
     */
    @Id
    @Column(nullable = false, length = 11)
    private String roomId;

    /**
     * ID of the player who created the room (admin).
     */
    @Column(nullable = false)
    private String adminId;

    /**
     * Timestamp when the room was created.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Game configuration settings.
     * Defaults to new GameSettings instance with default values.
     */
    @Embedded
    @Builder.Default
    private GameSettings settings = new GameSettings();

    /**
     * List of players in the room.
     * Stored as an element collection in a separate table.
     * Eagerly fetched to avoid lazy loading issues in WebSocket context.
     * Defaults to empty list.
     */
    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "room_players", joinColumns = @JoinColumn(name = "room_id"))
    @Builder.Default
    private List<Player> players = new ArrayList<>();

    /**
     * Current game state (null when in lobby, before game starts).
     * Not persisted - will be typed as GameState in later steps.
     */
    @Transient
    private Object gameState;  // Will be typed as GameState in later steps

    /**
     * Find a player by their ID.
     *
     * @param playerId the player ID to search for
     * @return Optional containing the player if found, empty otherwise
     */
    public Optional<Player> getPlayer(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst();
    }

    /**
     * Get all players on a specific team.
     *
     * @param team the team to filter by
     * @return list of players on the specified team
     */
    public List<Player> getPlayersByTeam(Team team) {
        return players.stream()
                .filter(p -> p.getTeam() == team)
                .collect(Collectors.toList());
    }

    /**
     * Check if the game can be started.
     * Requires:
     * - At least one BLUE SPYMASTER
     * - At least one RED SPYMASTER
     * - At least one BLUE OPERATIVE
     * - At least one RED OPERATIVE
     *
     * @return true if all required roles are filled, false otherwise
     */
    public boolean canStart() {
        boolean hasBlueSpymaster = players.stream()
                .anyMatch(p -> p.getTeam() == Team.BLUE && p.getRole() == Role.SPYMASTER);

        boolean hasRedSpymaster = players.stream()
                .anyMatch(p -> p.getTeam() == Team.RED && p.getRole() == Role.SPYMASTER);

        boolean hasBlueOperative = players.stream()
                .anyMatch(p -> p.getTeam() == Team.BLUE && p.getRole() == Role.OPERATIVE);

        boolean hasRedOperative = players.stream()
                .anyMatch(p -> p.getTeam() == Team.RED && p.getRole() == Role.OPERATIVE);

        return hasBlueSpymaster && hasRedSpymaster && hasBlueOperative && hasRedOperative;
    }
}
