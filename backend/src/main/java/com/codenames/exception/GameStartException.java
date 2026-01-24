package com.codenames.exception;

/**
 * Exception thrown when a game cannot be started due to invalid room state.
 */
public class GameStartException extends RuntimeException {

    /**
     * Creates a new GameStartException with a message.
     *
     * @param message the error message
     */
    public GameStartException(String message) {
        super(message);
    }
}
