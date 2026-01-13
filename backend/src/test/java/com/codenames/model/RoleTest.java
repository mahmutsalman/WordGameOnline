package com.codenames.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Role enum.
 * Tests role values for players in Codenames game.
 */
class RoleTest {

    /**
     * Test that the Role enum has exactly three roles.
     */
    @Test
    void shouldHaveThreeRoles() {
        // Arrange & Act
        Role[] roles = Role.values();

        // Assert
        assertThat(roles)
                .as("Role enum should have exactly 3 values")
                .hasSize(3);
    }

    /**
     * Test that the Role enum contains SPYMASTER.
     */
    @Test
    void shouldContainSpymaster() {
        // Arrange & Act
        Role[] roles = Role.values();

        // Assert
        assertThat(roles)
                .as("Role enum should contain SPYMASTER")
                .contains(Role.SPYMASTER);
    }

    /**
     * Test that the Role enum contains OPERATIVE.
     */
    @Test
    void shouldContainOperative() {
        // Arrange & Act
        Role[] roles = Role.values();

        // Assert
        assertThat(roles)
                .as("Role enum should contain OPERATIVE")
                .contains(Role.OPERATIVE);
    }

    /**
     * Test that the Role enum contains SPECTATOR.
     */
    @Test
    void shouldContainSpectator() {
        // Arrange & Act
        Role[] roles = Role.values();

        // Assert
        assertThat(roles)
                .as("Role enum should contain SPECTATOR")
                .contains(Role.SPECTATOR);
    }
}
