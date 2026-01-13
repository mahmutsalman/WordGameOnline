package com.codenames.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CreateRoomRequest DTO.
 * Tests validation constraints for room creation.
 */
class CreateRoomRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Test that valid username passes validation.
     */
    @Test
    void shouldPassValidationWithValidUsername() {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername("ValidUser");

        // Act
        Set<ConstraintViolation<CreateRoomRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations)
                .as("Valid username should have no validation errors")
                .isEmpty();
    }

    /**
     * Test that blank username fails validation.
     */
    @Test
    void shouldFailValidationWithBlankUsername() {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername("");

        // Act
        Set<ConstraintViolation<CreateRoomRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations)
                .as("Blank username should fail validation")
                .isNotEmpty();
    }

    /**
     * Test that null username fails validation.
     */
    @Test
    void shouldFailValidationWithNullUsername() {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername(null);

        // Act
        Set<ConstraintViolation<CreateRoomRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations)
                .as("Null username should fail validation")
                .isNotEmpty();
    }

    /**
     * Test that username over 20 characters fails validation.
     */
    @Test
    void shouldFailValidationWithUsernameTooLong() {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername("ThisUsernameIsWayTooLongForTheLimit");

        // Act
        Set<ConstraintViolation<CreateRoomRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations)
                .as("Username over 20 chars should fail validation")
                .isNotEmpty();
    }

    /**
     * Test that username with exactly 20 characters passes validation.
     */
    @Test
    void shouldPassValidationWith20CharUsername() {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername("ExactlyTwentyChars!");  // 20 chars

        // Act
        Set<ConstraintViolation<CreateRoomRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations)
                .as("Username with exactly 20 chars should pass")
                .isEmpty();
    }
}
