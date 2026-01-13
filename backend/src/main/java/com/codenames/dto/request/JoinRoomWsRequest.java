package com.codenames.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinRoomWsRequest {
    @NotBlank(message = "Username must not be blank")
    private String username;
}
