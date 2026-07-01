package com.gustavo.trackflowapp.modules.category;

import com.gustavo.trackflowapp.modules.category.dto.CategoryDataDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryRegisterDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryUpdateDTO;
import com.gustavo.trackflowapp.modules.user.User;
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
        if (dto.systemDefault() != null && dto.systemDefault())
            categoryRepository.unsetSystemDefault(user.getId());

        var category = new Category(user, dto.name(), dto.systemDefault());

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
        var category = categoryRepository.findCategory(userId, categoryId).orElseThrow(() -> new RuntimeException("Category not found for this user"));

        var hasChanged = false;

        hasChanged |= processActiveUpdate(dto.active(), category);
        System.out.println("ACTIVE");
        hasChanged |= category.updateName(dto.name());
        System.out.println("NME");
        hasChanged |= processSystemDefaultUpdate(dto.systemDefault(), category, userId);
        System.out.println("SYSTEM DEFAULT");

        if (!hasChanged)
            throw new RuntimeException("No changes were made (JSON null)");

        return new CategoryDataDTO(category);
    }

    private boolean processActiveUpdate(Boolean willBeActive, Category category) {
        // 1. Se o DTO não enviou nada, validamos apenas se a categoria já está inativa
        if (willBeActive == null) {
            if (!category.isActive()) {
                throw new RuntimeException("Category is inactive (turn it active first)");
            }
            return false;
        }

        // 2. Se tentou desativar (false) e ela é padrão do sistema, bloqueia
        if (!willBeActive && category.isSystemDefault()) {
            throw new RuntimeException("System default category cannot be deactivated");
        }

        // 3. Se passou pelas regras, atualiza e retorna se houve mudança
        return category.updateActive(willBeActive);
    }

    private boolean processSystemDefaultUpdate(Boolean willBeDefault, Category category, Long userId) {
        if (willBeDefault == null) {
            return false;
        }
        if (willBeDefault) {
            if (category.isSystemDefault())
                throw new RuntimeException("Category is already system default");
            categoryRepository.unsetSystemDefault(userId);
            return category.updateSystemDefault(true);
        } else {
            if (!category.isSystemDefault())
                throw new RuntimeException("Category is already not system default");
            return category.updateSystemDefault(false);
        }
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        var category = categoryRepository.findCategory(userId, categoryId).orElseThrow(() -> new RuntimeException("Category not found for this user"));

        if (category.isSystemDefault())
            throw new RuntimeException("System default category cannot be deleted");

        // TODO: Verificar se existem transações associadas. Nova regra: permitir que exista transacções com categorias null
        // if (transactionRepository.existsByCategoryId(categoryId)) {
        //     throw new RuntimeException("Cannot delete category with associated transactions");
        // }
        categoryRepository.delete(category);
    }

    public Category getCategory(Long categoryId, Long userId) {
        return categoryRepository.findCategory(userId, categoryId).orElseThrow(() -> new RuntimeException("Category id not found"));
    }
}
