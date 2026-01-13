package com.codenames.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GameSettings model.
 * Tests default values and custom settings for game configuration.
 */
class GameSettingsTest {

    /**
     * Test that GameSettings can be created with Builder defaults.
     */
    @Test
    void shouldCreateWithDefaults() {
        // Arrange & Act
        GameSettings settings = GameSettings.builder().build();

        // Assert
        assertThat(settings).isNotNull();
        assertThat(settings.getWordPack()).isNotNull();
        assertThat(settings.getTimerSeconds()).isNull();
    }

    /**
     * Test that GameSettings defaults to "english" word pack.
     */
    @Test
    void shouldDefaultToEnglishPack() {
        // Arrange & Act
        GameSettings settings = GameSettings.builder().build();

        // Assert
        assertThat(settings.getWordPack())
                .as("Default word pack should be 'english'")
                .isEqualTo("english");
    }

    /**
     * Test that GameSettings defaults to null timer (no time limit).
     */
    @Test
    void shouldDefaultToNullTimer() {
        // Arrange & Act
        GameSettings settings = GameSettings.builder().build();

        // Assert
        assertThat(settings.getTimerSeconds())
                .as("Default timer should be null (no time limit)")
                .isNull();
    }

    /**
     * Test that GameSettings allows custom word pack.
     */
    @Test
    void shouldAllowCustomWordPack() {
        // Arrange & Act
        GameSettings settings = GameSettings.builder()
                .wordPack("spanish")
                .build();

        // Assert
        assertThat(settings.getWordPack())
                .as("Should allow custom word pack")
                .isEqualTo("spanish");
    }

    /**
     * Test that GameSettings allows custom timer setting.
     */
    @Test
    void shouldAllowCustomTimer() {
        // Arrange & Act
        GameSettings settings = GameSettings.builder()
                .timerSeconds(120)
                .build();

        // Assert
        assertThat(settings.getTimerSeconds())
                .as("Should allow custom timer value")
                .isEqualTo(120);
    }
}
