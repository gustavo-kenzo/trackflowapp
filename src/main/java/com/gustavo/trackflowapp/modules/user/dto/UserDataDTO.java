package com.gustavo.trackflowapp.modules.user.dto;

import com.gustavo.trackflowapp.modules.user.User;

public record UserDataDTO(
        Long id,
        String name,
        String email
) {
    public UserDataDTO(User user) {
        this(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
