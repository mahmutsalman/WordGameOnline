package com.codenames.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GamePhase enum.
 * Tests game phase values for the Codenames game flow.
 */
class GamePhaseTest {

    /**
     * Test that the GamePhase enum has exactly four phases.
     */
    @Test
    void shouldHaveFourPhases() {
        // Arrange & Act
        GamePhase[] phases = GamePhase.values();

        // Assert
        assertThat(phases)
                .as("GamePhase enum should have exactly 4 values")
                .hasSize(4);
    }

    /**
     * Test that GamePhase contains all expected values.
     */
    @Test
    void shouldContainAllExpectedPhases() {
        // Arrange & Act
        GamePhase[] phases = GamePhase.values();

        // Assert
        assertThat(phases)
                .as("GamePhase enum should contain LOBBY, CLUE, GUESS, and GAME_OVER")
                .contains(GamePhase.LOBBY, GamePhase.CLUE, GamePhase.GUESS, GamePhase.GAME_OVER);
    }

    /**
     * Test that LOBBY phase exists.
     */
    @Test
    void shouldHaveLobbyPhase() {
        // Arrange & Act
        GamePhase lobby = GamePhase.LOBBY;

        // Assert
        assertThat(lobby.name())
                .as("LOBBY should have correct name")
                .isEqualTo("LOBBY");
    }

    /**
     * Test that CLUE phase exists.
     */
    @Test
    void shouldHaveCluePhase() {
        // Arrange & Act
        GamePhase clue = GamePhase.CLUE;

        // Assert
        assertThat(clue.name())
                .as("CLUE should have correct name")
                .isEqualTo("CLUE");
    }

    /**
     * Test that GUESS phase exists.
     */
    @Test
    void shouldHaveGuessPhase() {
        // Arrange & Act
        GamePhase guess = GamePhase.GUESS;

        // Assert
        assertThat(guess.name())
                .as("GUESS should have correct name")
                .isEqualTo("GUESS");
    }

    /**
     * Test that GAME_OVER phase exists.
     */
    @Test
    void shouldHaveGameOverPhase() {
        // Arrange & Act
        GamePhase gameOver = GamePhase.GAME_OVER;

        // Assert
        assertThat(gameOver.name())
                .as("GAME_OVER should have correct name")
                .isEqualTo("GAME_OVER");
    }

    /**
     * Test that phases follow correct order (ordinal values).
     */
    @Test
    void shouldHaveCorrectPhaseOrder() {
        // Assert
        assertThat(GamePhase.LOBBY.ordinal())
                .as("LOBBY should be first (ordinal 0)")
                .isLessThan(GamePhase.CLUE.ordinal());

        assertThat(GamePhase.CLUE.ordinal())
                .as("CLUE should come before GUESS")
                .isLessThan(GamePhase.GUESS.ordinal());

        assertThat(GamePhase.GUESS.ordinal())
                .as("GUESS should come before GAME_OVER")
                .isLessThan(GamePhase.GAME_OVER.ordinal());
    }

    /**
     * Test that GamePhase can be retrieved by name.
     */
    @Test
    void shouldBeRetrievableByName() {
        // Arrange & Act
        GamePhase lobby = GamePhase.valueOf("LOBBY");
        GamePhase clue = GamePhase.valueOf("CLUE");
        GamePhase guess = GamePhase.valueOf("GUESS");
        GamePhase gameOver = GamePhase.valueOf("GAME_OVER");

        // Assert
        assertThat(lobby).isEqualTo(GamePhase.LOBBY);
        assertThat(clue).isEqualTo(GamePhase.CLUE);
        assertThat(guess).isEqualTo(GamePhase.GUESS);
        assertThat(gameOver).isEqualTo(GamePhase.GAME_OVER);
    }
}
