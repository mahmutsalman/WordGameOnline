package com.codenames.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing game configuration settings.
 * Controls word pack selection and timer settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSettings {
    /**
     * Word pack to use for the game.
     * Defaults to "english" if not specified.
     */
    @Builder.Default
    private String wordPack = "english";

    /**
     * Timer duration in seconds for each turn.
     * Null means no time limit.
     */
    @Builder.Default
    private Integer timerSeconds = null;
}
