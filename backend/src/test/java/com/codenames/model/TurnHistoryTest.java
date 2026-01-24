package com.codenames.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurnHistory model.
 * Tests turn history creation and tracking of game turns.
 */
class TurnHistoryTest {

    /**
     * Test that TurnHistory can be created using Lombok Builder.
     */
    @Test
    void shouldCreateTurnHistoryWithBuilder() {
        // Arrange
        Clue clue = Clue.builder()
                .word("NATURE")
                .number(2)
                .team(Team.BLUE)
                .build();

        // Act
        TurnHistory history = TurnHistory.builder()
                .team(Team.BLUE)
                .clue(clue)
                .guessedWords(List.of("TREE", "FLOWER"))
                .guessedColors(List.of(CardColor.BLUE, CardColor.BLUE))
                .build();

        // Assert
        assertThat(history).isNotNull();
        assertThat(history.getTeam()).isEqualTo(Team.BLUE);
        assertThat(history.getClue()).isEqualTo(clue);
    }

    /**
     * Test that all TurnHistory fields can be set and retrieved.
     */
    @Test
    void shouldSetAllFields() {
        // Arrange
        Clue clue = Clue.builder()
                .word("FOOD")
                .number(3)
                .team(Team.RED)
                .build();
        List<String> words = List.of("APPLE", "ORANGE");
        List<CardColor> colors = List.of(CardColor.RED, CardColor.NEUTRAL);

        // Act
        TurnHistory history = TurnHistory.builder()
                .team(Team.RED)
                .clue(clue)
                .guessedWords(words)
                .guessedColors(colors)
                .build();

        // Assert
        assertThat(history.getTeam()).isEqualTo(Team.RED);
        assertThat(history.getClue()).isEqualTo(clue);
        assertThat(history.getGuessedWords()).containsExactly("APPLE", "ORANGE");
        assertThat(history.getGuessedColors()).containsExactly(CardColor.RED, CardColor.NEUTRAL);
    }

    /**
     * Test that TurnHistory can be created for blue team.
     */
    @Test
    void shouldSupportBlueTeam() {
        // Arrange & Act
        TurnHistory history = TurnHistory.builder()
                .team(Team.BLUE)
                .build();

        // Assert
        assertThat(history.getTeam())
                .as("TurnHistory should support BLUE team")
                .isEqualTo(Team.BLUE);
    }

    /**
     * Test that TurnHistory can be created for red team.
     */
    @Test
    void shouldSupportRedTeam() {
        // Arrange & Act
        TurnHistory history = TurnHistory.builder()
                .team(Team.RED)
                .build();

        // Assert
        assertThat(history.getTeam())
                .as("TurnHistory should support RED team")
                .isEqualTo(Team.RED);
    }

    /**
     * Test that guessedWords can be empty.
     */
    @Test
    void shouldSupportEmptyGuessedWords() {
        // Arrange & Act
        TurnHistory history = TurnHistory.builder()
                .team(Team.BLUE)
                .guessedWords(List.of())
                .guessedColors(List.of())
                .build();

        // Assert
        assertThat(history.getGuessedWords())
                .as("TurnHistory should support empty guessed words")
                .isEmpty();
    }

    /**
     * Test that guessedWords and guessedColors track multiple guesses.
     */
    @Test
    void shouldTrackMultipleGuesses() {
        // Arrange & Act
        TurnHistory history = TurnHistory.builder()
                .team(Team.RED)
                .guessedWords(List.of("WORD1", "WORD2", "WORD3"))
                .guessedColors(List.of(CardColor.RED, CardColor.RED, CardColor.ASSASSIN))
                .build();

        // Assert
        assertThat(history.getGuessedWords())
                .as("Should track multiple guessed words")
                .hasSize(3);
        assertThat(history.getGuessedColors())
                .as("Should track multiple guessed colors")
                .hasSize(3);
    }

    /**
     * Test that guessedColors can contain all color types.
     */
    @Test
    void shouldSupportAllColorTypes() {
        // Arrange & Act
        TurnHistory history = TurnHistory.builder()
                .team(Team.BLUE)
                .guessedColors(List.of(
                        CardColor.BLUE,
                        CardColor.RED,
                        CardColor.NEUTRAL,
                        CardColor.ASSASSIN))
                .build();

        // Assert
        assertThat(history.getGuessedColors())
                .as("Should support all card colors")
                .containsExactly(
                        CardColor.BLUE,
                        CardColor.RED,
                        CardColor.NEUTRAL,
                        CardColor.ASSASSIN);
    }

    /**
     * Test that Lombok getters and setters work correctly.
     */
    @Test
    void shouldSupportLombokGettersSetters() {
        // Arrange
        TurnHistory history = new TurnHistory();
        Clue clue = Clue.builder().word("TEST").number(1).team(Team.BLUE).build();

        // Act
        history.setTeam(Team.BLUE);
        history.setClue(clue);
        history.setGuessedWords(new ArrayList<>(List.of("GUESS")));
        history.setGuessedColors(new ArrayList<>(List.of(CardColor.BLUE)));

        // Assert
        assertThat(history.getTeam()).isEqualTo(Team.BLUE);
        assertThat(history.getClue()).isEqualTo(clue);
        assertThat(history.getGuessedWords()).containsExactly("GUESS");
        assertThat(history.getGuessedColors()).containsExactly(CardColor.BLUE);
    }

    /**
     * Test that TurnHistory has proper equals.
     */
    @Test
    void shouldHaveProperEquals() {
        // Arrange
        Clue clue = Clue.builder().word("SAME").number(1).team(Team.BLUE).build();

        TurnHistory history1 = TurnHistory.builder()
                .team(Team.BLUE)
                .clue(clue)
                .guessedWords(List.of("A"))
                .guessedColors(List.of(CardColor.BLUE))
                .build();

        TurnHistory history2 = TurnHistory.builder()
                .team(Team.BLUE)
                .clue(clue)
                .guessedWords(List.of("A"))
                .guessedColors(List.of(CardColor.BLUE))
                .build();

        TurnHistory history3 = TurnHistory.builder()
                .team(Team.RED)
                .clue(clue)
                .build();

        // Assert
        assertThat(history1)
                .as("Histories with same fields should be equal")
                .isEqualTo(history2);

        assertThat(history1)
                .as("Histories with different teams should not be equal")
                .isNotEqualTo(history3);
    }

    /**
     * Test that TurnHistory has proper hashCode.
     */
    @Test
    void shouldHaveProperHashCode() {
        // Arrange
        TurnHistory history1 = TurnHistory.builder()
                .team(Team.BLUE)
                .guessedWords(List.of("WORD"))
                .build();

        TurnHistory history2 = TurnHistory.builder()
                .team(Team.BLUE)
                .guessedWords(List.of("WORD"))
                .build();

        // Assert
        assertThat(history1.hashCode())
                .as("Equal histories should have same hashCode")
                .isEqualTo(history2.hashCode());
    }
}
