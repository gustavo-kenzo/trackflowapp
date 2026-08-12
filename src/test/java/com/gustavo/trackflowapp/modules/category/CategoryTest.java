package com.gustavo.trackflowapp.modules.category;

import com.gustavo.trackflowapp.modules.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Category")
class CategoryTest {

    private Category category;
    private User user;

    @BeforeEach
    void setup() {
        user = new User("Kenzo", "kenzo@email.com", "123456");
        category = new Category(user, "Alimentação");
    }

    @Nested
    @DisplayName("constructor()")
    class Constructor {
        @Test
        @DisplayName("Cria categoria ativa com usuário e nome informados")
        void shouldCreateActiveCategoryWithUserAndName() {
            //Arrange
            var name = "Transporte";

            //Act
            var categoryTest = new Category(user, name);

            //Assert
            assertThat(categoryTest.getUser()).isEqualTo(user);
            assertThat(categoryTest.getName()).isEqualTo(name);
            assertThat(categoryTest.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {
        @Test
        @DisplayName("Atualiza nome e status quando ambos são informados")
        void shouldUpdateNameAndActiveWhenBothAreProvided() {
            //Arrange
            var newName = "Lazer";

            //Act
            category.update(newName, false);

            //Assert
            assertThat(category.getName()).isEqualTo(newName);
            assertThat(category.isActive()).isFalse();
        }

        @Test
        @DisplayName("Não altera status quando somente o nome é informado")
        void shouldOnlyUpdateNameWhenActiveIsNull() {
            //Arrange
            var newName = "Lazer";

            //Act
            category.update(newName, null);

            //Assert
            assertThat(category.getName()).isEqualTo(newName);
            assertThat(category.isActive()).isTrue();
        }

        @Test
        @DisplayName("Não altera nome quando somente o status é informado")
        void shouldOnlyUpdateActiveWhenNameIsNull() {
            //Arrange
            var originalName = category.getName();

            //Act
            category.update(null, false);

            //Assert
            assertThat(category.getName()).isEqualTo(originalName);
            assertThat(category.isActive()).isFalse();
        }

        @Test
        @DisplayName("Não altera nada quando nome e status são nulos")
        void shouldNotUpdateAnythingWhenBothAreNull() {
            //Arrange
            var originalName = category.getName();
            var originalActive = category.isActive();

            //Act
            category.update(null, null);

            //Assert
            assertThat(category.getName()).isEqualTo(originalName);
            assertThat(category.isActive()).isEqualTo(originalActive);
        }
    }
}