package com.codenames.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model representing the history of a single turn in the Codenames game.
 * Tracks the team that played, the clue given, and all guesses made.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnHistory {
    /**
     * The team that took this turn.
     */
    private Team team;

    /**
     * The clue given by the spymaster during this turn.
     */
    private Clue clue;

    /**
     * The words that were guessed during this turn, in order.
     */
    private List<String> guessedWords;

    /**
     * The colors of the cards that were revealed by the guesses, in order.
     * Corresponds positionally to guessedWords.
     */
    private List<CardColor> guessedColors;
}
