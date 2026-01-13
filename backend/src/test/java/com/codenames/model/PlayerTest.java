package com.codenames.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Player model.
 * Tests player creation, Lombok functionality, and field handling.
 */
class PlayerTest {

    /**
     * Test that Player can be created using Lombok Builder.
     */
    @Test
    void shouldCreatePlayerWithBuilder() {
        // Arrange & Act
        Player player = Player.builder()
                .id("player-123")
                .username("TestUser")
                .team(Team.BLUE)
                .role(Role.OPERATIVE)
                .connected(true)
                .admin(false)
                .build();

        // Assert
        assertThat(player).isNotNull();
        assertThat(player.getId()).isEqualTo("player-123");
        assertThat(player.getUsername()).isEqualTo("TestUser");
    }

    /**
     * Test that all Player fields can be set and retrieved.
     */
    @Test
    void shouldSetAllFields() {
        // Arrange & Act
        Player player = Player.builder()
                .id("id-1")
                .username("User1")
                .team(Team.RED)
                .role(Role.SPYMASTER)
                .connected(false)
                .admin(true)
                .build();

        // Assert
        assertThat(player.getId()).isEqualTo("id-1");
        assertThat(player.getUsername()).isEqualTo("User1");
        assertThat(player.getTeam()).isEqualTo(Team.RED);
        assertThat(player.getRole()).isEqualTo(Role.SPYMASTER);
        assertThat(player.isConnected()).isFalse();
        assertThat(player.isAdmin()).isTrue();
    }

    /**
     * Test that Player can handle null team (spectators don't have a team yet).
     */
    @Test
    void shouldHandleNullTeam() {
        // Arrange & Act
        Player player = Player.builder()
                .id("player-1")
                .username("Spectator")
                .team(null)
                .role(Role.SPECTATOR)
                .connected(true)
                .admin(false)
                .build();

        // Assert
        assertThat(player.getTeam())
                .as("Spectators should have null team")
                .isNull();
    }

    /**
     * Test that Player admin field defaults to false if not explicitly set.
     */
    @Test
    void shouldDefaultToNotAdmin() {
        // Arrange & Act
        Player player = new Player();
        player.setId("player-1");
        player.setUsername("RegularUser");
        player.setAdmin(false);

        // Assert
        assertThat(player.isAdmin())
                .as("Players should default to non-admin")
                .isFalse();
    }

    /**
     * Test that Player connected field defaults to true if not explicitly set.
     */
    @Test
    void shouldDefaultToConnected() {
        // Arrange & Act
        Player player = new Player();
        player.setId("player-1");
        player.setUsername("User");
        player.setConnected(true);

        // Assert
        assertThat(player.isConnected())
                .as("Players should default to connected")
                .isTrue();
    }

    /**
     * Test that Player role can be changed dynamically.
     */
    @Test
    void shouldSupportRoleChange() {
        // Arrange
        Player player = Player.builder()
                .id("player-1")
                .username("User")
                .role(Role.SPECTATOR)
                .build();

        // Act
        player.setRole(Role.OPERATIVE);

        // Assert
        assertThat(player.getRole())
                .as("Player role should be changeable")
                .isEqualTo(Role.OPERATIVE);
    }

    /**
     * Test that Player team can be changed dynamically.
     */
    @Test
    void shouldSupportTeamChange() {
        // Arrange
        Player player = Player.builder()
                .id("player-1")
                .username("User")
                .team(null)
                .build();

        // Act
        player.setTeam(Team.BLUE);

        // Assert
        assertThat(player.getTeam())
                .as("Player team should be changeable")
                .isEqualTo(Team.BLUE);
    }

    /**
     * Test that Player equality is based on ID.
     */
    @Test
    void shouldHaveEqualsBasedOnId() {
        // Arrange
        Player player1 = Player.builder()
                .id("same-id")
                .username("User1")
                .build();

        Player player2 = Player.builder()
                .id("same-id")
                .username("User2")
                .build();

        Player player3 = Player.builder()
                .id("different-id")
                .username("User1")
                .build();

        // Act & Assert
        assertThat(player1)
                .as("Players with same ID should be equal")
                .isEqualTo(player2);

        assertThat(player1)
                .as("Players with different ID should not be equal")
                .isNotEqualTo(player3);
    }

    /**
     * Test that Player hashCode is consistent with equals.
     */
    @Test
    void shouldHaveProperHashCode() {
        // Arrange
        Player player1 = Player.builder()
                .id("id-1")
                .username("User")
                .build();

        Player player2 = Player.builder()
                .id("id-1")
                .username("Different")
                .build();

        // Act & Assert
        assertThat(player1.hashCode())
                .as("Equal players should have same hashCode")
                .isEqualTo(player2.hashCode());
    }

    /**
     * Test that Lombok getters and setters work correctly.
     */
    @Test
    void shouldSupportLombokGettersSetters() {
        // Arrange
        Player player = new Player();

        // Act
        player.setId("test-id");
        player.setUsername("TestUser");
        player.setTeam(Team.BLUE);
        player.setRole(Role.OPERATIVE);
        player.setConnected(true);
        player.setAdmin(false);

        // Assert
        assertThat(player.getId()).isEqualTo("test-id");
        assertThat(player.getUsername()).isEqualTo("TestUser");
        assertThat(player.getTeam()).isEqualTo(Team.BLUE);
        assertThat(player.getRole()).isEqualTo(Role.OPERATIVE);
        assertThat(player.isConnected()).isTrue();
        assertThat(player.isAdmin()).isFalse();
    }
}
