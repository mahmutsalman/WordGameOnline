package com.codenames.model;

/**
 * Enum representing the phases of a Codenames game.
 * The game progresses through these phases in order.
 */
public enum GamePhase {
    /**
     * Lobby phase - players joining and setting up teams before game starts.
     */
    LOBBY,

    /**
     * Clue phase - spymaster is giving a clue to their team.
     */
    CLUE,

    /**
     * Guess phase - operatives are guessing words based on the clue.
     */
    GUESS,

    /**
     * Game over phase - game has ended with a winner.
     */
    GAME_OVER
}
