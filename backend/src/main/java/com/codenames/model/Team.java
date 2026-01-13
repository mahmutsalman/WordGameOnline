package com.codenames.model;

/**
 * Enum representing the two teams in Codenames game.
 * Each team (BLUE and RED) competes to identify their team's words on the board.
 */
public enum Team {
    /**
     * Blue team.
     */
    BLUE,

    /**
     * Red team.
     */
    RED;

    /**
     * Returns the opposite team.
     * BLUE returns RED, RED returns BLUE.
     *
     * @return the opposite team
     */
    public Team opposite() {
        return this == BLUE ? RED : BLUE;
    }
}
