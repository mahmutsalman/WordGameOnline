package com.codenames.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing a card on the Codenames game board.
 * Each card has a word that players try to guess and a hidden color
 * that determines which team the card belongs to.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    /**
     * The word displayed on the card.
     */
    private String word;

    /**
     * The color of the card (BLUE, RED, NEUTRAL, or ASSASSIN).
     * This is hidden from operatives until the card is revealed.
     */
    private CardColor color;

    /**
     * Whether the card has been revealed (guessed).
     * Defaults to false for new cards.
     */
    @Builder.Default
    private boolean revealed = false;

    /**
     * The ID of the player who selected/revealed this card.
     * Null if the card has not been selected yet.
     */
    private String selectedBy;
}
