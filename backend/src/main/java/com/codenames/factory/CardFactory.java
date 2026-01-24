package com.codenames.factory;

import com.codenames.model.Card;
import com.codenames.model.CardColor;
import com.codenames.model.Team;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory for creating Codenames game cards and boards.
 * Creates a 25-card board with proper color distribution based on the starting team.
 */
@Component
@Slf4j
public class CardFactory {

    private static final int BOARD_SIZE = 25;
    private static final int NEUTRAL_COUNT = 7;
    private static final int ASSASSIN_COUNT = 1;
    private static final int STARTING_TEAM_CARDS = 9;
    private static final int OTHER_TEAM_CARDS = 8;

    private final SecureRandom random = new SecureRandom();

    /**
     * Creates a 25-card board with proper color distribution.
     * Starting team gets 9 cards, other team gets 8, plus 7 neutral and 1 assassin.
     *
     * @param words the 25 words to use for the board
     * @param startingTeam the team that goes first
     * @return list of 25 cards with assigned colors and words
     * @throws IllegalArgumentException if fewer than 25 words are provided
     */
    public List<Card> createBoard(List<String> words, Team startingTeam) {
        if (words.size() < BOARD_SIZE) {
            throw new IllegalArgumentException(
                    "Need " + BOARD_SIZE + " words for the board, but got " + words.size());
        }

        // Create color distribution
        List<CardColor> colors = createColorDistribution(startingTeam);

        // Shuffle colors to randomize the board using SecureRandom
        Collections.shuffle(colors, random);

        // Create cards by pairing words with colors
        List<Card> board = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            Card card = Card.builder()
                    .word(words.get(i).toUpperCase())
                    .color(colors.get(i))
                    .revealed(false)
                    .build();
            board.add(card);
        }

        log.debug("Created board with starting team: {}", startingTeam);
        return board;
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
