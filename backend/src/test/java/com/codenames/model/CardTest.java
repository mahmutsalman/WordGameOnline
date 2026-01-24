package com.codenames.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Card model.
 * Tests card creation, state management, and Lombok functionality.
 */
class CardTest {

    /**
     * Test that Card can be created using Lombok Builder.
     */
    @Test
    void shouldCreateCardWithBuilder() {
        // Arrange & Act
        Card card = Card.builder()
                .word("APPLE")
                .color(CardColor.BLUE)
                .build();

        // Assert
        assertThat(card).isNotNull();
        assertThat(card.getWord()).isEqualTo("APPLE");
        assertThat(card.getColor()).isEqualTo(CardColor.BLUE);
    }

    /**
     * Test that revealed defaults to false.
     */
    @Test
    void shouldDefaultRevealedToFalse() {
        // Arrange & Act
        Card card = Card.builder()
                .word("TEST")
                .color(CardColor.RED)
                .build();

        // Assert
        assertThat(card.isRevealed())
                .as("Card should default to not revealed")
                .isFalse();
    }

    /**
     * Test that selectedBy defaults to null.
     */
    @Test
    void shouldDefaultSelectedByToNull() {
        // Arrange & Act
        Card card = Card.builder()
                .word("TEST")
                .color(CardColor.NEUTRAL)
                .build();

        // Assert
        assertThat(card.getSelectedBy())
                .as("Card should default to no selector")
                .isNull();
    }

    /**
     * Test that all Card fields can be set and retrieved.
     */
    @Test
    void shouldSetAllFields() {
        // Arrange & Act
        Card card = Card.builder()
                .word("CASTLE")
                .color(CardColor.ASSASSIN)
                .revealed(true)
                .selectedBy("player-123")
                .build();

        // Assert
        assertThat(card.getWord()).isEqualTo("CASTLE");
        assertThat(card.getColor()).isEqualTo(CardColor.ASSASSIN);
        assertThat(card.isRevealed()).isTrue();
        assertThat(card.getSelectedBy()).isEqualTo("player-123");
    }

    /**
     * Test that Card can be created for each color type.
     */
    @Test
    void shouldSupportAllColorTypes() {
        // Arrange & Act
        Card blueCard = Card.builder().word("BLUE1").color(CardColor.BLUE).build();
        Card redCard = Card.builder().word("RED1").color(CardColor.RED).build();
        Card neutralCard = Card.builder().word("NEUTRAL1").color(CardColor.NEUTRAL).build();
        Card assassinCard = Card.builder().word("ASSASSIN1").color(CardColor.ASSASSIN).build();

        // Assert
        assertThat(blueCard.getColor()).isEqualTo(CardColor.BLUE);
        assertThat(redCard.getColor()).isEqualTo(CardColor.RED);
        assertThat(neutralCard.getColor()).isEqualTo(CardColor.NEUTRAL);
        assertThat(assassinCard.getColor()).isEqualTo(CardColor.ASSASSIN);
    }

    /**
     * Test that revealed state can be changed.
     */
    @Test
    void shouldSupportRevealStateChange() {
        // Arrange
        Card card = Card.builder()
                .word("HIDDEN")
                .color(CardColor.BLUE)
                .revealed(false)
                .build();

        // Act
        card.setRevealed(true);

        // Assert
        assertThat(card.isRevealed())
                .as("Card reveal state should be changeable")
                .isTrue();
    }

    /**
     * Test that selectedBy can be set after creation.
     */
    @Test
    void shouldSupportSettingSelectedBy() {
        // Arrange
        Card card = Card.builder()
                .word("TARGET")
                .color(CardColor.RED)
                .build();

        // Act
        card.setSelectedBy("player-456");

        // Assert
        assertThat(card.getSelectedBy())
                .as("Card selectedBy should be settable")
                .isEqualTo("player-456");
    }

    /**
     * Test that Lombok getters and setters work correctly.
     */
    @Test
    void shouldSupportLombokGettersSetters() {
        // Arrange
        Card card = new Card();

        // Act
        card.setWord("WORD");
        card.setColor(CardColor.NEUTRAL);
        card.setRevealed(false);
        card.setSelectedBy(null);

        // Assert
        assertThat(card.getWord()).isEqualTo("WORD");
        assertThat(card.getColor()).isEqualTo(CardColor.NEUTRAL);
        assertThat(card.isRevealed()).isFalse();
        assertThat(card.getSelectedBy()).isNull();
    }

    /**
     * Test that Card has proper equals based on all fields.
     */
    @Test
    void shouldHaveProperEquals() {
        // Arrange
        Card card1 = Card.builder()
                .word("SAME")
                .color(CardColor.BLUE)
                .revealed(false)
                .build();

        Card card2 = Card.builder()
                .word("SAME")
                .color(CardColor.BLUE)
                .revealed(false)
                .build();

        Card card3 = Card.builder()
                .word("DIFFERENT")
                .color(CardColor.BLUE)
                .revealed(false)
                .build();

        // Assert
        assertThat(card1)
                .as("Cards with same fields should be equal")
                .isEqualTo(card2);

        assertThat(card1)
                .as("Cards with different words should not be equal")
                .isNotEqualTo(card3);
    }

    /**
     * Test that Card has proper hashCode.
     */
    @Test
    void shouldHaveProperHashCode() {
        // Arrange
        Card card1 = Card.builder()
                .word("HASH")
                .color(CardColor.RED)
                .build();

        Card card2 = Card.builder()
                .word("HASH")
                .color(CardColor.RED)
                .build();

        // Assert
        assertThat(card1.hashCode())
                .as("Equal cards should have same hashCode")
                .isEqualTo(card2.hashCode());
    }

    /**
     * Test that Card handles empty word.
     */
    @Test
    void shouldHandleEmptyWord() {
        // Arrange & Act
        Card card = Card.builder()
                .word("")
                .color(CardColor.NEUTRAL)
                .build();

        // Assert
        assertThat(card.getWord())
                .as("Card should handle empty word")
                .isEmpty();
    }
}
