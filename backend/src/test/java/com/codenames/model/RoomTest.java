package com.codenames.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Room model.
 * Tests room creation, player management, and game readiness logic.
 */
class RoomTest {

    /**
     * Test that Room can be created using Lombok Builder.
     */
    @Test
    void shouldCreateRoomWithBuilder() {
        // Arrange & Act
        Room room = Room.builder()
                .roomId("TEST-12345")
                .adminId("admin-1")
                .createdAt(LocalDateTime.now())
                .build();

        // Assert
        assertThat(room).isNotNull();
        assertThat(room.getRoomId()).isEqualTo("TEST-12345");
        assertThat(room.getAdminId()).isEqualTo("admin-1");
    }

    /**
     * Test that Room defaults to empty players list if not specified.
     */
    @Test
    void shouldDefaultToEmptyPlayersList() {
        // Arrange & Act
        Room room = Room.builder()
                .roomId("TEST-12345")
                .adminId("admin-1")
                .build();

        // Assert
        assertThat(room.getPlayers())
                .as("Players list should default to empty")
                .isNotNull()
                .isEmpty();
    }

    /**
     * Test that Room uses CopyOnWriteArrayList for thread safety.
     */
    @Test
    void shouldUseCopyOnWriteArrayList() {
        // Arrange & Act
        Room room = Room.builder()
                .roomId("TEST-12345")
                .players(new CopyOnWriteArrayList<>())
                .build();

        // Assert
        assertThat(room.getPlayers())
                .as("Players list should be CopyOnWriteArrayList for thread safety")
                .isInstanceOf(CopyOnWriteArrayList.class);
    }

    /**
     * Test that getPlayer() finds player by ID.
     */
    @Test
    void shouldGetPlayerById() {
        // Arrange
        Player player1 = Player.builder().id("player-1").username("User1").build();
        Player player2 = Player.builder().id("player-2").username("User2").build();

        Room room = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(List.of(player1, player2)))
                .build();

        // Act
        Optional<Player> found = room.getPlayer("player-1");

        // Assert
        assertThat(found)
                .as("Should find player by ID")
                .isPresent()
                .contains(player1);
    }

    /**
     * Test that getPlayer() returns empty Optional when player not found.
     */
    @Test
    void shouldReturnEmptyWhenPlayerNotFound() {
        // Arrange
        Room room = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>())
                .build();

        // Act
        Optional<Player> found = room.getPlayer("nonexistent");

        // Assert
        assertThat(found)
                .as("Should return empty Optional for nonexistent player")
                .isEmpty();
    }

    /**
     * Test that getPlayersByTeam() filters players by team.
     */
    @Test
    void shouldGetPlayersByTeam() {
        // Arrange
        Player bluePlayer = Player.builder().id("1").team(Team.BLUE).build();
        Player redPlayer = Player.builder().id("2").team(Team.RED).build();
        Player spectator = Player.builder().id("3").team(null).build();

        Room room = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(List.of(bluePlayer, redPlayer, spectator)))
                .build();

        // Act
        List<Player> bluePlayers = room.getPlayersByTeam(Team.BLUE);

        // Assert
        assertThat(bluePlayers)
                .as("Should return only BLUE team players")
                .hasSize(1)
                .contains(bluePlayer)
                .doesNotContain(redPlayer, spectator);
    }

    /**
     * Test that getPlayersByTeam() returns empty list for team with no players.
     */
    @Test
    void shouldReturnEmptyListForTeamWithNoPlayers() {
        // Arrange
        Room room = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>())
                .build();

        // Act
        List<Player> bluePlayers = room.getPlayersByTeam(Team.BLUE);

        // Assert
        assertThat(bluePlayers)
                .as("Should return empty list when no players on team")
                .isEmpty();
    }

    /**
     * Test that canStart() returns false without required players.
     */
    @Test
    void canStartShouldReturnFalseWithoutPlayers() {
        // Arrange
        Room room = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>())
                .build();

        // Act
        boolean canStart = room.canStart();

        // Assert
        assertThat(canStart)
                .as("Cannot start game without players")
                .isFalse();
    }

    /**
     * Test that canStart() returns false with only Blue spymaster.
     */
    @Test
    void canStartShouldReturnFalseWithOnlyBlueSpymaster() {
        // Arrange
        Player blueSpymaster = Player.builder()
                .id("1")
                .team(Team.BLUE)
                .role(Role.SPYMASTER)
                .build();

        Room room = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(List.of(blueSpymaster)))
                .build();

        // Act
        boolean canStart = room.canStart();

        // Assert
        assertThat(canStart)
                .as("Cannot start with only one spymaster")
                .isFalse();
    }

    /**
     * Test that canStart() returns false with only Red spymaster.
     */
    @Test
    void canStartShouldReturnFalseWithOnlyRedSpymaster() {
        // Arrange
        Player redSpymaster = Player.builder()
                .id("1")
                .team(Team.RED)
                .role(Role.SPYMASTER)
                .build();

        Room room = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(List.of(redSpymaster)))
                .build();

        // Act
        boolean canStart = room.canStart();

        // Assert
        assertThat(canStart)
                .as("Cannot start with only one spymaster")
                .isFalse();
    }

    /**
     * Test that canStart() returns false without operatives.
     */
    @Test
    void canStartShouldReturnFalseWithoutOperatives() {
        // Arrange
        Player blueSpymaster = Player.builder().id("1").team(Team.BLUE).role(Role.SPYMASTER).build();
        Player redSpymaster = Player.builder().id("2").team(Team.RED).role(Role.SPYMASTER).build();

        Room room = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(List.of(blueSpymaster, redSpymaster)))
                .build();

        // Act
        boolean canStart = room.canStart();

        // Assert
        assertThat(canStart)
                .as("Cannot start without operatives")
                .isFalse();
    }

    /**
     * Test that canStart() returns true with minimal required setup.
     * Requires: 1 Blue spymaster, 1 Red spymaster, 1 Blue operative, 1 Red operative.
     * All players must be connected.
     */
    @Test
    void canStartShouldReturnTrueWithMinimalSetup() {
        // Arrange
        Player blueSpymaster = Player.builder().id("1").team(Team.BLUE).role(Role.SPYMASTER).connected(true).build();
        Player redSpymaster = Player.builder().id("2").team(Team.RED).role(Role.SPYMASTER).connected(true).build();
        Player blueOperative = Player.builder().id("3").team(Team.BLUE).role(Role.OPERATIVE).connected(true).build();
        Player redOperative = Player.builder().id("4").team(Team.RED).role(Role.OPERATIVE).connected(true).build();

        Room room = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(List.of(
                        blueSpymaster, redSpymaster, blueOperative, redOperative
                )))
                .build();

        // Act
        boolean canStart = room.canStart();

        // Assert
        assertThat(canStart)
                .as("Should be able to start with minimal setup")
                .isTrue();
    }

    /**
     * Test that canStart() returns true with multiple operatives per team.
     * All players must be connected.
     */
    @Test
    void canStartShouldReturnTrueWithMultipleOperatives() {
        // Arrange
        Player blueSpymaster = Player.builder().id("1").team(Team.BLUE).role(Role.SPYMASTER).connected(true).build();
        Player redSpymaster = Player.builder().id("2").team(Team.RED).role(Role.SPYMASTER).connected(true).build();
        Player blueOp1 = Player.builder().id("3").team(Team.BLUE).role(Role.OPERATIVE).connected(true).build();
        Player blueOp2 = Player.builder().id("4").team(Team.BLUE).role(Role.OPERATIVE).connected(true).build();
        Player redOp1 = Player.builder().id("5").team(Team.RED).role(Role.OPERATIVE).connected(true).build();
        Player redOp2 = Player.builder().id("6").team(Team.RED).role(Role.OPERATIVE).connected(true).build();

        Room room = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(List.of(
                        blueSpymaster, redSpymaster, blueOp1, blueOp2, redOp1, redOp2
                )))
                .build();

        // Act
        boolean canStart = room.canStart();

        // Assert
        assertThat(canStart)
                .as("Should be able to start with multiple operatives")
                .isTrue();
    }

    /**
     * Test that canStart() returns false when a required player is disconnected.
     */
    @Test
    void canStartShouldReturnFalseWhenRequiredPlayerDisconnected() {
        // Arrange - all players present but red spymaster is disconnected
        Player blueSpymaster = Player.builder().id("1").team(Team.BLUE).role(Role.SPYMASTER).connected(true).build();
        Player redSpymaster = Player.builder().id("2").team(Team.RED).role(Role.SPYMASTER).connected(false).build();
        Player blueOperative = Player.builder().id("3").team(Team.BLUE).role(Role.OPERATIVE).connected(true).build();
        Player redOperative = Player.builder().id("4").team(Team.RED).role(Role.OPERATIVE).connected(true).build();

        Room room = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(List.of(
                        blueSpymaster, redSpymaster, blueOperative, redOperative
                )))
                .build();

        // Act
        boolean canStart = room.canStart();

        // Assert
        assertThat(canStart)
                .as("Cannot start when required player is disconnected")
                .isFalse();
    }

    /**
     * Test that Room can have null gameState (lobby mode).
     */
    @Test
    void shouldAllowNullGameState() {
        // Arrange & Act
        Room room = Room.builder()
                .roomId("TEST")
                .gameState(null)
                .build();

        // Assert
        assertThat(room.getGameState())
                .as("GameState should be null in lobby")
                .isNull();
    }

    /**
     * Test that Room stores createdAt timestamp.
     */
    @Test
    void shouldStoreCreatedAt() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // Act
        Room room = Room.builder()
                .roomId("TEST")
                .createdAt(now)
                .build();

        // Assert
        assertThat(room.getCreatedAt())
                .as("Should store creation timestamp")
                .isEqualTo(now);
    }
}
