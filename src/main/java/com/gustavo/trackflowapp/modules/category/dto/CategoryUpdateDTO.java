package com.gustavo.trackflowapp.modules.category.dto;

import com.gustavo.trackflowapp.modules.category.CategoryType;

public record CategoryUpdateDTO(
        String name,
        CategoryType type,
        Boolean systemDefault,
        Boolean active
) {
}
