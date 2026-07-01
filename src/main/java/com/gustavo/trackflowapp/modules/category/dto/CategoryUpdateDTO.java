package com.gustavo.trackflowapp.modules.category.dto;


public record CategoryUpdateDTO(
        String name,
        Boolean systemDefault,
        Boolean active
) {
}
