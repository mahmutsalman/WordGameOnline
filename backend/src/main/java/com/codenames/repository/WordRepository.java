package com.codenames.repository;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repository for managing word packs used in the game.
 * Implements Singleton pattern via Spring's @Repository annotation.
 * Words are loaded once at application startup and cached in memory.
 * Follows Single Responsibility Principle by handling only word pack management.
 */
@Repository
public class WordRepository {

    private static final Logger logger = LoggerFactory.getLogger(WordRepository.class);
    private static final String DEFAULT_PACK = "english";

    // In-memory cache of word packs (Singleton pattern)
    private final Map<String, List<String>> wordPacks = new HashMap<>();

    /**
     * Load word packs from classpath resources on application startup.
     * Called after dependency injection is complete.
     * Words are stored in /resources/wordpacks/ directory.
     */
    @PostConstruct
    public void loadWordPacks() {
        logger.info("Loading word packs...");

        // Load default English pack
        List<String> englishWords = loadWordsFromFile("wordpacks/english.txt");
        wordPacks.put(DEFAULT_PACK, englishWords);

        logger.info("Loaded {} word pack(s)", wordPacks.size());
        logger.info("English pack contains {} words", englishWords.size());
    }

    /**
     * Load words from a resource file.
     * Each line in the file should contain one word (uppercase).
     *
     * @param path classpath resource path
     * @return list of words (unmodifiable)
     */
    private List<String> loadWordsFromFile(String path) {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(path)) {

            if (inputStream == null) {
                String errorMsg = "Word pack not found: " + path;
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            List<String> words = new BufferedReader(
                    new InputStreamReader(inputStream))
                    .lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("#"))  // Skip comments
                    .collect(Collectors.toCollection(ArrayList::new));

            logger.debug("Loaded {} words from {}", words.size(), path);

            return Collections.unmodifiableList(words);

        } catch (IOException e) {
            String errorMsg = "Failed to load word pack: " + path;
            logger.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * Get a specific word pack by name.
     * Returns the default pack if the requested pack doesn't exist.
     *
     * @param packName name of the word pack
     * @return unmodifiable list of words
     */
    public List<String> getWordPack(String packName) {
        List<String> pack = wordPacks.get(packName);
        if (pack == null) {
            logger.warn("Word pack '{}' not found, using default", packName);
            return wordPacks.get(DEFAULT_PACK);
        }
        return pack;
    }

    /**
     * Get the names of all available word packs.
     *
     * @return set of word pack names
     */
    public Set<String> getAvailablePacks() {
        return Collections.unmodifiableSet(wordPacks.keySet());
    }

    /**
     * Get the default word pack name.
     *
     * @return default pack name
     */
    public String getDefaultPackName() {
        return DEFAULT_PACK;
    }
}
