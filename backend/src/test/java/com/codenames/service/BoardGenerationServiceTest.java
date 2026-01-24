package com.codenames.service;

import com.codenames.model.Card;
import com.codenames.model.CardColor;
import com.codenames.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BoardGenerationService.
 * Tests board generation with proper color distribution.
 */
@DisplayName("BoardGenerationService")
@ExtendWith(MockitoExtension.class)
class BoardGenerationServiceTest {

    @Mock
    private WordPackService wordPackService;

    private BoardGenerationService boardGenerationService;

    private List<String> sampleWords;

    @BeforeEach
    void setUp() {
        boardGenerationService = new BoardGenerationService(wordPackService);

        // Generate 25 sample words
        sampleWords = IntStream.range(1, 26)
                .mapToObj(i -> "WORD" + i)
                .collect(Collectors.toList());
    }

    @Nested
    @DisplayName("generateBoard")
    class GenerateBoard {

        @Test
        @DisplayName("should generate exactly 25 cards")
        void shouldGenerateExactly25Cards() {
            List<Card> board = boardGenerationService.generateBoard(sampleWords, Team.BLUE);

            assertThat(board).hasSize(25);
        }

        @Test
        @DisplayName("should have 9 blue cards when blue starts")
        void shouldHave9BlueCardsWhenBlueStarts() {
            List<Card> board = boardGenerationService.generateBoard(sampleWords, Team.BLUE);

            long blueCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.BLUE)
                    .count();

            assertThat(blueCount).isEqualTo(9);
        }

        @Test
        @DisplayName("should have 8 red cards when blue starts")
        void shouldHave8RedCardsWhenBlueStarts() {
            List<Card> board = boardGenerationService.generateBoard(sampleWords, Team.BLUE);

            long redCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.RED)
                    .count();

            assertThat(redCount).isEqualTo(8);
        }

        @Test
        @DisplayName("should have 9 red cards when red starts")
        void shouldHave9RedCardsWhenRedStarts() {
            List<Card> board = boardGenerationService.generateBoard(sampleWords, Team.RED);

            long redCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.RED)
                    .count();

            assertThat(redCount).isEqualTo(9);
        }

        @Test
        @DisplayName("should have 8 blue cards when red starts")
        void shouldHave8BlueCardsWhenRedStarts() {
            List<Card> board = boardGenerationService.generateBoard(sampleWords, Team.RED);

            long blueCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.BLUE)
                    .count();

            assertThat(blueCount).isEqualTo(8);
        }

        @Test
        @DisplayName("should have 7 neutral cards")
        void shouldHave7NeutralCards() {
            List<Card> board = boardGenerationService.generateBoard(sampleWords, Team.BLUE);

            long neutralCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.NEUTRAL)
                    .count();

            assertThat(neutralCount).isEqualTo(7);
        }

        @Test
        @DisplayName("should have exactly 1 assassin card")
        void shouldHaveExactly1AssassinCard() {
            List<Card> board = boardGenerationService.generateBoard(sampleWords, Team.BLUE);

            long assassinCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.ASSASSIN)
                    .count();

            assertThat(assassinCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should have all cards unrevealed")
        void shouldHaveAllCardsUnrevealed() {
            List<Card> board = boardGenerationService.generateBoard(sampleWords, Team.BLUE);

            assertThat(board).allMatch(card -> !card.isRevealed());
        }

        @Test
        @DisplayName("should use all provided words")
        void shouldUseAllProvidedWords() {
            List<Card> board = boardGenerationService.generateBoard(sampleWords, Team.BLUE);

            List<String> boardWords = board.stream()
                    .map(Card::getWord)
                    .collect(Collectors.toList());

            assertThat(boardWords).containsExactlyInAnyOrderElementsOf(sampleWords);
        }

        @Test
        @DisplayName("should throw exception when not enough words provided")
        void shouldThrowExceptionWhenNotEnoughWords() {
            List<String> fewWords = Arrays.asList("WORD1", "WORD2", "WORD3");

            assertThatThrownBy(() -> boardGenerationService.generateBoard(fewWords, Team.BLUE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("25 words");
        }

        @Test
        @DisplayName("should produce different card arrangements on subsequent calls")
        void shouldProduceDifferentArrangements() {
            // Run multiple times to verify shuffling
            List<Card> board1 = boardGenerationService.generateBoard(sampleWords, Team.BLUE);
            List<Card> board2 = boardGenerationService.generateBoard(sampleWords, Team.BLUE);

            // Extract color patterns
            List<CardColor> colors1 = board1.stream().map(Card::getColor).collect(Collectors.toList());
            List<CardColor> colors2 = board2.stream().map(Card::getColor).collect(Collectors.toList());

            // Very unlikely to be identical
            assertThat(colors1).isNotEqualTo(colors2);
        }
    }

    @Nested
    @DisplayName("initializeGame")
    class InitializeGame {

        @Test
        @DisplayName("should initialize game with word pack")
        void shouldInitializeGameWithWordPack() {
            when(wordPackService.getRandomWords("english", 25)).thenReturn(sampleWords);

            var gameState = boardGenerationService.initializeGame("english", Team.BLUE);

            assertThat(gameState).isNotNull();
            assertThat(gameState.getBoard()).hasSize(25);
        }

        @Test
        @DisplayName("should set correct starting team")
        void shouldSetCorrectStartingTeam() {
            when(wordPackService.getRandomWords("english", 25)).thenReturn(sampleWords);

            var gameState = boardGenerationService.initializeGame("english", Team.RED);

            assertThat(gameState.getCurrentTeam()).isEqualTo(Team.RED);
        }

        @Test
        @DisplayName("should set blue remaining count correctly when blue starts")
        void shouldSetBlueRemainingWhenBlueStarts() {
            when(wordPackService.getRandomWords("english", 25)).thenReturn(sampleWords);

            var gameState = boardGenerationService.initializeGame("english", Team.BLUE);

            assertThat(gameState.getBlueRemaining()).isEqualTo(9);
            assertThat(gameState.getRedRemaining()).isEqualTo(8);
        }

        @Test
        @DisplayName("should set red remaining count correctly when red starts")
        void shouldSetRedRemainingWhenRedStarts() {
            when(wordPackService.getRandomWords("english", 25)).thenReturn(sampleWords);

            var gameState = boardGenerationService.initializeGame("english", Team.RED);

            assertThat(gameState.getRedRemaining()).isEqualTo(9);
            assertThat(gameState.getBlueRemaining()).isEqualTo(8);
        }

        @Test
        @DisplayName("should set phase to CLUE")
        void shouldSetPhaseToClue() {
            when(wordPackService.getRandomWords("english", 25)).thenReturn(sampleWords);

            var gameState = boardGenerationService.initializeGame("english", Team.BLUE);

            assertThat(gameState.getPhase()).isEqualTo(com.codenames.model.GamePhase.CLUE);
        }

        @Test
        @DisplayName("should initialize empty history")
        void shouldInitializeEmptyHistory() {
            when(wordPackService.getRandomWords("english", 25)).thenReturn(sampleWords);

            var gameState = boardGenerationService.initializeGame("english", Team.BLUE);

            assertThat(gameState.getHistory()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should have no current clue")
        void shouldHaveNoCurrentClue() {
            when(wordPackService.getRandomWords("english", 25)).thenReturn(sampleWords);

            var gameState = boardGenerationService.initializeGame("english", Team.BLUE);

            assertThat(gameState.getCurrentClue()).isNull();
        }

        @Test
        @DisplayName("should have no winner")
        void shouldHaveNoWinner() {
            when(wordPackService.getRandomWords("english", 25)).thenReturn(sampleWords);

            var gameState = boardGenerationService.initializeGame("english", Team.BLUE);

            assertThat(gameState.getWinner()).isNull();
        }
    }
}
