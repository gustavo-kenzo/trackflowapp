package com.gustavo.trackflowapp.modules.transaction;

import com.gustavo.trackflowapp.infra.security.SecurityFilter;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionDataDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionRegisterDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionSettleDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionUpdateDTO;
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
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TransactionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
class TransactionControllerTest {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    private User authenticatedUser;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setup() {
        authenticatedUser = new User("Kenzo", "kenzo@email.com", "123456");
        ReflectionTestUtils.setField(authenticatedUser, "id", 1L);
        authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
    }

    @Nested
    @DisplayName("POST /transactions")
    class CreateTransaction {
        @Test
        @DisplayName("Cria transação com sucesso e retorna dados")
        void shouldCreateTransactionAndReturnDataDto() throws Exception {
            //Arrange
            var registerDTO = new TransactionRegisterDTO(1L, 2L, new BigDecimal("50"), TransactionType.EXPENSE, LocalDate.now(), null, "Compra");
            var expected = new TransactionDataDTO(1L, authenticatedUser.getId(), registerDTO.accountId(), registerDTO.categoryId(), null,
                    registerDTO.amount(), registerDTO.type(), TransactionStatus.PENDING, registerDTO.competenceDate(), null, registerDTO.description());
            when(transactionService.create(registerDTO, authenticatedUser)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(post("/transactions")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(registerDTO))
            );

            //Assert
            verify(transactionService).create(registerDTO, authenticatedUser);
            response.andExpect(header().string("Location", endsWith("transactions/1")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.transaction_id").value(1L))
                    .andExpect(jsonPath("$.account_id").value(registerDTO.accountId()))
                    .andExpect(jsonPath("$.category_id").value(registerDTO.categoryId()))
                    .andExpect(jsonPath("$.amount").value(registerDTO.amount()))
                    .andExpect(jsonPath("$.type").value(registerDTO.type().toString()))
                    .andExpect(jsonPath("$.status").value(TransactionStatus.PENDING.toString()))
                    .andExpect(jsonPath("$.description").value(registerDTO.description()));
        }

        @Test
        @DisplayName("Deve retornar 400 quando houver registro de campos inválidos")
        void shouldReturn400WhenThereIsInvalidFieldsRegister() throws Exception {
            //Arrange
            var json = """
                    {
                        "account_id":null,
                        "amount":"50",
                        "type":"EXPENSE",
                        "competence_date":"2025-01-01"
                    }
                    """;

            //Act
            var response = mockMvc.perform(post("/transactions")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            );

            //Assert
            response.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.field").value("accountId"))
                    .andExpect(jsonPath("$.message").exists());
            verifyNoInteractions(transactionService);
        }

        @Test
        @DisplayName("Deve retornar 422 quando conta estiver inativa")
        void shouldReturn422WhenAccountIsInactive() throws Exception {
            //Arrange
            var registerDTO = new TransactionRegisterDTO(1L, 2L, new BigDecimal("50"), TransactionType.EXPENSE, LocalDate.now(), null, "Compra");
            when(transactionService.create(registerDTO, authenticatedUser))
                    .thenThrow(new BusinessRuleException("This account is inactive. Active first to submit transactions"));

            //Act
            var response = mockMvc.perform(post("/transactions")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(registerDTO))
            );

            //Assert
            response.andExpect(status().isUnprocessableContent());
        }
    }

    @Nested
    @DisplayName("PATCH /transactions/settle")
    class SettleTransaction {
        @Test
        @DisplayName("Liquida transação com sucesso")
        void shouldSettleTransaction() throws Exception {
            //Arrange
            var dto = new TransactionSettleDTO(1L, LocalDate.now());
            var expected = new TransactionDataDTO(1L, authenticatedUser.getId(), 2L, null, null,
                    new BigDecimal("50"), TransactionType.EXPENSE, TransactionStatus.SETTLED, LocalDate.now(), dto.settlementDate(), "Compra");
            when(transactionService.settleTransaction(dto, authenticatedUser)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(patch("/transactions/settle")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(TransactionStatus.SETTLED.toString()));
            verify(transactionService).settleTransaction(dto, authenticatedUser);
        }

        @Test
        @DisplayName("Retorna 404 quando transação não encontrada")
        void shouldReturn404WhenTransactionNotFound() throws Exception {
            //Arrange
            var dto = new TransactionSettleDTO(1L, LocalDate.now());
            when(transactionService.settleTransaction(dto, authenticatedUser))
                    .thenThrow(new ResourceNotFoundException("transaction not found"));

            //Act
            var response = mockMvc.perform(patch("/transactions/settle")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Retorna 422 quando transação já está liquidada")
        void shouldReturn422WhenTransactionAlreadySettled() throws Exception {
            //Arrange
            var dto = new TransactionSettleDTO(1L, LocalDate.now());
            when(transactionService.settleTransaction(dto, authenticatedUser))
                    .thenThrow(new BusinessRuleException("transaction already settled"));

            //Act
            var response = mockMvc.perform(patch("/transactions/settle")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isUnprocessableContent());
        }
    }

    @Nested
    @DisplayName("PATCH /transactions/reopen")
    class ReopenTransaction {
        @Test
        @DisplayName("Reabre transação com sucesso")
        void shouldReopenTransaction() throws Exception {
            //Arrange
            var dto = new TransactionSettleDTO(1L, null);
            var expected = new TransactionDataDTO(1L, authenticatedUser.getId(), 2L, null, null,
                    new BigDecimal("50"), TransactionType.EXPENSE, TransactionStatus.PENDING, LocalDate.now(), null, "Compra");
            when(transactionService.reopenTransaction(dto, authenticatedUser)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(patch("/transactions/reopen")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(TransactionStatus.PENDING.toString()));
            verify(transactionService).reopenTransaction(dto, authenticatedUser);
        }

        @Test
        @DisplayName("Retorna 404 quando transação não encontrada")
        void shouldReturn404WhenTransactionNotFound() throws Exception {
            //Arrange
            var dto = new TransactionSettleDTO(1L, null);
            when(transactionService.reopenTransaction(dto, authenticatedUser))
                    .thenThrow(new ResourceNotFoundException("transaction not found"));

            //Act
            var response = mockMvc.perform(patch("/transactions/reopen")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Retorna 422 quando transação já está pendente")
        void shouldReturn422WhenTransactionAlreadyPending() throws Exception {
            //Arrange
            var dto = new TransactionSettleDTO(1L, null);
            when(transactionService.reopenTransaction(dto, authenticatedUser))
                    .thenThrow(new BusinessRuleException("transaction already pending"));

            //Act
            var response = mockMvc.perform(patch("/transactions/reopen")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isUnprocessableContent());
        }
    }

    @Nested
    @DisplayName("PUT /transactions")
    class UpdateTransaction {
        @Test
        @DisplayName("Atualiza transação com sucesso")
        void shouldUpdateTransaction() throws Exception {
            //Arrange
            var dto = new TransactionUpdateDTO(1L, 2L, 3L, new BigDecimal("80"), TransactionType.INCOME, LocalDate.now(), "nova descrição");
            var expected = new TransactionDataDTO(1L, authenticatedUser.getId(), dto.accountId(), dto.categoryId(), null,
                    dto.amount(), dto.type(), TransactionStatus.PENDING, dto.competenceDate(), null, dto.description());
            when(transactionService.updateTransaction(dto, authenticatedUser)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(put("/transactions")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value(dto.description()))
                    .andExpect(jsonPath("$.amount").value(dto.amount()));
            verify(transactionService).updateTransaction(dto, authenticatedUser);
        }

        @Test
        @DisplayName("Deve retornar 400 quando id não informado")
        void shouldReturn400WhenIdIsNull() throws Exception {
            //Arrange
            var json = """
                    {
                        "id":null,
                        "description":"nova descrição"
                    }
                    """;

            //Act
            var response = mockMvc.perform(put("/transactions")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            );

            //Assert
            response.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.field").value("id"))
                    .andExpect(jsonPath("$.message").exists());
            verifyNoInteractions(transactionService);
        }

        @Test
        @DisplayName("Retorna 404 quando transação não encontrada")
        void shouldReturn404WhenTransactionNotFound() throws Exception {
            //Arrange
            var dto = new TransactionUpdateDTO(1L, null, null, null, null, null, "desc");
            when(transactionService.updateTransaction(dto, authenticatedUser))
                    .thenThrow(new ResourceNotFoundException("Transaction not found"));

            //Act
            var response = mockMvc.perform(put("/transactions")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Retorna 422 quando transação liquidada tenta alterar campo não permitido")
        void shouldReturn422WhenSettledTransactionTriesToChangeForbiddenField() throws Exception {
            //Arrange
            var dto = new TransactionUpdateDTO(1L, null, null, new BigDecimal("999"), null, null, null);
            when(transactionService.updateTransaction(dto, authenticatedUser))
                    .thenThrow(new BusinessRuleException("Can only update category and description. Transaction is settled"));

            //Act
            var response = mockMvc.perform(put("/transactions")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isUnprocessableContent());
        }
    }

    @Nested
    @DisplayName("GET /transactions/active")
    class ListTransactions {
        @Test
        @DisplayName("Lista transações de contas ativas do usuário")
        void shouldListActiveTransactions() throws Exception {
            //Arrange
            var defaultPageable = PageRequest.of(0, 20);
            var transactions = List.of(
                    new TransactionDataDTO(1L, authenticatedUser.getId(), 2L, 3L, null, new BigDecimal("50"), TransactionType.EXPENSE, TransactionStatus.PENDING, LocalDate.now(), null, "Compra"),
                    new TransactionDataDTO(2L, authenticatedUser.getId(), 2L, 4L, null, new BigDecimal("100"), TransactionType.INCOME, TransactionStatus.SETTLED, LocalDate.now(), LocalDate.now(), "Recebimento")
            );
            var expected = new PageImpl<>(transactions, defaultPageable, transactions.size());
            when(transactionService.listTransactions(authenticatedUser, null, null, null, defaultPageable)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/transactions/active")
                    .with(authentication(authentication)));

            //Assert
            verify(transactionService).listTransactions(authenticatedUser, null, null, null, defaultPageable);
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(transactions.size()))
                    .andExpect(jsonPath("$.content[0].transaction_id").value(transactions.get(0).transactionId()))
                    .andExpect(jsonPath("$.content[1].transaction_id").value(transactions.get(1).transactionId()));
        }

        @Test
        @DisplayName("Repassa filtros de categoria, tipo e status para o service")
        void shouldForwardFiltersToService() throws Exception {
            //Arrange
            var defaultPageable = PageRequest.of(0, 20);
            var expected = new PageImpl<TransactionDataDTO>(List.of(), defaultPageable, 0);
            when(transactionService.listTransactions(authenticatedUser, 3L, TransactionType.EXPENSE, TransactionStatus.PENDING, defaultPageable))
                    .thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/transactions/active")
                    .param("categoryId", "3")
                    .param("type", "EXPENSE")
                    .param("status", "PENDING")
                    .with(authentication(authentication)));

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
            verify(transactionService).listTransactions(authenticatedUser, 3L, TransactionType.EXPENSE, TransactionStatus.PENDING, defaultPageable);
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário não tem transações")
        void shouldReturnEmptyPageWhenUserHasNoTransactions() throws Exception {
            //Arrange
            var defaultPageable = PageRequest.of(0, 20);
            var expected = new PageImpl<TransactionDataDTO>(List.of(), defaultPageable, 0);
            when(transactionService.listTransactions(authenticatedUser, null, null, null, defaultPageable)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/transactions/active")
                    .with(authentication(authentication)));

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /transactions/inactive")
    class ListTransactionsInactiveAccount {
        @Test
        @DisplayName("Lista transações de contas inativas do usuário")
        void shouldListInactiveAccountTransactions() throws Exception {
            //Arrange
            var defaultPageable = PageRequest.of(0, 20);
            var transactions = List.of(
                    new TransactionDataDTO(1L, authenticatedUser.getId(), 2L, 3L, null, new BigDecimal("50"), TransactionType.EXPENSE, TransactionStatus.PENDING, LocalDate.now(), null, "Compra")
            );
            var expected = new PageImpl<>(transactions, defaultPageable, transactions.size());
            when(transactionService.listTransactionsInactiveAccount(authenticatedUser, null, null, null, defaultPageable)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/transactions/inactive")
                    .with(authentication(authentication)));

            //Assert
            verify(transactionService).listTransactionsInactiveAccount(authenticatedUser, null, null, null, defaultPageable);
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].transaction_id").value(transactions.get(0).transactionId()));
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário não tem transações em contas inativas")
        void shouldReturnEmptyPageWhenUserHasNoInactiveAccountTransactions() throws Exception {
            //Arrange
            var defaultPageable = PageRequest.of(0, 20);
            var expected = new PageImpl<TransactionDataDTO>(List.of(), defaultPageable, 0);
            when(transactionService.listTransactionsInactiveAccount(authenticatedUser, null, null, null, defaultPageable)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/transactions/inactive")
                    .with(authentication(authentication)));

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }
    }

    @Nested
    @DisplayName("DELETE /transactions/{transactionId}")
    class DeleteTransaction {
        @Test
        @DisplayName("Deleta transação do usuário informado")
        void shouldDeleteUserTransaction() throws Exception {
            //Arrange
            var transactionId = 1L;
            doNothing().when(transactionService).deleteTransaction(transactionId, authenticatedUser);

            //Act
            var response = mockMvc.perform(delete("/transactions/{transactionId}", transactionId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isNoContent());
            verify(transactionService).deleteTransaction(transactionId, authenticatedUser);
        }

        @Test
        @DisplayName("Retorna 404 quando transação não encontrada para exclusão")
        void shouldReturn404WhenTransactionNotFound() throws Exception {
            //Arrange
            var transactionId = 1L;
            doThrow(new ResourceNotFoundException("Transaction id not found for hard delete"))
                    .when(transactionService).deleteTransaction(transactionId, authenticatedUser);

            //Act
            var response = mockMvc.perform(delete("/transactions/{transactionId}", transactionId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isNotFound());
        }
    }
}