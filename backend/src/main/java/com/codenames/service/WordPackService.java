package com.codenames.service;

import com.codenames.exception.WordPackNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for loading and managing word packs for Codenames game.
 * Word packs are stored as text files in resources/wordpacks/ directory.
 */
@Service
@Slf4j
public class WordPackService {

    private static final String WORDPACK_PATH = "wordpacks/";

    /**
     * Loads all words from a word pack file.
     * Words are expected to be one per line, uppercase.
     * Lines starting with '#' are treated as comments and ignored.
     *
     * @param packName the name of the word pack (without .txt extension)
     * @return list of words from the word pack
     * @throws WordPackNotFoundException if the word pack file doesn't exist
     */
    public List<String> loadWordPack(String packName) {
        String resourcePath = WORDPACK_PATH + packName + ".txt";

        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.warn("Word pack not found: {}", packName);
                throw new WordPackNotFoundException(packName);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                List<String> words = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.startsWith("#"))
                        .map(String::toUpperCase)
                        .collect(Collectors.toList());

                log.debug("Loaded {} words from word pack: {}", words.size(), packName);
                return words;
            }
        } catch (IOException e) {
            log.error("Failed to load word pack: {}", packName, e);
            throw new WordPackNotFoundException(packName);
        }
    }

    /**
     * Gets a random selection of words from a word pack.
     * The words are shuffled and the first 'count' words are returned.
     *
     * @param packName the name of the word pack
     * @param count the number of random words to return
     * @return list of random words
     * @throws WordPackNotFoundException if the word pack doesn't exist
     * @throws IllegalArgumentException if count exceeds available words
     */
    public List<String> getRandomWords(String packName, int count) {
        List<String> allWords = loadWordPack(packName);

        if (count > allWords.size()) {
            throw new IllegalArgumentException(
                    "Not enough words in pack. Requested: " + count + ", Available: " + allWords.size());
        }

        // Create a mutable copy and shuffle
        List<String> shuffled = new ArrayList<>(allWords);
        Collections.shuffle(shuffled);

        return shuffled.subList(0, count);
    }
}
