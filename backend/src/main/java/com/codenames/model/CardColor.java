package com.codenames.model;

/**
 * Enum representing the possible colors of cards on the Codenames game board.
 * Each card can be one of four colors, determining its significance in the game.
 */
public enum CardColor {
    /**
     * Blue team's card - guessing reveals a blue agent.
     */
    BLUE,

    /**
     * Red team's card - guessing reveals a red agent.
     */
    RED,

    /**
     * Neutral card - guessing ends the turn with no team benefit.
     */
    NEUTRAL,

    /**
     * Assassin card - guessing causes immediate loss for the guessing team.
     */
    ASSASSIN
}
