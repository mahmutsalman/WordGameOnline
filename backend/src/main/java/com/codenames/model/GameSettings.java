package com.codenames.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing game configuration settings.
 * Controls word pack selection and timer settings.
 * JPA Embeddable - stored as part of Room entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class GameSettings {
    /**
     * Word pack to use for the game.
     * Defaults to "english" if not specified.
     */
    @Builder.Default
    @Column(nullable = false, length = 50)
    private String wordPack = "english";

    /**
     * Timer duration in seconds for each turn.
     * Null means no time limit.
     */
    @Builder.Default
    @Column
    private Integer timerSeconds = null;
}
