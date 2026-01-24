package com.codenames.dto.response;

import com.codenames.model.Clue;
import com.codenames.model.GamePhase;
import com.codenames.model.Team;
import com.codenames.model.TurnHistory;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for game state in API responses.
 * Contains the full game board and current game status.
 */
@Data
@Builder
public class GameStateResponse {
    /**
     * The 25-card game board.
     */
    private List<CardResponse> board;

    /**
     * The team whose turn it currently is.
     */
    private Team currentTeam;

    /**
     * The current phase of the game.
     */
    private GamePhase phase;

    /**
     * The current clue given by the spymaster.
     */
    private Clue currentClue;

    /**
     * Number of guesses remaining for the current turn.
     */
    private int guessesRemaining;

    /**
     * Number of blue cards remaining to be revealed.
     */
    private int blueRemaining;

    /**
     * Number of red cards remaining to be revealed.
     */
    private int redRemaining;

    /**
     * The winning team, or null if game is still in progress.
     */
    private Team winner;

    /**
     * History of all turns played in this game.
     */
    private List<TurnHistory> history;
}
