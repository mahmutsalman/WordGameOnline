package com.codenames.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to join a room with a username that already exists.
 * Returns HTTP 409 CONFLICT status.
 */
@ResponseStatus(org.springframework.http.HttpStatus.CONFLICT)
public class UsernameAlreadyExistsException extends RuntimeException {
    /**
     * Creates exception with username in message.
     *
     * @param username the username that already exists
     */
    public UsernameAlreadyExistsException(String username) {
        super("Username already taken: " + username);
    }
}
