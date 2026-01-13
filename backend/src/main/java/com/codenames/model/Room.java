package com.codenames.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Model representing a game room in Codenames.
 * A room contains players, game settings, and can transition from lobby to active game.
 * Uses thread-safe collections for concurrent access.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    /**
     * Unique room identifier (format: XXXXX-XXXXX).
     */
    private String roomId;

    /**
     * ID of the player who created the room (admin).
     */
    private String adminId;

    /**
     * Timestamp when the room was created.
     */
    private LocalDateTime createdAt;

    /**
     * Game configuration settings.
     * Defaults to new GameSettings instance with default values.
     */
    @Builder.Default
    private GameSettings settings = new GameSettings();

    /**
     * List of players in the room.
     * Uses CopyOnWriteArrayList for thread-safe concurrent access.
     * Defaults to empty list.
     */
    @Builder.Default
    private List<Player> players = new CopyOnWriteArrayList<>();

    /**
     * Current game state (null when in lobby, before game starts).
     */
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
