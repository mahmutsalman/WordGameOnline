package com.codenames.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Clue model.
 * Tests clue creation and Lombok functionality.
 */
class ClueTest {

    /**
     * Test that Clue can be created using Lombok Builder.
     */
    @Test
    void shouldCreateClueWithBuilder() {
        // Arrange & Act
        Clue clue = Clue.builder()
                .word("ANIMAL")
                .number(3)
                .team(Team.BLUE)
                .build();

        // Assert
        assertThat(clue).isNotNull();
        assertThat(clue.getWord()).isEqualTo("ANIMAL");
        assertThat(clue.getNumber()).isEqualTo(3);
        assertThat(clue.getTeam()).isEqualTo(Team.BLUE);
    }

    /**
     * Test that all Clue fields can be set and retrieved.
     */
    @Test
    void shouldSetAllFields() {
        // Arrange & Act
        Clue clue = Clue.builder()
                .word("TRAVEL")
                .number(2)
                .team(Team.RED)
                .build();

        // Assert
        assertThat(clue.getWord()).isEqualTo("TRAVEL");
        assertThat(clue.getNumber()).isEqualTo(2);
        assertThat(clue.getTeam()).isEqualTo(Team.RED);
    }

    /**
     * Test that Clue can be created for blue team.
     */
    @Test
    void shouldSupportBlueTeam() {
        // Arrange & Act
        Clue clue = Clue.builder()
                .word("TEST")
                .number(1)
                .team(Team.BLUE)
                .build();

        // Assert
        assertThat(clue.getTeam())
                .as("Clue should support BLUE team")
                .isEqualTo(Team.BLUE);
    }

    /**
     * Test that Clue can be created for red team.
     */
    @Test
    void shouldSupportRedTeam() {
        // Arrange & Act
        Clue clue = Clue.builder()
                .word("TEST")
                .number(1)
                .team(Team.RED)
                .build();

        // Assert
        assertThat(clue.getTeam())
                .as("Clue should support RED team")
                .isEqualTo(Team.RED);
    }

    /**
     * Test that Clue number can be zero (unlimited guesses).
     */
    @Test
    void shouldSupportZeroNumber() {
        // Arrange & Act
        Clue clue = Clue.builder()
                .word("UNLIMITED")
                .number(0)
                .team(Team.BLUE)
                .build();

        // Assert
        assertThat(clue.getNumber())
                .as("Clue number can be 0 for unlimited guesses")
                .isZero();
    }

    /**
     * Test that Clue supports high numbers.
     */
    @Test
    void shouldSupportHighNumbers() {
        // Arrange & Act
        Clue clue = Clue.builder()
                .word("MANY")
                .number(9)
                .team(Team.RED)
                .build();

        // Assert
        assertThat(clue.getNumber())
                .as("Clue should support high numbers")
                .isEqualTo(9);
    }

    /**
     * Test that Lombok getters and setters work correctly.
     */
    @Test
    void shouldSupportLombokGettersSetters() {
        // Arrange
        Clue clue = new Clue();

        // Act
        clue.setWord("SETTER");
        clue.setNumber(4);
        clue.setTeam(Team.BLUE);

        // Assert
        assertThat(clue.getWord()).isEqualTo("SETTER");
        assertThat(clue.getNumber()).isEqualTo(4);
        assertThat(clue.getTeam()).isEqualTo(Team.BLUE);
    }

    /**
     * Test that Clue has proper equals.
     */
    @Test
    void shouldHaveProperEquals() {
        // Arrange
        Clue clue1 = Clue.builder()
                .word("SAME")
                .number(2)
                .team(Team.BLUE)
                .build();

        Clue clue2 = Clue.builder()
                .word("SAME")
                .number(2)
                .team(Team.BLUE)
                .build();

        Clue clue3 = Clue.builder()
                .word("DIFFERENT")
                .number(2)
                .team(Team.BLUE)
                .build();

        // Assert
        assertThat(clue1)
                .as("Clues with same fields should be equal")
                .isEqualTo(clue2);

        assertThat(clue1)
                .as("Clues with different words should not be equal")
                .isNotEqualTo(clue3);
    }

    /**
     * Test that Clue has proper hashCode.
     */
    @Test
    void shouldHaveProperHashCode() {
        // Arrange
        Clue clue1 = Clue.builder()
                .word("HASH")
                .number(3)
                .team(Team.RED)
                .build();

        Clue clue2 = Clue.builder()
                .word("HASH")
                .number(3)
                .team(Team.RED)
                .build();

        // Assert
        assertThat(clue1.hashCode())
                .as("Equal clues should have same hashCode")
                .isEqualTo(clue2.hashCode());
    }
}
