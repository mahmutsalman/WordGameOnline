package com.codenames.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Team enum.
 * Tests team values and opposite team functionality.
 */
class TeamTest {

    /**
     * Test that the Team enum has exactly two teams: BLUE and RED.
     */
    @Test
    void shouldHaveTwoTeams() {
        // Arrange & Act
        Team[] teams = Team.values();

        // Assert
        assertThat(teams)
                .as("Team enum should have exactly 2 values")
                .hasSize(2);
        assertThat(teams)
                .as("Team enum should contain BLUE and RED")
                .contains(Team.BLUE, Team.RED);
    }

    /**
     * Test that the opposite() method exists and returns a Team.
     * This verifies the method signature is correct.
     */
    @Test
    void shouldReturnOppositeTeam() {
        // Arrange
        Team blue = Team.BLUE;

        // Act
        Team opposite = blue.opposite();

        // Assert
        assertThat(opposite)
                .as("opposite() should return a Team value")
                .isNotNull()
                .isInstanceOf(Team.class);
    }

    /**
     * Test that BLUE team's opposite is RED.
     */
    @Test
    void blueOppositeShouldBeRed() {
        // Arrange
        Team blue = Team.BLUE;

        // Act
        Team opposite = blue.opposite();

        // Assert
        assertThat(opposite)
                .as("BLUE team's opposite should be RED")
                .isEqualTo(Team.RED);
    }

    /**
     * Test that RED team's opposite is BLUE.
     */
    @Test
    void redOppositeShouldBeBlue() {
        // Arrange
        Team red = Team.RED;

        // Act
        Team opposite = red.opposite();

        // Assert
        assertThat(opposite)
                .as("RED team's opposite should be BLUE")
                .isEqualTo(Team.BLUE);
    }
}
