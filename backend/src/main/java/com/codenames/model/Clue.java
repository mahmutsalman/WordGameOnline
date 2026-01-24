package com.codenames.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing a clue given by a spymaster in the Codenames game.
 * A clue consists of a single word and a number indicating how many
 * cards on the board relate to that clue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clue {
    /**
     * The clue word given by the spymaster.
     * Must be a single word that is not on the board.
     */
    private String word;

    /**
     * The number of cards that relate to the clue.
     * 0 means unlimited guesses, otherwise operatives can guess number + 1 times.
     */
    private int number;

    /**
     * The team that gave this clue.
     */
    private Team team;
}
