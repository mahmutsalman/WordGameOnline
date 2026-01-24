package com.codenames.factory;

import com.codenames.model.Card;
import com.codenames.model.CardColor;
import com.codenames.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for CardFactory.
 * Tests card creation with proper color distribution.
 */
@DisplayName("CardFactory")
class CardFactoryTest {

    private CardFactory cardFactory;
    private List<String> sampleWords;

    @BeforeEach
    void setUp() {
        cardFactory = new CardFactory();
        sampleWords = IntStream.rangeClosed(1, 25)
                .mapToObj(i -> "word" + i)
                .collect(Collectors.toList());
    }

    @Nested
    @DisplayName("createBoard")
    class CreateBoard {

        @Test
        @DisplayName("should create exactly 25 cards")
        void shouldCreate25Cards() {
            List<Card> board = cardFactory.createBoard(sampleWords, Team.BLUE);

            assertThat(board).hasSize(25);
        }

        @Test
        @DisplayName("should have correct distribution when blue starts (9 blue, 8 red, 7 neutral, 1 assassin)")
        void shouldHaveCorrectDistributionWhenBlueStarts() {
            List<Card> board = cardFactory.createBoard(sampleWords, Team.BLUE);

            long blueCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.BLUE)
                    .count();
            long redCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.RED)
                    .count();
            long neutralCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.NEUTRAL)
                    .count();
            long assassinCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.ASSASSIN)
                    .count();

            assertThat(blueCount).isEqualTo(9);
            assertThat(redCount).isEqualTo(8);
            assertThat(neutralCount).isEqualTo(7);
            assertThat(assassinCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should have correct distribution when red starts (8 blue, 9 red, 7 neutral, 1 assassin)")
        void shouldHaveCorrectDistributionWhenRedStarts() {
            List<Card> board = cardFactory.createBoard(sampleWords, Team.RED);

            long blueCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.BLUE)
                    .count();
            long redCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.RED)
                    .count();
            long neutralCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.NEUTRAL)
                    .count();
            long assassinCount = board.stream()
                    .filter(card -> card.getColor() == CardColor.ASSASSIN)
                    .count();

            assertThat(blueCount).isEqualTo(8);
            assertThat(redCount).isEqualTo(9);
            assertThat(neutralCount).isEqualTo(7);
            assertThat(assassinCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should randomize card positions")
        void shouldRandomizeCardPositions() {
            // Run multiple times to verify shuffling
            List<Card> board1 = cardFactory.createBoard(sampleWords, Team.BLUE);
            List<Card> board2 = cardFactory.createBoard(sampleWords, Team.BLUE);

            // Extract color patterns
            List<CardColor> colors1 = board1.stream().map(Card::getColor).collect(Collectors.toList());
            List<CardColor> colors2 = board2.stream().map(Card::getColor).collect(Collectors.toList());

            // Very unlikely to be identical (1 in 25! combinations)
            assertThat(colors1).isNotEqualTo(colors2);
        }

        @Test
        @DisplayName("should throw when insufficient words provided")
        void shouldThrowWhenInsufficientWords() {
            List<String> fewWords = Arrays.asList("word1", "word2", "word3");

            assertThatThrownBy(() -> cardFactory.createBoard(fewWords, Team.BLUE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("25");
        }

        @Test
        @DisplayName("should not reveal cards initially")
        void shouldNotRevealCardsInitially() {
            List<Card> board = cardFactory.createBoard(sampleWords, Team.BLUE);

            assertThat(board).allMatch(card -> !card.isRevealed());
        }

        @Test
        @DisplayName("should not have selectedBy initially")
        void shouldNotHaveSelectedByInitially() {
            List<Card> board = cardFactory.createBoard(sampleWords, Team.BLUE);

            assertThat(board).allMatch(card -> card.getSelectedBy() == null);
        }

        @Test
        @DisplayName("should assign words to cards")
        void shouldAssignWordsToCards() {
            List<Card> board = cardFactory.createBoard(sampleWords, Team.BLUE);

            List<String> boardWords = board.stream()
                    .map(Card::getWord)
                    .collect(Collectors.toList());

            // All words should be present (as uppercase)
            List<String> upperCaseWords = sampleWords.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
            assertThat(boardWords).containsExactlyInAnyOrderElementsOf(upperCaseWords);
        }

        @Test
        @DisplayName("should convert words to uppercase")
        void shouldConvertWordsToUpperCase() {
            List<String> lowercaseWords = Arrays.asList(
                    "apple", "banana", "cherry", "date", "elderberry",
                    "fig", "grape", "honeydew", "imbe", "jackfruit",
                    "kiwi", "lemon", "mango", "nectarine", "orange",
                    "papaya", "quince", "raspberry", "strawberry", "tangerine",
                    "ugli", "vanilla", "watermelon", "ximenia", "yuzu"
            );

            List<Card> board = cardFactory.createBoard(lowercaseWords, Team.BLUE);

            assertThat(board).allMatch(card ->
                card.getWord().equals(card.getWord().toUpperCase()));
        }
    }
}
