package com.gustavo.trackflowapp.modules.category;

import com.gustavo.trackflowapp.modules.category.dto.CategoryDataDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryRegisterDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryUpdateDTO;
import com.gustavo.trackflowapp.modules.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryDataDTO> createCategory(@RequestBody @Valid CategoryRegisterDTO dto, @AuthenticationPrincipal User user, UriComponentsBuilder uriBuilder) {
        var categoryData = categoryService.createCategory(dto, user);
        var uri = uriBuilder.path("/categories/{id}").buildAndExpand(categoryData.id()).toUri();
        return ResponseEntity.created(uri).body(categoryData);
    }

    @GetMapping("/active")
    public ResponseEntity<Page<CategoryDataDTO>> listCategoriesActive(Pageable pageable, @AuthenticationPrincipal(expression = "id") Long id) {
        return ResponseEntity.ok(categoryService.listCategoriesActive(id, pageable));
    }

    @GetMapping("/inactive")
    public ResponseEntity<Page<CategoryDataDTO>> listCategoriesInactive(Pageable pageable, @AuthenticationPrincipal(expression = "id") Long id) {
        return ResponseEntity.ok(categoryService.listCategoriesInactive(id, pageable));
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryDataDTO> updateCategory(@RequestBody @Valid CategoryUpdateDTO dto, @AuthenticationPrincipal(expression = "id") Long userId, @PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.updateCategory(dto, userId, categoryId));
    }
}
