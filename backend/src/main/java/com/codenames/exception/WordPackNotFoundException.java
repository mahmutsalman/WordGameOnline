package com.codenames.exception;

/**
 * Exception thrown when a requested word pack cannot be found.
 */
public class WordPackNotFoundException extends RuntimeException {

    /**
     * Creates a new WordPackNotFoundException.
     *
     * @param packName the name of the word pack that was not found
     */
    public WordPackNotFoundException(String packName) {
        super("Word pack not found: " + packName);
    }
}
