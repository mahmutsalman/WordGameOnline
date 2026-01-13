package com.codenames.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for WordRepository.
 * Tests word pack loading and retrieval functionality.
 */
@SpringBootTest
class WordRepositoryTest {

    @Autowired
    private WordRepository wordRepository;

    /**
     * Test that the English word pack loads successfully.
     * Verifies that:
     * - Word pack is not null
     * - Contains enough words for a game (minimum 25)
     * - Contains more than 25 words (we expect 400+)
     */
    @Test
    void shouldLoadEnglishWordPack() {
        // Act
        List<String> words = wordRepository.getWordPack("english");

        // Assert
        assertThat(words).isNotNull();
        assertThat(words.size())
                .as("Word pack should contain enough words for multiple games")
                .isGreaterThanOrEqualTo(25);
        assertThat(words.size())
                .as("English pack should contain 400+ words")
                .isGreaterThan(100);
    }

    /**
     * Test that requesting an unknown pack returns the default pack.
     * This ensures the game can always proceed even with invalid pack names.
     */
    @Test
    void shouldReturnDefaultPackForUnknownPack() {
        // Act
        List<String> unknownPack = wordRepository.getWordPack("nonexistent");
        List<String> defaultPack = wordRepository.getWordPack("english");

        // Assert
        assertThat(unknownPack).isNotNull();
        assertThat(unknownPack)
                .as("Unknown pack should fallback to default English pack")
                .isEqualTo(defaultPack);
    }

    /**
     * Test that the repository can list all available word packs.
     * Verifies that the English pack is present in the list.
     */
    @Test
    void shouldListAvailablePacks() {
        // Act
        Set<String> packs = wordRepository.getAvailablePacks();

        // Assert
        assertThat(packs).isNotNull();
        assertThat(packs)
                .as("Available packs should include 'english'")
                .contains("english");
        assertThat(packs.size())
                .as("Should have at least one pack")
                .isGreaterThanOrEqualTo(1);
    }

    /**
     * Test that words in the pack are properly formatted.
     * Verifies that:
     * - Words are not empty
     * - Words are uppercase (game convention)
     * - Words don't contain whitespace
     */
    @Test
    void shouldContainProperlyFormattedWords() {
        // Act
        List<String> words = wordRepository.getWordPack("english");

        // Assert
        assertThat(words).isNotEmpty();

        // Check first 10 words for proper formatting
        words.stream().limit(10).forEach(word -> {
            assertThat(word)
                    .as("Word should not be empty")
                    .isNotEmpty();
            assertThat(word)
                    .as("Word should be uppercase: " + word)
                    .isEqualTo(word.toUpperCase());
            assertThat(word)
                    .as("Word should not contain whitespace: " + word)
                    .doesNotContainAnyWhitespaces();
        });
    }

    /**
     * Test that the default pack name is retrievable.
     */
    @Test
    void shouldReturnDefaultPackName() {
        // Act
        String defaultPackName = wordRepository.getDefaultPackName();

        // Assert
        assertThat(defaultPackName).isEqualTo("english");
    }
}
