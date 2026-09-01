package com.gustavo.trackflowapp.integration.modules;

import com.gustavo.trackflowapp.integration.AbstractIntegrationTest;
import com.gustavo.trackflowapp.integration.fixtures.LoginTestFixture;
import com.gustavo.trackflowapp.integration.fixtures.UserTestFixture;
import com.gustavo.trackflowapp.modules.category.Category;
import com.gustavo.trackflowapp.modules.category.CategoryRepository;
import com.gustavo.trackflowapp.modules.category.dto.CategoryDataDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryRegisterDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryUpdateDTO;
import com.gustavo.trackflowapp.modules.user.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CategoryIntegrationTest extends AbstractIntegrationTest {

    private static final String PATH = "/categories";
    private static final String USER_NAME = "Kenzo";
    private static final String USER_EMAIL = "kenzo@email.com";
    private static final String USER_PASSWORD = "valid_password_123456S&";
    private static String CATEGORY_NAME = "Categoria 1";

    @Autowired
    private UserTestFixture userFixture;

    @Autowired
    private LoginTestFixture loginFixture;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user;

    private Category category;

    private void createUser() {
        user = userFixture.register(USER_NAME, USER_EMAIL, USER_PASSWORD);
    }

    private void updateCategoryName() {
        CATEGORY_NAME = "Categoria 1 Atualizada";
    }

    @Test
    @Order(1)
    @DisplayName("POST - Cria categoria vinculada a usuário")
    void shouldCreateUserCategory() {
        //Arrange
        createUser();
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);
        var dto = new CategoryRegisterDTO(CATEGORY_NAME);

        //Act
        var response = restTestClient.post().uri(PATH)
                .headers(header -> header.setBearerAuth(token))
                .body(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(CategoryDataDTO.class)
                .returnResult()
                .getResponseBody();

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isPositive();
        assertThat(response.name()).isEqualTo(CATEGORY_NAME);
        assertThat(response.owner()).isEqualTo(user.getName());
        assertThat(response.active()).isTrue();

        var optionalCategory = categoryRepository.findCategory(user.getId(), response.id());
        assertThat(optionalCategory).isPresent();
        this.category = optionalCategory.get();
    }

    @Test
    @Order(2)
    @DisplayName("GET - Retorna contas ativas do usuário")
    void shouldReturnUserActiveAccounts() {
        //Arrange
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);

        //Act
        var response = restTestClient.get().uri(PATH + "/active")
                .headers(header -> header.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class)
                .returnResult()
                .getResponseBody();

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.get("totalElements").asLong()).isEqualTo(1L);
        assertThat(response.get("totalPages").asLong()).isEqualTo(1L);
        var firstContentResponse = response.get("content").get(0);
        assertThat(firstContentResponse.get("name").asString()).isEqualTo(CATEGORY_NAME);
        assertThat(firstContentResponse.get("owner").asString()).isEqualTo(USER_NAME);
        assertThat(firstContentResponse.get("active").asBoolean()).isEqualTo(true);
    }

    @Test
    @Order(3)
    @DisplayName("PATCH - Atualiza nome da categoria")
    void shouldUpdateCategoryName() {
        //Arrange
        updateCategoryName();
        var dto = new CategoryUpdateDTO(CATEGORY_NAME, null);
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);

        //Act
        var response = restTestClient.patch().uri(PATH + "/{categoryId}", this.category.getId())
                .headers(header -> header.setBearerAuth(token))
                .body(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CategoryDataDTO.class)
                .returnResult()
                .getResponseBody();

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isPositive();
        assertThat(response.name()).isEqualTo(CATEGORY_NAME);
        assertThat(response.owner()).isEqualTo(USER_NAME);
        assertThat(response.active()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("PATCH - Atualiza status da categoria")
    void shouldUpdateCategoryStatus() {
        //Arrange
        var dto = new CategoryUpdateDTO(null, false);
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);

        //Act
        var response = restTestClient.patch().uri(PATH + "/{categoryId}", this.category.getId())
                .headers(header -> header.setBearerAuth(token))
                .body(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CategoryDataDTO.class)
                .returnResult()
                .getResponseBody();

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isPositive();
        assertThat(response.name()).isEqualTo(CATEGORY_NAME);
        assertThat(response.owner()).isEqualTo(USER_NAME);
        assertThat(response.active()).isFalse();
    }

    @Test
    @Order(5)
    @DisplayName("GET - Retorna contas inativas do usuário")
    void shouldReturnUserInactiveAccounts() {
        //Arrange
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);

        //Act
        var response = restTestClient.get().uri(PATH + "/inactive")
                .headers(header -> header.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class)
                .returnResult()
                .getResponseBody();

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.get("totalElements").asLong()).isEqualTo(1L);
        assertThat(response.get("totalPages").asLong()).isEqualTo(1L);
        var firstContentResponse = response.get("content").get(0);
        assertThat(firstContentResponse.get("name").asString()).isEqualTo(CATEGORY_NAME);
        assertThat(firstContentResponse.get("owner").asString()).isEqualTo(USER_NAME);
        assertThat(firstContentResponse.get("active").asBoolean()).isEqualTo(false);
    }

    @Test
    @Order(6)
    @DisplayName("DELETE - Deleta categoria do usuário")
    void shouldDeleteUserCategory() {
        //Arrange
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);

        //Act
        restTestClient.delete().uri(PATH + "/{categoryId}", category.getId())
                .headers(header -> header.setBearerAuth(token))
                .exchange()
                .expectStatus().isNoContent();

        //Assert
        assertThat(categoryRepository.findCategory(user.getId(), category.getId())).isNotPresent();
    }
}
