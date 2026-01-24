package com.codenames.service;

import com.codenames.model.Card;
import com.codenames.model.CardColor;
import com.codenames.model.GamePhase;
import com.codenames.model.GameState;
import com.codenames.model.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for generating Codenames game boards.
 * Creates a 25-card board with proper color distribution based on the starting team.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BoardGenerationService {

    private static final int BOARD_SIZE = 25;
    private static final int NEUTRAL_COUNT = 7;
    private static final int ASSASSIN_COUNT = 1;
    private static final int STARTING_TEAM_CARDS = 9;
    private static final int OTHER_TEAM_CARDS = 8;

    private final WordPackService wordPackService;

    /**
     * Generates a 25-card board with the proper color distribution.
     * Standard Codenames distribution:
     * - Starting team: 9 cards
     * - Other team: 8 cards
     * - Neutral: 7 cards
     * - Assassin: 1 card
     *
     * @param words the 25 words to use for the board
     * @param startingTeam the team that goes first
     * @return list of 25 cards with assigned colors and words
     * @throws IllegalArgumentException if fewer than 25 words are provided
     */
    public List<Card> generateBoard(List<String> words, Team startingTeam) {
        if (words.size() < BOARD_SIZE) {
            throw new IllegalArgumentException(
                    "Need exactly " + BOARD_SIZE + " words for the board, but got " + words.size());
        }

        // Create color distribution
        List<CardColor> colors = createColorDistribution(startingTeam);

        // Shuffle colors to randomize the board
        Collections.shuffle(colors);

        // Create cards by pairing words with colors
        List<Card> board = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            Card card = Card.builder()
                    .word(words.get(i))
                    .color(colors.get(i))
                    .revealed(false)
                    .build();
            board.add(card);
        }

        log.debug("Generated board with starting team: {}", startingTeam);
        return board;
    }

    /**
     * Initializes a complete game state with a generated board.
     *
     * @param wordPack the word pack to use
     * @param startingTeam the team that goes first
     * @return initialized game state ready for play
     */
    public GameState initializeGame(String wordPack, Team startingTeam) {
        List<String> words = wordPackService.getRandomWords(wordPack, BOARD_SIZE);
        List<Card> board = generateBoard(words, startingTeam);

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

        log.info("Initialized game with word pack: {}, starting team: {}", wordPack, startingTeam);
        return gameState;
    }

    /**
     * Creates the color distribution for the board based on the starting team.
     *
     * @param startingTeam the team that goes first (gets 9 cards)
     * @return list of 25 colors in order (not shuffled)
     */
    private List<CardColor> createColorDistribution(Team startingTeam) {
        List<CardColor> colors = new ArrayList<>();

        // Starting team gets 9 cards
        CardColor startingColor = startingTeam == Team.BLUE ? CardColor.BLUE : CardColor.RED;
        CardColor otherColor = startingTeam == Team.BLUE ? CardColor.RED : CardColor.BLUE;

        // Add cards for starting team (9)
        for (int i = 0; i < STARTING_TEAM_CARDS; i++) {
            colors.add(startingColor);
        }

        // Add cards for other team (8)
        for (int i = 0; i < OTHER_TEAM_CARDS; i++) {
            colors.add(otherColor);
        }

        // Add neutral cards (7)
        for (int i = 0; i < NEUTRAL_COUNT; i++) {
            colors.add(CardColor.NEUTRAL);
        }

        // Add assassin (1)
        for (int i = 0; i < ASSASSIN_COUNT; i++) {
            colors.add(CardColor.ASSASSIN);
        }

        return colors;
    }
}
