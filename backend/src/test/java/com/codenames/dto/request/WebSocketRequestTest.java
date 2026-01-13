package com.codenames.dto.request;

import com.codenames.model.Role;
import com.codenames.model.Team;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketRequestTest {

    private Validator validator;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ========== JOIN_ROOM_WS_REQUEST TESTS ==========

    @Test
    void shouldValidateValidJoinRoomWsRequest() {
        JoinRoomWsRequest request = new JoinRoomWsRequest();
        request.setUsername("ValidUser");

        Set<ConstraintViolation<JoinRoomWsRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectBlankUsername() {
        JoinRoomWsRequest request = new JoinRoomWsRequest();
        request.setUsername("");

        Set<ConstraintViolation<JoinRoomWsRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("must not be blank");
    }

    @Test
    void shouldRejectNullUsername() {
        JoinRoomWsRequest request = new JoinRoomWsRequest();
        request.setUsername(null);

        Set<ConstraintViolation<JoinRoomWsRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldAcceptUsernameWithSpaces() {
        JoinRoomWsRequest request = new JoinRoomWsRequest();
        request.setUsername("User Name");

        Set<ConstraintViolation<JoinRoomWsRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // ========== CHANGE_TEAM_REQUEST TESTS ==========

    @Test
    void shouldAllowNullTeamForSpectator() {
        ChangeTeamRequest request = new ChangeTeamRequest();
        request.setTeam(null);
        request.setRole(Role.SPECTATOR);

        Set<ConstraintViolation<ChangeTeamRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldAllowValidTeamAndRole() {
        ChangeTeamRequest request = new ChangeTeamRequest();
        request.setTeam(Team.BLUE);
        request.setRole(Role.OPERATIVE);

        Set<ConstraintViolation<ChangeTeamRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldAllowBlueTeamWithSpymaster() {
        ChangeTeamRequest request = new ChangeTeamRequest();
        request.setTeam(Team.BLUE);
        request.setRole(Role.SPYMASTER);

        Set<ConstraintViolation<ChangeTeamRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldAllowRedTeamWithOperative() {
        ChangeTeamRequest request = new ChangeTeamRequest();
        request.setTeam(Team.RED);
        request.setRole(Role.OPERATIVE);

        Set<ConstraintViolation<ChangeTeamRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldAllowNullRoleWithNullTeam() {
        ChangeTeamRequest request = new ChangeTeamRequest();
        request.setTeam(null);
        request.setRole(null);

        Set<ConstraintViolation<ChangeTeamRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}
