package com.codenames.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers.
 * Handles validation errors and custom exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid annotations.
     *
     * @param ex the validation exception
     * @return map of field names to error messages
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(
            org.springframework.web.bind.MethodArgumentNotValidException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        java.util.Map<String, String> errors = new java.util.HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return org.springframework.http.ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handles RoomNotFoundException and returns 404.
     *
     * @param ex the exception
     * @return error response with message
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(com.codenames.exception.RoomNotFoundException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> handleRoomNotFound(
            com.codenames.exception.RoomNotFoundException ex) {
        return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body(java.util.Map.of("error", ex.getMessage()));
    }

    /**
     * Handles UsernameAlreadyExistsException.
     * Returns 409 CONFLICT status.
     *
     * @param ex the exception
     * @return response entity with error message
     */
    @ExceptionHandler(com.codenames.exception.UsernameAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUsernameAlreadyExists(
            com.codenames.exception.UsernameAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles GameStartException.
     * Returns 400 BAD REQUEST status when game can't be started.
     *
     * @param ex the exception
     * @return response entity with error message
     */
    @ExceptionHandler(com.codenames.exception.GameStartException.class)
    public ResponseEntity<Map<String, String>> handleGameStartException(
            com.codenames.exception.GameStartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles WordPackNotFoundException.
     * Returns 404 NOT FOUND status when word pack doesn't exist.
     *
     * @param ex the exception
     * @return response entity with error message
     */
    @ExceptionHandler(com.codenames.exception.WordPackNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleWordPackNotFound(
            com.codenames.exception.WordPackNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
