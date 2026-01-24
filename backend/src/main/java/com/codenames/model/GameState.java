package com.codenames.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model representing the complete state of a Codenames game.
 * Contains the board, current game phase, and all game-related tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    /**
     * The 25-card game board.
     * Standard Codenames has a 5x5 grid of cards.
     */
    private List<Card> board;

    /**
     * The team whose turn it currently is.
     */
    private Team currentTeam;

    /**
     * The current phase of the game.
     */
    private GamePhase phase;

    /**
     * The current clue given by the spymaster.
     * Null during LOBBY and CLUE phases (before a clue is given).
     */
    private Clue currentClue;

    /**
     * Number of guesses remaining for the current turn.
     * Typically clue number + 1, or unlimited if clue number was 0.
     */
    private int guessesRemaining;

    /**
     * Number of blue cards remaining to be revealed.
     * Blue team wins when this reaches 0.
     */
    private int blueRemaining;

    /**
     * Number of red cards remaining to be revealed.
     * Red team wins when this reaches 0.
     */
    private int redRemaining;

    /**
     * The winning team, or null if game is still in progress.
     */
    private Team winner;

    /**
     * History of all turns played in this game.
     */
    private List<TurnHistory> history;

    /**
     * Gets the card at the specified board index.
     *
     * @param index the index of the card (0-24 for standard 5x5 board)
     * @return the card at the specified index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Card getCard(int index) {
        return board.get(index);
    }

    /**
     * Reveals the card at the specified board index.
     * Sets the card's revealed flag to true.
     *
     * @param index the index of the card to reveal (0-24 for standard 5x5 board)
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void revealCard(int index) {
        board.get(index).setRevealed(true);
    }
}
