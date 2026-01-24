package com.codenames.factory;

import com.codenames.model.Card;
import com.codenames.model.GamePhase;
import com.codenames.model.GameState;
import com.codenames.model.Team;
import com.codenames.service.WordPackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Factory for creating new Codenames game states.
 * Handles game initialization with proper board setup and initial values.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameStateFactory {

    private static final int BOARD_SIZE = 25;
    private static final int STARTING_TEAM_CARDS = 9;
    private static final int OTHER_TEAM_CARDS = 8;

    private final WordPackService wordPackService;
    private final CardFactory cardFactory;
    private final Random random = new SecureRandom();

    /**
     * Creates a new GameState with random starting team.
     *
     * @param wordPack the word pack to use for the game
     * @return initialized game state with randomly selected starting team
     */
    public GameState create(String wordPack) {
        Team startingTeam = random.nextBoolean() ? Team.BLUE : Team.RED;
        log.debug("Randomly selected starting team: {}", startingTeam);
        return create(wordPack, startingTeam);
    }

    /**
     * Creates a new GameState with specified starting team.
     *
     * @param wordPack the word pack to use for the game
     * @param startingTeam the team that goes first
     * @return initialized game state ready for play
     */
    public GameState create(String wordPack, Team startingTeam) {
        List<String> words = wordPackService.getRandomWords(wordPack, BOARD_SIZE);
        List<Card> board = cardFactory.createBoard(words, startingTeam);

        int blueCards = startingTeam == Team.BLUE ? STARTING_TEAM_CARDS : OTHER_TEAM_CARDS;
        int redCards = startingTeam == Team.RED ? STARTING_TEAM_CARDS : OTHER_TEAM_CARDS;

        GameState gameState = GameState.builder()
                .board(board)
                .currentTeam(startingTeam)
                .phase(GamePhase.CLUE)
                .blueRemaining(blueCards)
                .redRemaining(redCards)
                .guessesRemaining(0)
                .history(new ArrayList<>())
                .build();

        log.info("Created game state with word pack: {}, starting team: {}", wordPack, startingTeam);
        return gameState;
    }
}
