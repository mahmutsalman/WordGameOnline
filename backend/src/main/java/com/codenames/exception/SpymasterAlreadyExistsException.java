package com.codenames.exception;

import com.codenames.model.Team;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SpymasterAlreadyExistsException extends RuntimeException {
    public SpymasterAlreadyExistsException(Team team) {
        super("Team " + team + " already has a spymaster");
    }
}
