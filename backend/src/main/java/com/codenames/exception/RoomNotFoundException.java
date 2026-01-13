package com.codenames.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested room cannot be found.
 * Results in HTTP 404 NOT_FOUND response.
 */
@ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
public class RoomNotFoundException extends RuntimeException {
    /**
     * Creates exception with room ID in message.
     *
     * @param roomId the room ID that was not found
     */
    public RoomNotFoundException(String roomId) {
        super("Room not found: " + roomId);
    }
}
