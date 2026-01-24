package com.codenames.service;

import com.codenames.factory.CardFactory;
import com.codenames.factory.GameStateFactory;
import com.codenames.model.Card;
import com.codenames.model.GameState;
import com.codenames.model.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for generating Codenames game boards.
 * Delegates to CardFactory and GameStateFactory for actual board and game creation.
 *
 * @deprecated Use {@link CardFactory} and {@link GameStateFactory} directly.
 */
@Deprecated
@Service
@RequiredArgsConstructor
@Slf4j
public class BoardGenerationService {

    private final GameStateFactory gameStateFactory;
    private final CardFactory cardFactory;

    /**
     * Generates a 25-card board with the proper color distribution.
     * Delegates to {@link CardFactory#createBoard(List, Team)}.
     *
     * @param words the 25 words to use for the board
     * @param startingTeam the team that goes first
     * @return list of 25 cards with assigned colors and words
     * @throws IllegalArgumentException if fewer than 25 words are provided
     * @deprecated Use {@link CardFactory#createBoard(List, Team)} directly.
     */
    @Deprecated
    public List<Card> generateBoard(List<String> words, Team startingTeam) {
        log.debug("Delegating generateBoard to CardFactory");
        return cardFactory.createBoard(words, startingTeam);
    }

    /**
     * Initializes a complete game state with a generated board.
     * Delegates to {@link GameStateFactory#create(String, Team)}.
     *
     * @param wordPack the word pack to use
     * @param startingTeam the team that goes first
     * @return initialized game state ready for play
     * @deprecated Use {@link GameStateFactory#create(String, Team)} directly.
     */
    @Deprecated
    public GameState initializeGame(String wordPack, Team startingTeam) {
        log.debug("Delegating initializeGame to GameStateFactory");
        return gameStateFactory.create(wordPack, startingTeam);
    }
}
