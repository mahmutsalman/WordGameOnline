package com.codenames.dto.response;

import com.codenames.model.CardColor;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for card data in API responses.
 * Color can be hidden for operatives (non-spymasters).
 */
@Data
@Builder
public class CardResponse {
    /**
     * The word displayed on the card.
     */
    private String word;

    /**
     * The color of the card (may be null for hidden cards).
     */
    private CardColor color;

    /**
     * Whether the card has been revealed.
     */
    private boolean revealed;

    /**
     * The ID of the player who selected this card.
     */
    private String selectedBy;
}
