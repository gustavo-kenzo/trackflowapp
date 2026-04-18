package com.gustavo.trackflowapp.modules.user.dto;

import jakarta.validation.constraints.Pattern;

public record UserUpdateDTO(
        String name,
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$")
        String password
) {
}
