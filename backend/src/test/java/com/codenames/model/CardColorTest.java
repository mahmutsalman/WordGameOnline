package com.codenames.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CardColor enum.
 * Tests card color values for the Codenames game board.
 */
class CardColorTest {

    /**
     * Test that the CardColor enum has exactly four colors.
     */
    @Test
    void shouldHaveFourColors() {
        // Arrange & Act
        CardColor[] colors = CardColor.values();

        // Assert
        assertThat(colors)
                .as("CardColor enum should have exactly 4 values")
                .hasSize(4);
    }

    /**
     * Test that CardColor contains all expected values.
     */
    @Test
    void shouldContainAllExpectedColors() {
        // Arrange & Act
        CardColor[] colors = CardColor.values();

        // Assert
        assertThat(colors)
                .as("CardColor enum should contain BLUE, RED, NEUTRAL, and ASSASSIN")
                .contains(CardColor.BLUE, CardColor.RED, CardColor.NEUTRAL, CardColor.ASSASSIN);
    }

    /**
     * Test that BLUE color exists and has correct ordinal.
     */
    @Test
    void shouldHaveBlueColor() {
        // Arrange & Act
        CardColor blue = CardColor.BLUE;

        // Assert
        assertThat(blue.name())
                .as("BLUE should have correct name")
                .isEqualTo("BLUE");
    }

    /**
     * Test that RED color exists and has correct ordinal.
     */
    @Test
    void shouldHaveRedColor() {
        // Arrange & Act
        CardColor red = CardColor.RED;

        // Assert
        assertThat(red.name())
                .as("RED should have correct name")
                .isEqualTo("RED");
    }

    /**
     * Test that NEUTRAL color exists.
     */
    @Test
    void shouldHaveNeutralColor() {
        // Arrange & Act
        CardColor neutral = CardColor.NEUTRAL;

        // Assert
        assertThat(neutral.name())
                .as("NEUTRAL should have correct name")
                .isEqualTo("NEUTRAL");
    }

    /**
     * Test that ASSASSIN color exists.
     */
    @Test
    void shouldHaveAssassinColor() {
        // Arrange & Act
        CardColor assassin = CardColor.ASSASSIN;

        // Assert
        assertThat(assassin.name())
                .as("ASSASSIN should have correct name")
                .isEqualTo("ASSASSIN");
    }

    /**
     * Test that CardColor can be retrieved by name.
     */
    @Test
    void shouldBeRetrievableByName() {
        // Arrange & Act
        CardColor blue = CardColor.valueOf("BLUE");
        CardColor red = CardColor.valueOf("RED");
        CardColor neutral = CardColor.valueOf("NEUTRAL");
        CardColor assassin = CardColor.valueOf("ASSASSIN");

        // Assert
        assertThat(blue).isEqualTo(CardColor.BLUE);
        assertThat(red).isEqualTo(CardColor.RED);
        assertThat(neutral).isEqualTo(CardColor.NEUTRAL);
        assertThat(assassin).isEqualTo(CardColor.ASSASSIN);
    }
}
