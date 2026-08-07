package com.gustavo.trackflowapp.modules.account;

import com.gustavo.trackflowapp.infra.security.SecurityFilter;
import com.gustavo.trackflowapp.modules.account.dto.AccountDataDTO;
import com.gustavo.trackflowapp.modules.account.dto.AccountRegisterDTO;
import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.exception.BusinessRuleException;
import com.gustavo.trackflowapp.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AccountController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
class AccountControllerTest {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;



    private User authenticatedUser;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setup() {
        authenticatedUser = new User("Kenzo", "kenzo@email.com", "123456");
        ReflectionTestUtils.setField(authenticatedUser, "id", 1L);
        authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
    }

    @Nested
    @DisplayName("POST /accounts")
    class CreateAccount {
        @Test
        @DisplayName("Cria conta com sucesso e retorna dados")
        void shouldCreateAccountAndReturnDataDto() throws Exception {
            //Arrange
            var registerDTO = new AccountRegisterDTO("Nubank", new BigDecimal("100"));
            var expected = new AccountDataDTO(1L, authenticatedUser.getName(), registerDTO.name(), registerDTO.openingBalance(), true);
            when(accountService.create(registerDTO, authenticatedUser)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(post("/accounts")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(registerDTO))
            );

            //Assert
            verify(accountService).create(registerDTO, authenticatedUser);
            response.andExpect(header().string("Location", endsWith("accounts/1")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(authenticatedUser.getId()))
                    .andExpect(jsonPath("$.owner").value(authenticatedUser.getName()))
                    .andExpect(jsonPath("$.account_name").value(registerDTO.name()))
                    .andExpect(jsonPath("$.current_balance").value(registerDTO.openingBalance()))
                    .andExpect(jsonPath("active").value(true));
        }

        @Test
        @DisplayName("Deve retornar 400 quando houver registro de campos inválidos")
        void shouldReturn400WhenThereIsInvalidFieldsRegister() throws Exception {
            //Arrange
            var json = """
                    {
                        "name":null,
                        "opening_balance":"100"
                    }
                    """;

            //Act
            var response = mockMvc.perform(post("/accounts")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            );

            //Assert
            response.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.field").value("name"))
                    .andExpect(jsonPath("$.message").exists());
            verifyNoInteractions(accountService);
        }
    }

    @Nested
    @DisplayName("GET /accounts")
    class FindMyAccounts {
        @Test
        @DisplayName("Lista contas do usuário informado")
        void shouldListUserAccounts() throws Exception {
            //Arrange
            var defaultPageable = PageRequest.of(0, 20);
            var accounts = List.of(
                    new AccountDataDTO(1L, "Kenzo", "Nubank", new BigDecimal("100"), true),
                    new AccountDataDTO(4L, "Kenzo", "PagBank", new BigDecimal("150"), false)
            );
            var expected = new PageImpl<AccountDataDTO>(accounts, defaultPageable, accounts.size());
            when(accountService.findMyAccounts(authenticatedUser.getId(), defaultPageable)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/accounts")
                    .with(authentication(authentication)));

            //Assert
            var assertedAccount1 = accounts.get(0);
            var assertedAccount2 = accounts.get(1);
            verify(accountService).findMyAccounts(authenticatedUser.getId(), defaultPageable);
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(accounts.size()))

                    .andExpect(jsonPath("$.content[0].id").value(assertedAccount1.id()))
                    .andExpect(jsonPath("$.content[0].owner").value(assertedAccount1.owner()))
                    .andExpect(jsonPath("$.content[0].account_name").value(assertedAccount1.accountName()))
                    .andExpect(jsonPath("$.content[0].current_balance").value(assertedAccount1.currentBalance()))
                    .andExpect(jsonPath("$.content[0].active").value(assertedAccount1.active()))

                    .andExpect(jsonPath("$.content[1].id").value(assertedAccount2.id()))
                    .andExpect(jsonPath("$.content[1].owner").value(assertedAccount2.owner()))
                    .andExpect(jsonPath("$.content[1].account_name").value(assertedAccount2.accountName()))
                    .andExpect(jsonPath("$.content[1].current_balance").value(assertedAccount2.currentBalance()))
                    .andExpect(jsonPath("$.content[1].active").value(assertedAccount2.active()));
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário não tem contas")
        void shouldReturnEmptyPageWhenUserDoesNotHaveAccounts() throws Exception {
            //Arrange
            var userId = authenticatedUser.getId();
            var defaultPageable = PageRequest.of(0, 20);
            var expected = new PageImpl<AccountDataDTO>(List.of(), defaultPageable, 0);
            when(accountService.findMyAccounts(userId, defaultPageable)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/accounts")
                    .with(authentication(authentication))
            );

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }
    }

    @Nested
    @DisplayName("DELETE /accounts/{accountId}")
    class DeleteMyAccount {
        @Test
        @DisplayName("Deleta conta do usuário informado")
        void shouldDeleteUserAccount() throws Exception {
            //Arrange
            var accountId = 1L;
            var userId = authenticatedUser.getId();
            doNothing().when(accountService).hardDelete(accountId, userId);

            //Act
            var response = mockMvc.perform(delete("/accounts/{accountId}", accountId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isNoContent());
            verify(accountService).hardDelete(accountId, userId);
        }

        @Test
        @DisplayName("Retorna 404 quando conta não encontrada para exclusão")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            //Arrange
            var accountId = 1L;
            var userId = authenticatedUser.getId();
            doThrow(new ResourceNotFoundException("Account id not found for hard delete"))
                    .when(accountService).hardDelete(accountId, userId);
            //Act
            var response = mockMvc.perform(delete("/accounts/{accountId}", accountId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /accounts/{accountId}/deactivate")
    class DeactivateMyAccount {
        @Test
        @DisplayName("Desativa conta do usuário informado")
        void shouldDeactivateUserAccount() throws Exception {
            //Arrange
            var accountId = 1L;
            var userId = authenticatedUser.getId();
            var expected = new AccountDataDTO(accountId, authenticatedUser.getName(), "Nubank", new BigDecimal("100"), false);
            when(accountService.deactivate(accountId, userId)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(patch("/accounts/{accountId}/deactivate", accountId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
            verify(accountService).deactivate(accountId, userId);
        }

        @Test
        @DisplayName("Retorna 404 quando conta não encontrada para desativação")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            //Arrange
            var accountId = 1L;
            var userId = authenticatedUser.getId();
            when(accountService.deactivate(accountId,userId)).thenThrow(new ResourceNotFoundException("Account id not found for deactivate"));

            //Act
            var response = mockMvc.perform(patch("/accounts/{accountId}/deactivate", accountId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Retorna 422 quando conta já está inativa")
        void shouldReturn422WhenAccountAlreadyInactive() throws Exception{
            //Arrange
            var accountId = 1L;
            var userId = authenticatedUser.getId();
            when(accountService.deactivate(accountId,userId)).thenThrow(new BusinessRuleException("Account is already inactive"));

            //Act
            var response = mockMvc.perform(patch("/accounts/{accountId}/deactivate",accountId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isUnprocessableContent());
        }
    }

    @Nested
    @DisplayName("PATCH /accounts/{accountId}/activate")
    class ActivateMyAccount {
        @Test
        @DisplayName("Ativa conta do usuário informado")
        void shouldActivateUserAccount() throws Exception {
            //Arrange
            var accountId = 1L;
            var userId = authenticatedUser.getId();
            var expected = new AccountDataDTO(accountId, authenticatedUser.getName(), "Nubank", new BigDecimal("100"), true);
            when(accountService.activate(accountId, userId)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(patch("/accounts/{accountId}/activate", accountId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true));
            verify(accountService).activate(accountId, userId);

        }

        @Test
        @DisplayName("Retorna 404 quando conta não encontrada para ativação")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            //Arrange
            var accountId = 1L;
            var userId = authenticatedUser.getId();
            when(accountService.activate(accountId, userId)).thenThrow(new ResourceNotFoundException("Account id not found for deactivate"));

            //Act
            var response = mockMvc.perform(patch("/accounts/{accountId}/activate", accountId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Retorna 422 quando conta já está ativa")
        void shouldReturn422WhenAccountAlreadyActive() throws Exception{
            //Arrange
            var accountId = 1L;
            var userId = authenticatedUser.getId();
            when(accountService.activate(accountId,userId)).thenThrow(new BusinessRuleException("Account is already inactive"));

            //Act
            var response = mockMvc.perform(patch("/accounts/{accountId}/activate",accountId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isUnprocessableContent());
        }
    }

}
