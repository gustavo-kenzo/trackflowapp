package com.gustavo.trackflowapp.modules.category;

import com.gustavo.trackflowapp.modules.category.dto.CategoryDataDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryRegisterDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryUpdateDTO;
import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryDataDTO createCategory(CategoryRegisterDTO dto, User user) {
        var category = new Category(user, dto.name());

        categoryRepository.save(category);
        return new CategoryDataDTO(category);
    }

    public Page<CategoryDataDTO> listCategoriesActive(Long id, Pageable pageable) {
        var category = categoryRepository.findCategoriesActive(id, pageable);
        return category.map(CategoryDataDTO::new);
    }

    public Page<CategoryDataDTO> listCategoriesInactive(Long id, Pageable pageable) {
        var category = categoryRepository.findCategoriesInactive(id, pageable);
        return category.map(CategoryDataDTO::new);
    }

    @Transactional
    public CategoryDataDTO updateCategory(CategoryUpdateDTO dto, Long userId, Long categoryId) {
        var category = categoryRepository.findCategory(userId, categoryId).orElseThrow(() -> new ResourceNotFoundException("Category not found for this user"));

        category.update(dto.name(), dto.active());
        return new CategoryDataDTO(category);
    }

    public Category getCategory(Long categoryId, Long userId) {
        return categoryRepository.findCategory(userId, categoryId).orElseThrow(() -> new ResourceNotFoundException("Category id not found"));
    }
}
