package com.gustavo.trackflowapp.modules.category;

import com.gustavo.trackflowapp.modules.category.dto.CategoryDataDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryRegisterDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryUpdateDTO;
import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Category Service")
class CategoryServiceTest {

    private static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 10);

    @Mock
    CategoryRepository categoryRepository;
    @InjectMocks
    CategoryService categoryService;

    private User user;
    private Category category1;
    private Category category2;

    @BeforeEach
    void setup() {
        user = new User("Kenzo", "kenzo@email.com", "123456");
        ReflectionTestUtils.setField(user, "id", 1L);
        category1 = new Category(user, "Alimentação");
        category2 = new Category(user, "Transporte");
    }

    @Nested
    @DisplayName("createCategory()")
    class CreateCategory {
        @Test
        @DisplayName("Salva categoria corretamente no banco e retorna esses dados em um DTO")
        void shouldSaveCategoryAndReturnDataWhenCreateCategory() {
            //Arrange
            var dto = new CategoryRegisterDTO("Alimentação");
            var captor = ArgumentCaptor.forClass(Category.class);

            //Act
            var result = categoryService.createCategory(dto, user);

            //Assert
            verify(categoryRepository).save(captor.capture());
            verifyNoMoreInteractions(categoryRepository);

            var capturedCategory = captor.getValue();
            assertThat(capturedCategory.getUser()).isEqualTo(user);
            assertThat(capturedCategory.getName()).isEqualTo(dto.name());

            assertThat(result.owner()).isEqualTo(user.getName());
            assertThat(result.name()).isEqualTo(dto.name());
            assertThat(result.active()).isTrue();
        }
    }

    @Nested
    @DisplayName("listCategoriesActive()")
    class ListCategoriesActive {
        @Test
        @DisplayName("Retorna categorias ativas paginadas quando usuário tem categorias")
        void shouldReturnPaginatedActiveCategories() {
            //Arrange
            var id = 1L;
            var categoryPages = new PageImpl<>(List.of(category1, category2), DEFAULT_PAGE, 2);
            when(categoryRepository.findCategoriesActive(id, DEFAULT_PAGE)).thenReturn(categoryPages);
            var expectedCategories = categoryPages.get().map(Category::getName).toList();

            //Act
            var result = categoryService.listCategoriesActive(id, DEFAULT_PAGE);

            //Assert
            verify(categoryRepository).findCategoriesActive(id, DEFAULT_PAGE);
            verifyNoMoreInteractions(categoryRepository);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getContent()).extracting(CategoryDataDTO::name)
                    .containsExactlyElementsOf(expectedCategories);
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário não tem categorias ativas")
        void shouldReturnEmptyPageWhenUserDoesNotHaveActiveCategories() {
            //Arrange
            var id = 1L;
            when(categoryRepository.findCategoriesActive(id, DEFAULT_PAGE)).thenReturn(Page.empty(DEFAULT_PAGE));

            //Act
            var result = categoryService.listCategoriesActive(id, DEFAULT_PAGE);

            //Assert
            verify(categoryRepository).findCategoriesActive(id, DEFAULT_PAGE);
            verifyNoMoreInteractions(categoryRepository);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("listCategoriesInactive()")
    class ListCategoriesInactive {
        @Test
        @DisplayName("Retorna categorias inativas paginadas quando usuário tem categorias")
        void shouldReturnPaginatedInactiveCategories() {
            //Arrange
            var id = 1L;
            category1.update(null, false);
            var categoryPages = new PageImpl<>(List.of(category1), DEFAULT_PAGE, 1);
            when(categoryRepository.findCategoriesInactive(id, DEFAULT_PAGE)).thenReturn(categoryPages);

            //Act
            var result = categoryService.listCategoriesInactive(id, DEFAULT_PAGE);

            //Assert
            verify(categoryRepository).findCategoriesInactive(id, DEFAULT_PAGE);
            verifyNoMoreInteractions(categoryRepository);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).extracting(CategoryDataDTO::active)
                    .containsExactly(false);
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário não tem categorias inativas")
        void shouldReturnEmptyPageWhenUserDoesNotHaveInactiveCategories() {
            //Arrange
            var id = 1L;
            when(categoryRepository.findCategoriesInactive(id, DEFAULT_PAGE)).thenReturn(Page.empty(DEFAULT_PAGE));

            //Act
            var result = categoryService.listCategoriesInactive(id, DEFAULT_PAGE);

            //Assert
            verify(categoryRepository).findCategoriesInactive(id, DEFAULT_PAGE);
            verifyNoMoreInteractions(categoryRepository);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("updateCategory()")
    class UpdateCategory {
        @Test
        @DisplayName("Atualiza categoria do usuário com sucesso")
        void shouldUpdateUserCategory() {
            //Arrange
            var userId = 1L;
            var categoryId = 1L;
            var dto = new CategoryUpdateDTO("Lazer", false);
            when(categoryRepository.findCategory(userId, categoryId)).thenReturn(Optional.of(category1));

            //Act
            var result = categoryService.updateCategory(dto, userId, categoryId);

            //Assert
            verify(categoryRepository).findCategory(userId, categoryId);
            verifyNoMoreInteractions(categoryRepository);
            /*Não chame save() explicitamente - depende do dirty checking do Transactional*/
            verify(categoryRepository, never()).save(any());

            assertThat(result.name()).isEqualTo(dto.name());
            assertThat(result.active()).isFalse();
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando categoria não encontrada")
        void shouldThrowWhenCategoryNotFound() {
            //Arrange
            var userId = 1L;
            var categoryId = 1L;
            var dto = new CategoryUpdateDTO("Lazer", false);
            when(categoryRepository.findCategory(userId, categoryId)).thenReturn(Optional.empty());

            //Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(dto, userId, categoryId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category not found for this user");
            verify(categoryRepository).findCategory(userId, categoryId);
            verifyNoMoreInteractions(categoryRepository);
        }
    }

    @Nested
    @DisplayName("getCategory()")
    class GetCategory {
        @Test
        @DisplayName("Retorna entidade completa da categoria buscada pelo usuário")
        void shouldReturnEntityCategory() {
            //Arrange
            var userId = 1L;
            var categoryId = 1L;
            when(categoryRepository.findCategory(userId, categoryId)).thenReturn(Optional.of(category1));

            //Act
            var result = categoryService.getCategory(categoryId, userId);

            //Assert
            verify(categoryRepository).findCategory(userId, categoryId);
            verifyNoMoreInteractions(categoryRepository);
            assertThat(result).isSameAs(category1);
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando categoria não encontrada")
        void shouldThrowWhenCategoryNotFound() {
            //Arrange
            var userId = 1L;
            var categoryId = 1L;
            when(categoryRepository.findCategory(userId, categoryId)).thenReturn(Optional.empty());

            //Act & Assert
            assertThatThrownBy(() -> categoryService.getCategory(categoryId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category id not found");
            verify(categoryRepository).findCategory(userId, categoryId);
            verifyNoMoreInteractions(categoryRepository);
        }
    }

    @Nested
    @DisplayName("hardDelete()")
    class HardDelete {
        @Test
        @DisplayName("Exclui registro de categoria do usuário do banco com sucesso")
        void shouldDeleteUserCategory() {
            //Arrange
            var categoryId = 1L;
            when(categoryRepository.hardDelete(categoryId, user.getId())).thenReturn(1);

            //Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> categoryService.hardDelete(categoryId, user));
            verify(categoryRepository).hardDelete(categoryId, user.getId());
            verifyNoMoreInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando categoria não encontrada")
        void shouldThrowWhenUserCategoryNotFound() {
            //Arrange
            var categoryId = 1L;
            when(categoryRepository.hardDelete(categoryId, user.getId())).thenReturn(0);

            //Act & Assert
            assertThatThrownBy(() -> categoryService.hardDelete(categoryId, user))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category not found for hard delete");
            verify(categoryRepository).hardDelete(categoryId, user.getId());
            verifyNoMoreInteractions(categoryRepository);
        }
    }
}