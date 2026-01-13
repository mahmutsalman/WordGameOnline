package com.codenames.dto.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorEvent {
    private final String type = "ERROR";
    private String message;
}
