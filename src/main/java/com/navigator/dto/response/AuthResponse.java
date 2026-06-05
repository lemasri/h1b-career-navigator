package com.navigator.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthResponse {
    private final String token;
    @Builder.Default
    private final String tokenType = "Bearer";
    private final UUID userId;
    private final String email;
    private final String fullName;
}
