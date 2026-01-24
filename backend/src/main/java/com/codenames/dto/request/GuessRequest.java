package com.codenames.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Request DTO for making a guess.
 */
@Data
public class GuessRequest {
    /**
     * Index of the card on the board (0-24).
     */
    @Min(0)
    @Max(24)
    private int cardIndex;
}

