package com.gustavo.trackflowapp.integration.modules;

import com.gustavo.trackflowapp.integration.AbstractIntegrationTest;
import com.gustavo.trackflowapp.integration.fixtures.LoginTestFixture;
import com.gustavo.trackflowapp.integration.fixtures.UserTestFixture;
import com.gustavo.trackflowapp.modules.account.Account;
import com.gustavo.trackflowapp.modules.account.AccountRepository;
import com.gustavo.trackflowapp.modules.account.dto.AccountDataDTO;
import com.gustavo.trackflowapp.modules.account.dto.AccountRegisterDTO;
import com.gustavo.trackflowapp.modules.user.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountIntegrationTest extends AbstractIntegrationTest {

    private static final String PATH = "/accounts";
    private static final String USER_NAME = "Kenzo";
    private static final String USER_EMAIL = "kenzo@email.com";
    private static final String USER_PASSWORD = "valid_password_123456S&";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserTestFixture userFixture;

    @Autowired
    private LoginTestFixture loginFixture;

    private Account account;

    private User user;

    private void registerUser() {
        user = userFixture.register(USER_NAME, USER_EMAIL, USER_PASSWORD);
    }

    @Test
    @Order(1)
    @DisplayName("POST - Cria uma conta com informações que o usuário passa")
    void shouldCreateAccountWhenUserProvidesData() {
        //Arrange
        registerUser();
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);
        var dto = new AccountRegisterDTO("Nubank", new BigDecimal("120"));

        //Act
        var response = restTestClient.post().uri(PATH)
                .headers(header -> header.setBearerAuth(token))
                .body(dto)
                .exchange()
                .expectHeader().exists("Location")
                .expectStatus().isCreated()
                .expectBody(AccountDataDTO.class)
                .returnResult()
                .getResponseBody();

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isPositive();
        assertThat(response.owner()).isEqualTo(user.getName());
        assertThat(response.accountName()).isEqualTo("Nubank");
        assertThat(response.currentBalance()).isEqualByComparingTo("120.00");
        assertThat(response.active()).isTrue();
        var optionalAccount = accountRepository.findMyAccount(response.id(), user.getId());
        assertThat(optionalAccount).isPresent();

        this.account = optionalAccount.get();
    }

    @Test
    @Order(2)
    @DisplayName("PATCH - Desativa conta do usuário")
    void shouldDeactivateUserAccount() {
        //Arrange
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);

        //Act
        var response = restTestClient.patch().uri(PATH + "/{accountId}/deactivate", this.account.getId())
                .headers(header -> header.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountDataDTO.class)
                .returnResult()
                .getResponseBody();

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isPositive();
        assertThat(response.owner()).isEqualTo(this.user.getName());
        assertThat(response.accountName()).isEqualTo("Nubank");
        assertThat(response.currentBalance()).isEqualByComparingTo("120.00");
        assertThat(response.active()).isFalse();
    }

    @Test
    @Order(3)
    @DisplayName("PATCH - Ativa conta do usuário")
    void shouldActivateUserAccount() {
        //Arrange
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);

        //Act
        var response = restTestClient.patch().uri(PATH + "/{accountId}/activate", this.account.getId())
                .headers(header -> header.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountDataDTO.class)
                .returnResult()
                .getResponseBody();

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isPositive();
        assertThat(response.owner()).isEqualTo(this.user.getName());
        assertThat(response.accountName()).isEqualTo("Nubank");
        assertThat(response.currentBalance()).isEqualByComparingTo("120.00");
        assertThat(response.active()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("GET - Retorna contas do usuário paginada")
    void shouldReturnPaginatedUserAccounts() {
        //Arrange
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);

        //Act
        var response = restTestClient.get().uri(PATH)
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
        assertThat(firstContentResponse.get("owner").asString()).isEqualTo(this.user.getName());
        assertThat(firstContentResponse.get("account_name").asString()).isEqualTo("Nubank");
        assertThat(firstContentResponse.get("current_balance").asDecimal()).isEqualByComparingTo("120.00");
        assertThat(firstContentResponse.get("active").asBoolean()).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("DELETE - Deleta conta do usuário")
    void shouldDeleteUserAccount() {
        //Arrange
        var token = loginFixture.login(USER_EMAIL, USER_PASSWORD);

        //Act
        restTestClient.delete().uri(PATH + "/{accountId}", this.account.getId())
                .headers(header -> header.setBearerAuth(token))
                .exchange()
                .expectStatus().isNoContent();

        //Assert
        assertThat(accountRepository.findMyAccount(this.account.getId(), this.user.getId())).isNotPresent();
    }

}
