package com.codenames.service;

import com.codenames.factory.CardFactory;
import com.codenames.factory.GameStateFactory;
import com.codenames.model.Card;
import com.codenames.model.CardColor;
import com.codenames.model.GamePhase;
import com.codenames.model.GameState;
import com.codenames.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BoardGenerationService.
 * Tests that it properly delegates to CardFactory and GameStateFactory.
 */
@DisplayName("BoardGenerationService")
@ExtendWith(MockitoExtension.class)
class BoardGenerationServiceTest {

    @Mock
    private GameStateFactory gameStateFactory;

    @Mock
    private CardFactory cardFactory;

    private BoardGenerationService boardGenerationService;

    private List<String> sampleWords;

    @BeforeEach
    void setUp() {
        boardGenerationService = new BoardGenerationService(gameStateFactory, cardFactory);

        // Generate 25 sample words
        sampleWords = IntStream.range(1, 26)
                .mapToObj(i -> "WORD" + i)
                .collect(Collectors.toList());
    }

    @Nested
    @DisplayName("generateBoard")
    class GenerateBoard {

        @Test
        @DisplayName("should delegate to CardFactory")
        void shouldDelegateToCardFactory() {
            List<Card> expectedBoard = createMockBoard(Team.BLUE);
            when(cardFactory.createBoard(sampleWords, Team.BLUE)).thenReturn(expectedBoard);

            List<Card> board = boardGenerationService.generateBoard(sampleWords, Team.BLUE);

            assertThat(board).isEqualTo(expectedBoard);
            verify(cardFactory).createBoard(sampleWords, Team.BLUE);
        }

        @Test
        @DisplayName("should pass correct team to CardFactory")
        void shouldPassCorrectTeamToCardFactory() {
            List<Card> expectedBoard = createMockBoard(Team.RED);
            when(cardFactory.createBoard(sampleWords, Team.RED)).thenReturn(expectedBoard);

            boardGenerationService.generateBoard(sampleWords, Team.RED);

            verify(cardFactory).createBoard(sampleWords, Team.RED);
        }
    }

    @Nested
    @DisplayName("initializeGame")
    class InitializeGame {

        @Test
        @DisplayName("should delegate to GameStateFactory")
        void shouldDelegateToGameStateFactory() {
            GameState expectedState = createMockGameState(Team.BLUE);
            when(gameStateFactory.create("english", Team.BLUE)).thenReturn(expectedState);

            GameState gameState = boardGenerationService.initializeGame("english", Team.BLUE);

            assertThat(gameState).isEqualTo(expectedState);
            verify(gameStateFactory).create("english", Team.BLUE);
        }

        @Test
        @DisplayName("should pass correct word pack to GameStateFactory")
        void shouldPassCorrectWordPackToGameStateFactory() {
            GameState expectedState = createMockGameState(Team.BLUE);
            when(gameStateFactory.create("custom-pack", Team.BLUE)).thenReturn(expectedState);

            boardGenerationService.initializeGame("custom-pack", Team.BLUE);

            verify(gameStateFactory).create("custom-pack", Team.BLUE);
        }

        @Test
        @DisplayName("should pass correct team to GameStateFactory")
        void shouldPassCorrectTeamToGameStateFactory() {
            GameState expectedState = createMockGameState(Team.RED);
            when(gameStateFactory.create("english", Team.RED)).thenReturn(expectedState);

            boardGenerationService.initializeGame("english", Team.RED);

            verify(gameStateFactory).create("english", Team.RED);
        }
    }

    private List<Card> createMockBoard(Team startingTeam) {
        List<Card> board = new ArrayList<>();
        int blueCount = startingTeam == Team.BLUE ? 9 : 8;
        int redCount = startingTeam == Team.RED ? 9 : 8;

        for (int i = 0; i < blueCount; i++) {
            board.add(Card.builder()
                    .word("WORD" + (i + 1))
                    .color(CardColor.BLUE)
                    .revealed(false)
                    .build());
        }

        for (int i = 0; i < redCount; i++) {
            board.add(Card.builder()
                    .word("WORD" + (blueCount + i + 1))
                    .color(CardColor.RED)
                    .revealed(false)
                    .build());
        }

        for (int i = 0; i < 7; i++) {
            board.add(Card.builder()
                    .word("WORD" + (blueCount + redCount + i + 1))
                    .color(CardColor.NEUTRAL)
                    .revealed(false)
                    .build());
        }

        board.add(Card.builder()
                .word("WORD25")
                .color(CardColor.ASSASSIN)
                .revealed(false)
                .build());

        return board;
    }

    private GameState createMockGameState(Team startingTeam) {
        return GameState.builder()
                .board(createMockBoard(startingTeam))
                .currentTeam(startingTeam)
                .phase(GamePhase.CLUE)
                .blueRemaining(startingTeam == Team.BLUE ? 9 : 8)
                .redRemaining(startingTeam == Team.RED ? 9 : 8)
                .guessesRemaining(0)
                .history(new ArrayList<>())
                .build();
    }
}
