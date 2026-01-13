package com.codenames.model;

/**
 * Enum representing player roles in Codenames game.
 * Each player has one role that determines their responsibilities.
 */
public enum Role {
    /**
     * Spymaster role - gives clues to help operatives guess words.
     * Can see all card colors on the board.
     */
    SPYMASTER,

    /**
     * Operative role - guesses words based on spymaster's clues.
     * Cannot see card colors until revealed.
     */
    OPERATIVE,

    /**
     * Spectator role - watches the game without participating.
     * Default role for players in the lobby before joining a team.
     */
    SPECTATOR
}
