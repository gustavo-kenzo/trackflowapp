package com.gustavo.trackflowapp.modules.category.dto;

import com.gustavo.trackflowapp.modules.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRegisterDTO(
        @NotBlank
        String name,

        @NotNull
        CategoryType type,

        Boolean systemDefault) {
}
