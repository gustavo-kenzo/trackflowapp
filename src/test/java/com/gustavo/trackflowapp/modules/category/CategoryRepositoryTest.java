package com.gustavo.trackflowapp.modules.category;

import com.gustavo.trackflowapp.modules.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    private static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 10);

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TestEntityManager entityManager;

    private List<Category> categories = new ArrayList<>();
    private User user1;
    private User user2;

    @BeforeEach
    void setup() {
        user1 = new User("Mark", "mark@email.com", "123456&");
        user2 = new User("Nolan", "nolan@email.com", "123456&");
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        var category1 = new Category(user1, "Alimentação");
        var category2 = new Category(user1, "Transporte");
        var category3 = new Category(user1, "Lazer");
        var category4 = new Category(user2, "Alimentação");
        var category5 = new Category(user2, "Saúde");
        categories.addAll(List.of(category1, category2, category3, category4, category5));
        categoryRepository.saveAll(categories);
    }

    @Nested
    @DisplayName("findCategoriesActive()")
    class FindCategoriesActive {
        @Test
        @DisplayName("Retorna apenas categorias ativas do usuário informado")
        void shouldReturnOnlyActiveCategoriesBelongingToUser() {
            //Arrange
            var userId = user1.getId();
            var expectedIds = Stream.of(categories.get(0), categories.get(1), categories.get(2)).map(Category::getId).toList();

            //Act
            var result = categoryRepository.findCategoriesActive(userId, DEFAULT_PAGE);

            //Assert
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent()).extracting(Category::getId)
                    .containsExactlyInAnyOrderElementsOf(expectedIds);
            assertThat(result.getContent()).allMatch(Category::isActive);
        }

        @Test
        @DisplayName("Não retorna categorias inativas")
        void shouldNotReturnInactiveCategories() {
            //Arrange
            var userId = user1.getId();
            var inactiveCategory = categories.get(0);
            inactiveCategory.update(null, false);
            entityManager.flush();

            //Act
            var result = categoryRepository.findCategoriesActive(userId, DEFAULT_PAGE);

            //Assert
            assertThat(result.getContent()).extracting(Category::getId)
                    .doesNotContain(inactiveCategory.getId());
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Não retorna categorias de outros usuários")
        void shouldNotReturnCategoriesFromAnotherUsers() {
            //Arrange
            var categoriesFromUser1 = Stream.of(categories.get(0), categories.get(1), categories.get(2)).map(Category::getId).toList();

            //Act
            var result = categoryRepository.findCategoriesActive(user2.getId(), DEFAULT_PAGE);

            //Assert
            assertThat(result.getContent()).extracting(Category::getId)
                    .doesNotContainAnyElementsOf(categoriesFromUser1);
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário não tem categorias ativas")
        void shouldReturnEmptyPageWhenUserHasNoActiveCategories() {
            //Arrange
            var userWithoutCategories = new User("Anna", "anna@email.com", "123456&");
            entityManager.persistAndFlush(userWithoutCategories);

            //Act
            var result = categoryRepository.findCategoriesActive(userWithoutCategories.getId(), DEFAULT_PAGE);

            //Assert
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("findCategoriesInactive()")
    class FindCategoriesInactive {
        @Test
        @DisplayName("Retorna apenas categorias inativas do usuário informado")
        void shouldReturnOnlyInactiveCategoriesBelongingToUser() {
            //Arrange
            var userId = user1.getId();
            var inactiveCategories = List.of(categories.get(0), categories.get(1));
            inactiveCategories.forEach(category -> category.update(null, false));
            entityManager.flush();
            var expectedIds = inactiveCategories.stream().map(Category::getId).toList();

            //Act
            var result = categoryRepository.findCategoriesInactive(userId, DEFAULT_PAGE);

            //Assert
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(Category::getId)
                    .containsExactlyInAnyOrderElementsOf(expectedIds);
            assertThat(result.getContent()).noneMatch(Category::isActive);
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário não tem categorias inativas")
        void shouldReturnEmptyPageWhenUserHasNoInactiveCategories() {
            //Act
            var result = categoryRepository.findCategoriesInactive(user1.getId(), DEFAULT_PAGE);

            //Assert
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("findCategory()")
    class FindCategory {
        @Test
        @DisplayName("Retorna categoria especificada do usuário informado")
        void shouldReturnSpecificCategoryFromUser() {
            //Arrange
            var expectedCategory = categories.getFirst();
            var userId = user1.getId();

            //Act
            var result = categoryRepository.findCategory(userId, expectedCategory.getId());

            //Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(expectedCategory.getId());
            assertThat(result.get().getUser().getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Retorna vazio quando categoria não existe")
        void shouldReturnEmptyWhenCategoryDoesNotExist() {
            //Arrange
            var nonExistedCategoryId = 9999L;

            //Act
            var result = categoryRepository.findCategory(user1.getId(), nonExistedCategoryId);

            //Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Retorna vazio quando categoria não pertence ao usuário")
        void shouldReturnEmptyWhenCategoryDoesNotBelongToUser() {
            //Arrange
            var categoryFromUser2 = categories.get(4).getId();

            //Act
            var result = categoryRepository.findCategory(user1.getId(), categoryFromUser2);

            //Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hardDelete()")
    class HardDelete {
        @Test
        @DisplayName("Deleta categoria do usuário e retorna linhas alteradas")
        void shouldDeleteUserCategoryAndReturnRowsAffected() {
            //Arrange
            var categoryId = categories.getFirst().getId();
            var userId = user1.getId();

            //Act
            var result = categoryRepository.hardDelete(categoryId, userId);

            //Assert
            assertThat(result).isEqualTo(1);
            assertThat(categoryRepository.findCategory(userId, categoryId)).isEmpty();
        }

        @Test
        @DisplayName("Retorna 0 quando categoria não existe")
        void shouldReturnZeroWhenCategoryDoesNotExist() {
            //Arrange
            var nonExistedCategoryId = 9999L;

            //Act
            var result = categoryRepository.hardDelete(nonExistedCategoryId, user1.getId());

            //Assert
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("Retorna 0 quando categoria não pertence ao usuário")
        void shouldReturnZeroWhenCategoryDoesNotBelongToUser() {
            //Arrange
            var categoryFromUser2 = categories.get(4).getId();

            //Act
            var result = categoryRepository.hardDelete(categoryFromUser2, user1.getId());

            //Assert
            assertThat(result).isEqualTo(0);
            assertThat(categoryRepository.findCategory(user2.getId(), categoryFromUser2)).isPresent();
        }
    }
}