package com.codenames.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for GameState model.
 * Tests game state creation, helper methods, and state management.
 */
class GameStateTest {

    /**
     * Test that GameState can be created using Lombok Builder.
     */
    @Test
    void shouldCreateGameStateWithBuilder() {
        // Arrange & Act
        GameState state = GameState.builder()
                .currentTeam(Team.BLUE)
                .phase(GamePhase.LOBBY)
                .build();

        // Assert
        assertThat(state).isNotNull();
        assertThat(state.getCurrentTeam()).isEqualTo(Team.BLUE);
        assertThat(state.getPhase()).isEqualTo(GamePhase.LOBBY);
    }

    /**
     * Test that all GameState fields can be set and retrieved.
     */
    @Test
    void shouldSetAllFields() {
        // Arrange
        List<Card> board = createSampleBoard();
        Clue clue = Clue.builder().word("TEST").number(2).team(Team.BLUE).build();
        List<TurnHistory> history = new ArrayList<>();

        // Act
        GameState state = GameState.builder()
                .board(board)
                .currentTeam(Team.BLUE)
                .phase(GamePhase.GUESS)
                .currentClue(clue)
                .guessesRemaining(3)
                .blueRemaining(9)
                .redRemaining(8)
                .winner(null)
                .history(history)
                .build();

        // Assert
        assertThat(state.getBoard()).hasSize(25);
        assertThat(state.getCurrentTeam()).isEqualTo(Team.BLUE);
        assertThat(state.getPhase()).isEqualTo(GamePhase.GUESS);
        assertThat(state.getCurrentClue()).isEqualTo(clue);
        assertThat(state.getGuessesRemaining()).isEqualTo(3);
        assertThat(state.getBlueRemaining()).isEqualTo(9);
        assertThat(state.getRedRemaining()).isEqualTo(8);
        assertThat(state.getWinner()).isNull();
        assertThat(state.getHistory()).isEmpty();
    }

    /**
     * Test that getCard returns the correct card at index.
     */
    @Test
    void shouldGetCardAtIndex() {
        // Arrange
        List<Card> board = createSampleBoard();
        GameState state = GameState.builder()
                .board(board)
                .build();

        // Act
        Card card0 = state.getCard(0);
        Card card5 = state.getCard(5);
        Card card24 = state.getCard(24);

        // Assert
        assertThat(card0.getWord()).isEqualTo("WORD_0");
        assertThat(card5.getWord()).isEqualTo("WORD_5");
        assertThat(card24.getWord()).isEqualTo("WORD_24");
    }

    /**
     * Test that getCard throws exception for invalid index.
     */
    @Test
    void shouldThrowExceptionForInvalidCardIndex() {
        // Arrange
        List<Card> board = createSampleBoard();
        GameState state = GameState.builder()
                .board(board)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> state.getCard(-1))
                .as("Should throw exception for negative index")
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> state.getCard(25))
                .as("Should throw exception for index >= board size")
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    /**
     * Test that revealCard marks the card as revealed.
     */
    @Test
    void shouldRevealCard() {
        // Arrange
        List<Card> board = createSampleBoard();
        GameState state = GameState.builder()
                .board(board)
                .build();

        // Act
        state.revealCard(10);

        // Assert
        assertThat(state.getCard(10).isRevealed())
                .as("Card at index 10 should be revealed")
                .isTrue();
    }

    /**
     * Test that revealCard does not affect other cards.
     */
    @Test
    void shouldOnlyRevealSpecifiedCard() {
        // Arrange
        List<Card> board = createSampleBoard();
        GameState state = GameState.builder()
                .board(board)
                .build();

        // Act
        state.revealCard(5);

        // Assert
        assertThat(state.getCard(5).isRevealed()).isTrue();
        assertThat(state.getCard(4).isRevealed()).isFalse();
        assertThat(state.getCard(6).isRevealed()).isFalse();
    }

    /**
     * Test that revealCard throws exception for invalid index.
     */
    @Test
    void shouldThrowExceptionForInvalidRevealIndex() {
        // Arrange
        List<Card> board = createSampleBoard();
        GameState state = GameState.builder()
                .board(board)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> state.revealCard(-1))
                .as("Should throw exception for negative index")
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> state.revealCard(25))
                .as("Should throw exception for index >= board size")
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    /**
     * Test that GameState supports LOBBY phase.
     */
    @Test
    void shouldSupportLobbyPhase() {
        // Arrange & Act
        GameState state = GameState.builder()
                .phase(GamePhase.LOBBY)
                .build();

        // Assert
        assertThat(state.getPhase()).isEqualTo(GamePhase.LOBBY);
    }

    /**
     * Test that GameState supports CLUE phase.
     */
    @Test
    void shouldSupportCluePhase() {
        // Arrange & Act
        GameState state = GameState.builder()
                .phase(GamePhase.CLUE)
                .build();

        // Assert
        assertThat(state.getPhase()).isEqualTo(GamePhase.CLUE);
    }

    /**
     * Test that GameState supports GUESS phase.
     */
    @Test
    void shouldSupportGuessPhase() {
        // Arrange & Act
        GameState state = GameState.builder()
                .phase(GamePhase.GUESS)
                .build();

        // Assert
        assertThat(state.getPhase()).isEqualTo(GamePhase.GUESS);
    }

    /**
     * Test that GameState supports GAME_OVER phase.
     */
    @Test
    void shouldSupportGameOverPhase() {
        // Arrange & Act
        GameState state = GameState.builder()
                .phase(GamePhase.GAME_OVER)
                .winner(Team.BLUE)
                .build();

        // Assert
        assertThat(state.getPhase()).isEqualTo(GamePhase.GAME_OVER);
        assertThat(state.getWinner()).isEqualTo(Team.BLUE);
    }

    /**
     * Test that history can track multiple turns.
     */
    @Test
    void shouldTrackMultipleTurns() {
        // Arrange
        TurnHistory turn1 = TurnHistory.builder()
                .team(Team.BLUE)
                .clue(Clue.builder().word("FIRST").number(1).team(Team.BLUE).build())
                .build();
        TurnHistory turn2 = TurnHistory.builder()
                .team(Team.RED)
                .clue(Clue.builder().word("SECOND").number(2).team(Team.RED).build())
                .build();

        // Act
        GameState state = GameState.builder()
                .history(new ArrayList<>(List.of(turn1, turn2)))
                .build();

        // Assert
        assertThat(state.getHistory())
                .as("History should track multiple turns")
                .hasSize(2);
    }

    /**
     * Test that Lombok getters and setters work correctly.
     */
    @Test
    void shouldSupportLombokGettersSetters() {
        // Arrange
        GameState state = new GameState();
        List<Card> board = createSampleBoard();

        // Act
        state.setBoard(board);
        state.setCurrentTeam(Team.RED);
        state.setPhase(GamePhase.CLUE);
        state.setGuessesRemaining(0);
        state.setBlueRemaining(9);
        state.setRedRemaining(8);

        // Assert
        assertThat(state.getBoard()).hasSize(25);
        assertThat(state.getCurrentTeam()).isEqualTo(Team.RED);
        assertThat(state.getPhase()).isEqualTo(GamePhase.CLUE);
        assertThat(state.getGuessesRemaining()).isZero();
        assertThat(state.getBlueRemaining()).isEqualTo(9);
        assertThat(state.getRedRemaining()).isEqualTo(8);
    }

    /**
     * Test that winner can be set when game ends.
     */
    @Test
    void shouldSetWinner() {
        // Arrange
        GameState state = GameState.builder()
                .phase(GamePhase.GAME_OVER)
                .build();

        // Act
        state.setWinner(Team.RED);

        // Assert
        assertThat(state.getWinner())
                .as("Winner should be settable")
                .isEqualTo(Team.RED);
    }

    /**
     * Test that currentTeam can switch between teams.
     */
    @Test
    void shouldSwitchCurrentTeam() {
        // Arrange
        GameState state = GameState.builder()
                .currentTeam(Team.BLUE)
                .build();

        // Act
        state.setCurrentTeam(Team.RED);

        // Assert
        assertThat(state.getCurrentTeam())
                .as("Current team should be switchable")
                .isEqualTo(Team.RED);
    }

    /**
     * Helper method to create a sample 25-card board.
     */
    private List<Card> createSampleBoard() {
        List<Card> board = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            CardColor color;
            if (i < 9) {
                color = CardColor.BLUE;
            } else if (i < 17) {
                color = CardColor.RED;
            } else if (i < 24) {
                color = CardColor.NEUTRAL;
            } else {
                color = CardColor.ASSASSIN;
            }
            board.add(Card.builder()
                    .word("WORD_" + i)
                    .color(color)
                    .revealed(false)
                    .build());
        }
        return board;
    }
}
