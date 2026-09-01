package com.gustavo.trackflowapp.integration.modules;

import com.gustavo.trackflowapp.integration.AbstractIntegrationTest;
import com.gustavo.trackflowapp.integration.fixtures.LoginTestFixture;
import com.gustavo.trackflowapp.modules.user.UserRepository;
import com.gustavo.trackflowapp.modules.user.dto.UserDataDTO;
import com.gustavo.trackflowapp.modules.user.dto.UserRegisterDTO;
import com.gustavo.trackflowapp.modules.user.dto.UserUpdateDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserIntegrationTest extends AbstractIntegrationTest {

    private static final String PATH = "/users";
    private static final String USER_NAME = "Kenzo";
    private static final String USER_EMAIL = "kenzo@email.com";
    private static final String USER_PASSWORD = "valid_password_123456S&";
    private static final String NEW_USER_PASSWORD = "new_valid_password_123456S&";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginTestFixture loginFixture;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Order(1)
    @DisplayName("POST - Cria usuário com sucesso")
    void should() {
        //Arrange
        var dto = new UserRegisterDTO(USER_NAME, USER_EMAIL, USER_PASSWORD);

        //Act
        var response = restTestClient.post().uri(PATH)
                .body(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(UserDataDTO.class)
                .returnResult()
                .getResponseBody();

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isPositive();
        assertThat(response.name()).isEqualTo("Kenzo");
        assertThat(response.email()).isEqualTo("kenzo@email.com");
        assertThat(userRepository.findById(response.id())).isPresent();
    }


    @Test
    @Order(2)
    @DisplayName("PUT - Atualiza nome e senha do usuário")
    void shouldUpdateUserNameAndPassword() {
        //Arrange
        var dto = new UserUpdateDTO("New Name", "new_valid_password_123456S&");
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);

        //Act
        var response = restTestClient.put().uri(PATH)
                .headers(header -> header.setBearerAuth(token))
                .body(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDataDTO.class)
                .returnResult()
                .getResponseBody();

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isPositive();
        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.email()).isEqualTo("kenzo@email.com");
        var optionalUser = userRepository.findByEmail("kenzo@email.com");
        assertThat(optionalUser).isPresent();
        assertThat(passwordEncoder.matches("valid_password_123456S&", optionalUser.get().getPassword())).isFalse();
        assertThat(passwordEncoder.matches("new_valid_password_123456S&", optionalUser.get().getPassword())).isTrue();

    }

    @Test
    @Order(3)
    @DisplayName("GET - Retorna os dados do usuário")
    void shouldReturnUserData() {
        //Arrange
        var token = loginFixture.login(USER_EMAIL, NEW_USER_PASSWORD);

        //Act
        var response = restTestClient.get().uri(PATH)
                .headers(header -> header.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDataDTO.class)
                .returnResult()
                .getResponseBody();

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isPositive();
        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.email()).isEqualTo("kenzo@email.com");

    }

    @Test
    @Order(4)
    @DisplayName("DELETE - Deleta usuário")
    void shouldDeleteUser() {
        //Arrange
        var token = loginFixture.login(USER_EMAIL, NEW_USER_PASSWORD);

        //Act
        restTestClient.delete().uri(PATH)
                .headers(header -> header.setBearerAuth(token))
                .exchange()
                .expectStatus().isNoContent();

        //Assert
        assertThat(userRepository.findByEmail("kenzo@email.com")).isNotPresent();
    }
}
