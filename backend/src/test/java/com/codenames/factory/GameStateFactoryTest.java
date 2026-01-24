package com.codenames.factory;

import com.codenames.model.Card;
import com.codenames.model.CardColor;
import com.codenames.model.GamePhase;
import com.codenames.model.GameState;
import com.codenames.model.Team;
import com.codenames.service.WordPackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GameStateFactory.
 * Tests game state creation with proper initialization.
 */
@DisplayName("GameStateFactory")
@ExtendWith(MockitoExtension.class)
class GameStateFactoryTest {

    @Mock
    private WordPackService wordPackService;

    @Mock
    private CardFactory cardFactory;

    private GameStateFactory gameStateFactory;

    private List<String> mockWords;
    private List<Card> mockBoard;

    @BeforeEach
    void setUp() {
        gameStateFactory = new GameStateFactory(wordPackService, cardFactory);

        mockWords = IntStream.rangeClosed(1, 25)
                .mapToObj(i -> "WORD" + i)
                .collect(Collectors.toList());
    }

    private List<Card> createMockBoard(Team startingTeam) {
        List<Card> board = new ArrayList<>();
        int blueCount = startingTeam == Team.BLUE ? 9 : 8;
        int redCount = startingTeam == Team.RED ? 9 : 8;

        // Add blue cards
        for (int i = 0; i < blueCount; i++) {
            board.add(Card.builder()
                    .word("WORD" + (i + 1))
                    .color(CardColor.BLUE)
                    .revealed(false)
                    .build());
        }

        // Add red cards
        for (int i = 0; i < redCount; i++) {
            board.add(Card.builder()
                    .word("WORD" + (blueCount + i + 1))
                    .color(CardColor.RED)
                    .revealed(false)
                    .build());
        }

        // Add neutral cards (7)
        for (int i = 0; i < 7; i++) {
            board.add(Card.builder()
                    .word("WORD" + (blueCount + redCount + i + 1))
                    .color(CardColor.NEUTRAL)
                    .revealed(false)
                    .build());
        }

        // Add assassin (1)
        board.add(Card.builder()
                .word("WORD25")
                .color(CardColor.ASSASSIN)
                .revealed(false)
                .build());

        return board;
    }

    @Nested
    @DisplayName("create with starting team")
    class CreateWithStartingTeam {

        @Test
        @DisplayName("should create game state with board")
        void shouldCreateGameStateWithBoard() {
            List<Card> board = createMockBoard(Team.BLUE);
            when(wordPackService.getRandomWords("english", 25)).thenReturn(mockWords);
            when(cardFactory.createBoard(mockWords, Team.BLUE)).thenReturn(board);

            GameState gameState = gameStateFactory.create("english", Team.BLUE);

            assertThat(gameState).isNotNull();
            assertThat(gameState.getBoard()).hasSize(25);
        }

        @Test
        @DisplayName("should count remaining cards correctly when blue starts")
        void shouldCountRemainingCardsCorrectlyWhenBlueStarts() {
            List<Card> board = createMockBoard(Team.BLUE);
            when(wordPackService.getRandomWords("english", 25)).thenReturn(mockWords);
            when(cardFactory.createBoard(mockWords, Team.BLUE)).thenReturn(board);

            GameState gameState = gameStateFactory.create("english", Team.BLUE);

            assertThat(gameState.getBlueRemaining()).isEqualTo(9);
            assertThat(gameState.getRedRemaining()).isEqualTo(8);
        }

        @Test
        @DisplayName("should count remaining cards correctly when red starts")
        void shouldCountRemainingCardsCorrectlyWhenRedStarts() {
            List<Card> board = createMockBoard(Team.RED);
            when(wordPackService.getRandomWords("english", 25)).thenReturn(mockWords);
            when(cardFactory.createBoard(mockWords, Team.RED)).thenReturn(board);

            GameState gameState = gameStateFactory.create("english", Team.RED);

            assertThat(gameState.getRedRemaining()).isEqualTo(9);
            assertThat(gameState.getBlueRemaining()).isEqualTo(8);
        }

        @Test
        @DisplayName("should initialize with empty history")
        void shouldInitializeWithEmptyHistory() {
            List<Card> board = createMockBoard(Team.BLUE);
            when(wordPackService.getRandomWords("english", 25)).thenReturn(mockWords);
            when(cardFactory.createBoard(mockWords, Team.BLUE)).thenReturn(board);

            GameState gameState = gameStateFactory.create("english", Team.BLUE);

            assertThat(gameState.getHistory()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should initialize with zero guesses remaining")
        void shouldInitializeWithZeroGuessesRemaining() {
            List<Card> board = createMockBoard(Team.BLUE);
            when(wordPackService.getRandomWords("english", 25)).thenReturn(mockWords);
            when(cardFactory.createBoard(mockWords, Team.BLUE)).thenReturn(board);

            GameState gameState = gameStateFactory.create("english", Team.BLUE);

            assertThat(gameState.getGuessesRemaining()).isEqualTo(0);
        }

        @Test
        @DisplayName("should have no winner initially")
        void shouldHaveNoWinnerInitially() {
            List<Card> board = createMockBoard(Team.BLUE);
            when(wordPackService.getRandomWords("english", 25)).thenReturn(mockWords);
            when(cardFactory.createBoard(mockWords, Team.BLUE)).thenReturn(board);

            GameState gameState = gameStateFactory.create("english", Team.BLUE);

            assertThat(gameState.getWinner()).isNull();
        }

        @Test
        @DisplayName("should have no current clue initially")
        void shouldHaveNoCurrentClueInitially() {
            List<Card> board = createMockBoard(Team.BLUE);
            when(wordPackService.getRandomWords("english", 25)).thenReturn(mockWords);
            when(cardFactory.createBoard(mockWords, Team.BLUE)).thenReturn(board);

            GameState gameState = gameStateFactory.create("english", Team.BLUE);

            assertThat(gameState.getCurrentClue()).isNull();
        }

        @Test
        @DisplayName("should start in clue phase")
        void shouldStartInCluePhase() {
            List<Card> board = createMockBoard(Team.BLUE);
            when(wordPackService.getRandomWords("english", 25)).thenReturn(mockWords);
            when(cardFactory.createBoard(mockWords, Team.BLUE)).thenReturn(board);

            GameState gameState = gameStateFactory.create("english", Team.BLUE);

            assertThat(gameState.getPhase()).isEqualTo(GamePhase.CLUE);
        }

        @Test
        @DisplayName("should use correct word pack")
        void shouldUseCorrectWordPack() {
            List<Card> board = createMockBoard(Team.BLUE);
            when(wordPackService.getRandomWords("custom-pack", 25)).thenReturn(mockWords);
            when(cardFactory.createBoard(mockWords, Team.BLUE)).thenReturn(board);

            GameState gameState = gameStateFactory.create("custom-pack", Team.BLUE);

            assertThat(gameState).isNotNull();
            assertThat(gameState.getBoard()).hasSize(25);
        }

        @Test
        @DisplayName("should set current team to starting team")
        void shouldSetCurrentTeamToStartingTeam() {
            List<Card> board = createMockBoard(Team.RED);
            when(wordPackService.getRandomWords("english", 25)).thenReturn(mockWords);
            when(cardFactory.createBoard(mockWords, Team.RED)).thenReturn(board);

            GameState gameState = gameStateFactory.create("english", Team.RED);

            assertThat(gameState.getCurrentTeam()).isEqualTo(Team.RED);
        }
    }

    @Nested
    @DisplayName("create with random starting team")
    class CreateWithRandomStartingTeam {

        @RepeatedTest(10)
        @DisplayName("should randomly select starting team when not specified")
        void shouldRandomlySelectStartingTeamWhenNotSpecified() {
            // This test runs multiple times to verify randomness
            // Use lenient stubbing since only one team stub will be used per run
            lenient().when(wordPackService.getRandomWords(anyString(), anyInt())).thenReturn(mockWords);
            lenient().when(cardFactory.createBoard(mockWords, Team.BLUE)).thenReturn(createMockBoard(Team.BLUE));
            lenient().when(cardFactory.createBoard(mockWords, Team.RED)).thenReturn(createMockBoard(Team.RED));

            GameState gameState = gameStateFactory.create("english");

            // Should have either team as current team
            assertThat(gameState.getCurrentTeam()).isIn(Team.BLUE, Team.RED);
        }
    }
}
