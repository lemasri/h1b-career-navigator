package com.navigator.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    // Kept out of toString() so the plaintext password never reaches logs.
    @ToString.Exclude
    @NotBlank(message = "Password is required")
    private String password;
}
