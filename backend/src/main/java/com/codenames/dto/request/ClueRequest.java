package com.codenames.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for submitting a clue.
 */
@Data
public class ClueRequest {
    /**
     * The clue word (single word).
     */
    @NotBlank
    @Size(max = 30)
    private String word;

    /**
     * The clue number (0-9).
     */
    @Min(0)
    @Max(9)
    private int number;
}

