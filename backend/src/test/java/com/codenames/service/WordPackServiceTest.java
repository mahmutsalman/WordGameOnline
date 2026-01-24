package com.codenames.service;

import com.codenames.exception.WordPackNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for WordPackService.
 * Tests word pack loading and random word selection.
 */
@DisplayName("WordPackService")
class WordPackServiceTest {

    private WordPackService wordPackService;

    @BeforeEach
    void setUp() {
        wordPackService = new WordPackService();
    }

    @Nested
    @DisplayName("loadWordPack")
    class LoadWordPack {

        @Test
        @DisplayName("should load english word pack successfully")
        void shouldLoadEnglishWordPack() {
            List<String> words = wordPackService.loadWordPack("english");

            assertThat(words).isNotEmpty();
            assertThat(words.size()).isGreaterThanOrEqualTo(400);
        }

        @Test
        @DisplayName("should return words in uppercase")
        void shouldReturnWordsInUppercase() {
            List<String> words = wordPackService.loadWordPack("english");

            assertThat(words).allMatch(word -> word.equals(word.toUpperCase()));
        }

        @Test
        @DisplayName("should filter out comment lines")
        void shouldFilterOutCommentLines() {
            List<String> words = wordPackService.loadWordPack("english");

            assertThat(words).noneMatch(word -> word.startsWith("#"));
        }

        @Test
        @DisplayName("should filter out empty lines")
        void shouldFilterOutEmptyLines() {
            List<String> words = wordPackService.loadWordPack("english");

            assertThat(words).noneMatch(String::isBlank);
        }

        @Test
        @DisplayName("should throw exception for non-existent word pack")
        void shouldThrowExceptionForNonExistentWordPack() {
            assertThatThrownBy(() -> wordPackService.loadWordPack("nonexistent"))
                    .isInstanceOf(WordPackNotFoundException.class)
                    .hasMessageContaining("nonexistent");
        }

        @Test
        @DisplayName("should contain specific words from english pack")
        void shouldContainSpecificWords() {
            List<String> words = wordPackService.loadWordPack("english");

            assertThat(words).contains("AFRICA", "AGENT", "APPLE", "BANK", "CAR");
        }
    }

    @Nested
    @DisplayName("getRandomWords")
    class GetRandomWords {

        @Test
        @DisplayName("should return exactly the requested number of words")
        void shouldReturnExactCount() {
            List<String> words = wordPackService.getRandomWords("english", 25);

            assertThat(words).hasSize(25);
        }

        @Test
        @DisplayName("should return unique words")
        void shouldReturnUniqueWords() {
            List<String> words = wordPackService.getRandomWords("english", 25);

            Set<String> uniqueWords = new HashSet<>(words);
            assertThat(uniqueWords).hasSize(25);
        }

        @Test
        @DisplayName("should return different words on subsequent calls")
        void shouldReturnDifferentWords() {
            List<String> words1 = wordPackService.getRandomWords("english", 25);
            List<String> words2 = wordPackService.getRandomWords("english", 25);

            // Very unlikely to be identical (probability is negligible)
            assertThat(words1).isNotEqualTo(words2);
        }

        @Test
        @DisplayName("should throw exception when requesting more words than available")
        void shouldThrowExceptionWhenRequestingTooManyWords() {
            assertThatThrownBy(() -> wordPackService.getRandomWords("english", 10000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Not enough words");
        }

        @Test
        @DisplayName("should return words in uppercase")
        void shouldReturnUppercaseWords() {
            List<String> words = wordPackService.getRandomWords("english", 25);

            assertThat(words).allMatch(word -> word.equals(word.toUpperCase()));
        }

        @Test
        @DisplayName("should throw exception for non-existent word pack")
        void shouldThrowExceptionForNonExistentPack() {
            assertThatThrownBy(() -> wordPackService.getRandomWords("nonexistent", 25))
                    .isInstanceOf(WordPackNotFoundException.class);
        }
    }
}
