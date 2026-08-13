package com.gustavo.trackflowapp.modules.transaction;

import com.gustavo.trackflowapp.modules.account.Account;
import com.gustavo.trackflowapp.modules.account.AccountService;
import com.gustavo.trackflowapp.modules.category.Category;
import com.gustavo.trackflowapp.modules.category.CategoryService;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionRegisterDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionSettleDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionUpdateDTO;
import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.exception.BusinessRuleException;
import com.gustavo.trackflowapp.shared.exception.ResourceNotFoundException;
import org.aspectj.util.Reflection;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Observação: a funcionalidade de Recurrence ainda está em desenvolvimento. Nas transações
// criadas manualmente abaixo, o parâmetro de recurrence é sempre passado como null apenas
// para satisfazer o construtor de Transaction — sem relevância para os cenários testados aqui.
@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Service")
class TransactionServiceTest {

    private static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 10);

    @Mock
    AccountService accountService;
    @Mock
    CategoryService categoryService;
    @Mock
    TransactionRepository transactionRepository;
    @InjectMocks
    TransactionService transactionService;

    private User user;
    private Account account;
    private Category category;
    private Transaction transaction;

    @BeforeEach
    void setup() {
        user = new User("Kenzo", "kenzo@email.com", "123456");
        // Entidade não persistida tem id nulo por padrão; setamos manualmente pois
        // validateOwnership() em Transaction compara owner.getId().equals(user.getId()).
        ReflectionTestUtils.setField(user, "id", 1L);
        account = new Account(user, "Nubank", BigDecimal.valueOf(100));
        category = new Category(user, "Mercado");
        // Entidades não persistidas têm id nulo por padrão; setamos manualmente pois os testes
        // usam account.getId()/category.getId() para montar os DTOs, e o service só chama
        // accountService/categoryService quando esses ids não são nulos.
        ReflectionTestUtils.setField(account, "id", 10L);
        ReflectionTestUtils.setField(category, "id", 20L);
        transaction = new Transaction(user, account, category, null, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), null, "Compra");
    }

    @Nested
    @DisplayName("create()")
    class Create {
        @Test
        @DisplayName("Cria transação PENDING sem afetar saldo da conta")
        void shouldCreatePendingTransactionWithoutAffectingAccountBalance() {
            //Arrange
            var dto = new TransactionRegisterDTO(account.getId(), category.getId(), BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), null, "Compra");
            var captor = ArgumentCaptor.forClass(Transaction.class);
            var balanceBefore = account.getCurrentBalance();
            when(accountService.getAccount(dto.accountId(), user.getId())).thenReturn(account);
            when(categoryService.getCategory(dto.categoryId(), user.getId())).thenReturn(category);

            //Act
            var result = transactionService.create(dto, user);

            //Assert
            verify(accountService).getAccount(dto.accountId(), user.getId());
            verify(categoryService).getCategory(dto.categoryId(), user.getId());
            verify(transactionRepository).save(captor.capture());
            verifyNoMoreInteractions(transactionRepository);

            assertThat(result.amount()).isEqualByComparingTo(dto.amount());
            assertThat(result.type()).isEqualTo(dto.type());
            assertThat(result.status()).isEqualTo(TransactionStatus.PENDING);
            assertThat(account.getCurrentBalance()).isEqualByComparingTo(balanceBefore);
        }

        @Test
        @DisplayName("Cria transação SETTLED do tipo INCOME e credita a conta")
        void shouldCreateSettledIncomeTransactionAndCreditAccount() {
            //Arrange
            var dto = new TransactionRegisterDTO(account.getId(), null, BigDecimal.valueOf(50), TransactionType.INCOME, LocalDate.now(), LocalDate.now(), "Recebimento");
            when(accountService.getAccount(dto.accountId(), user.getId())).thenReturn(account);

            //Act
            var result = transactionService.create(dto, user);

            //Assert
            verify(accountService).getAccount(dto.accountId(), user.getId());
            verify(transactionRepository).save(any(Transaction.class));
            verifyNoInteractions(categoryService);

            assertThat(result.status()).isEqualTo(TransactionStatus.SETTLED);
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("150");
        }

        @Test
        @DisplayName("Cria transação SETTLED do tipo EXPENSE e debita a conta")
        void shouldCreateSettledExpenseTransactionAndDebitAccount() {
            //Arrange
            var dto = new TransactionRegisterDTO(account.getId(), null, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), LocalDate.now(), "Compra");
            when(accountService.getAccount(dto.accountId(), user.getId())).thenReturn(account);

            //Act
            var result = transactionService.create(dto, user);

            //Assert
            verify(transactionRepository).save(any(Transaction.class));
            assertThat(result.status()).isEqualTo(TransactionStatus.SETTLED);
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando conta está inativa")
        void shouldThrowWhenAccountIsInactive() {
            //Arrange
            account.deactivate();
            var dto = new TransactionRegisterDTO(account.getId(), null, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), null, "Compra");
            when(accountService.getAccount(dto.accountId(), user.getId())).thenReturn(account);

            //Act & Assert
            assertThatThrownBy(() -> transactionService.create(dto, user))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("This account is inactive. Active first to submit transactions");
            verify(accountService).getAccount(dto.accountId(), user.getId());
            verifyNoInteractions(transactionRepository, categoryService);
        }

        @Test
        @DisplayName("Não busca categoria quando categoryId não é informado")
        void shouldNotFetchCategoryWhenCategoryIdIsNull() {
            //Arrange
            var dto = new TransactionRegisterDTO(account.getId(), null, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), null, "Compra");
            when(accountService.getAccount(dto.accountId(), user.getId())).thenReturn(account);

            //Act
            transactionService.create(dto, user);

            //Assert
            verifyNoInteractions(categoryService);
        }
    }

    @Nested
    @DisplayName("settleTransaction()")
    class SettleTransaction {
        @Test
        @DisplayName("Liquida transação INCOME e credita a conta")
        void shouldSettleIncomeTransactionAndCreditAccount() {
            //Arrange
            var incomeTransaction = new Transaction(user, account, category, null, BigDecimal.valueOf(50), TransactionType.INCOME, LocalDate.now(), null, "Recebimento");
            var dto = new TransactionSettleDTO(1L, LocalDate.now());
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.of(incomeTransaction));

            //Act
            var result = transactionService.settleTransaction(dto, user);

            //Assert
            verify(transactionRepository).findMyTransactionById(dto.id(), user.getId());
            verifyNoMoreInteractions(transactionRepository);
            assertThat(result.status()).isEqualTo(TransactionStatus.SETTLED);
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("150");
        }

        @Test
        @DisplayName("Liquida transação EXPENSE e debita a conta")
        void shouldSettleExpenseTransactionAndDebitAccount() {
            //Arrange
            var dto = new TransactionSettleDTO(1L, LocalDate.now());
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.of(transaction));

            //Act
            var result = transactionService.settleTransaction(dto, user);

            //Assert
            assertThat(result.status()).isEqualTo(TransactionStatus.SETTLED);
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando transação não encontrada")
        void shouldThrowWhenTransactionNotFound() {
            //Arrange
            var dto = new TransactionSettleDTO(1L, LocalDate.now());
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.empty());

            //Act & Assert
            assertThatThrownBy(() -> transactionService.settleTransaction(dto, user))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("transaction not found");
            verify(transactionRepository).findMyTransactionById(dto.id(), user.getId());
            verifyNoMoreInteractions(transactionRepository);
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando conta da transação está inativa")
        void shouldThrowWhenAccountIsInactive() {
            //Arrange
            account.deactivate();
            var dto = new TransactionSettleDTO(1L, LocalDate.now());
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.of(transaction));

            //Act & Assert
            assertThatThrownBy(() -> transactionService.settleTransaction(dto, user))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("This account is inactive. Active first to submit transactions");
        }
    }

    @Nested
    @DisplayName("reopenTransaction()")
    class ReopenTransaction {
        @Test
        @DisplayName("Reabre transação INCOME liquidada e debita a conta")
        void shouldReopenIncomeTransactionAndDebitAccount() {
            //Arrange
            var incomeTransaction = new Transaction(user, account, category, null, BigDecimal.valueOf(50), TransactionType.INCOME, LocalDate.now(), LocalDate.now(), "Recebimento");
            var dto = new TransactionSettleDTO(1L, null);
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.of(incomeTransaction));

            //Act
            var result = transactionService.reopenTransaction(dto, user);

            //Assert
            assertThat(result.status()).isEqualTo(TransactionStatus.PENDING);
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("Reabre transação EXPENSE liquidada e credita a conta")
        void shouldReopenExpenseTransactionAndCreditAccount() {
            //Arrange
            var settledTransaction = new Transaction(user, account, category, null, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), LocalDate.now(), "Compra");
            var dto = new TransactionSettleDTO(1L, null);
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.of(settledTransaction));

            //Act
            var result = transactionService.reopenTransaction(dto, user);

            //Assert
            assertThat(result.status()).isEqualTo(TransactionStatus.PENDING);
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("150");
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando transação não encontrada")
        void shouldThrowWhenTransactionNotFound() {
            //Arrange
            var dto = new TransactionSettleDTO(1L, null);
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.empty());

            //Act & Assert
            assertThatThrownBy(() -> transactionService.reopenTransaction(dto, user))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("transaction not found");
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando conta da transação está inativa")
        void shouldThrowWhenAccountIsInactive() {
            //Arrange
            var settledTransaction = new Transaction(user, account, category, null, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), LocalDate.now(), "Compra");
            account.deactivate();
            var dto = new TransactionSettleDTO(1L, null);
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.of(settledTransaction));

            //Act & Assert
            assertThatThrownBy(() -> transactionService.reopenTransaction(dto, user))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("This account is inactive. Active first to submit transactions");
        }
    }

    @Nested
    @DisplayName("updateTransaction()")
    class UpdateTransaction {
        @Test
        @DisplayName("Atualiza todos os campos de transação PENDING")
        void shouldUpdateAllFieldsOfPendingTransaction() {
            //Arrange
            var newAccount = new Account(user, "PagBank", BigDecimal.valueOf(10));
            ReflectionTestUtils.setField(newAccount,"id",2L);
            var newCategory = new Category(user, "Lazer");
            ReflectionTestUtils.setField(newCategory,"id",3L);
            var dto = new TransactionUpdateDTO(1L, newAccount.getId(), newCategory.getId(), BigDecimal.valueOf(80), TransactionType.INCOME, LocalDate.now(), "nova descrição");
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.of(transaction));
            when(accountService.getAccount(dto.accountId(), user.getId())).thenReturn(newAccount);
            when(categoryService.getCategory(dto.categoryId(), user.getId())).thenReturn(newCategory);

            //Act
            var result = transactionService.updateTransaction(dto, user);

            //Assert
            verify(transactionRepository).findMyTransactionById(dto.id(), user.getId());
            verify(accountService).getAccount(dto.accountId(), user.getId());
            verify(categoryService).getCategory(dto.categoryId(), user.getId());
            verifyNoMoreInteractions(transactionRepository);

            assertThat(result.accountId()).isEqualTo(newAccount.getId());
            assertThat(result.categoryId()).isEqualTo(newCategory.getId());
            assertThat(result.amount()).isEqualByComparingTo("80");
            assertThat(result.type()).isEqualTo(TransactionType.INCOME);
            assertThat(result.description()).isEqualTo("nova descrição");
        }

        @Test
        @DisplayName("Atualiza somente categoria e descrição de transação SETTLED")
        void shouldUpdateOnlyCategoryAndDescriptionOfSettledTransaction() {
            //Arrange
            var settledTransaction = new Transaction(user, account, category, null, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), LocalDate.now(), "Compra");
            var newCategory = new Category(user, "Lazer");
            ReflectionTestUtils.setField(newCategory,"id",2L);
            var dto = new TransactionUpdateDTO(1L, null, newCategory.getId(), null, null, null, "nova descrição");
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.of(settledTransaction));
            when(categoryService.getCategory(dto.categoryId(), user.getId())).thenReturn(newCategory);

            //Act
            var result = transactionService.updateTransaction(dto, user);

            //Assert
            verifyNoInteractions(accountService);
            assertThat(result.categoryId()).isEqualTo(newCategory.getId());
            assertThat(result.description()).isEqualTo("nova descrição");
            assertThat(result.amount()).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("Lança BusinessRuleException ao tentar alterar amount de transação SETTLED")
        void shouldThrowWhenTryingToChangeAmountOfSettledTransaction() {
            //Arrange
            var settledTransaction = new Transaction(user, account, category, null, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), LocalDate.now(), "Compra");
            var dto = new TransactionUpdateDTO(1L, null, null, BigDecimal.valueOf(999), null, null, null);
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.of(settledTransaction));

            //Act & Assert
            assertThatThrownBy(() -> transactionService.updateTransaction(dto, user))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Can only update category and description. Transaction is settled");
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando transação não encontrada")
        void shouldThrowWhenTransactionNotFound() {
            //Arrange
            var dto = new TransactionUpdateDTO(1L, null, null, null, null, null, "desc");
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.empty());

            //Act & Assert
            assertThatThrownBy(() -> transactionService.updateTransaction(dto, user))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transaction not found");
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando conta da transação está inativa")
        void shouldThrowWhenAccountIsInactive() {
            //Arrange
            account.deactivate();
            var dto = new TransactionUpdateDTO(1L, null, null, null, null, null, "desc");
            when(transactionRepository.findMyTransactionById(dto.id(), user.getId())).thenReturn(Optional.of(transaction));

            //Act & Assert
            assertThatThrownBy(() -> transactionService.updateTransaction(dto, user))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("This account is inactive. Active first to submit transactions");
        }
    }

    @Nested
    @DisplayName("listTransactions()")
    class ListTransactions {
        @Test
        @DisplayName("Retorna transações paginadas de contas ativas")
        void shouldReturnPaginatedTransactionsFromActiveAccounts() {
            //Arrange
            var transactionPage = new PageImpl<>(List.of(transaction), DEFAULT_PAGE, 1);
            when(transactionRepository.findMyTransactions(user.getId(), null, null, null, DEFAULT_PAGE)).thenReturn(transactionPage);

            //Act
            var result = transactionService.listTransactions(user, null, null, null, DEFAULT_PAGE);

            //Assert
            verify(transactionRepository).findMyTransactions(user.getId(), null, null, null, DEFAULT_PAGE);
            verifyNoMoreInteractions(transactionRepository);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().transactionId()).isEqualTo(transaction.getId());
        }

        @Test
        @DisplayName("Retorna page vazia quando não há transações")
        void shouldReturnEmptyPageWhenThereAreNoTransactions() {
            //Arrange
            when(transactionRepository.findMyTransactions(user.getId(), null, null, null, DEFAULT_PAGE)).thenReturn(Page.empty(DEFAULT_PAGE));

            //Act
            var result = transactionService.listTransactions(user, null, null, null, DEFAULT_PAGE);

            //Assert
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("listTransactionsInactiveAccount()")
    class ListTransactionsInactiveAccount {
        @Test
        @DisplayName("Retorna transações paginadas de contas inativas")
        void shouldReturnPaginatedTransactionsFromInactiveAccounts() {
            //Arrange
            var transactionPage = new PageImpl<>(List.of(transaction), DEFAULT_PAGE, 1);
            when(transactionRepository.findMyTransactionsInactive(user.getId(), null, null, null, DEFAULT_PAGE)).thenReturn(transactionPage);

            //Act
            var result = transactionService.listTransactionsInactiveAccount(user, null, null, null, DEFAULT_PAGE);

            //Assert
            verify(transactionRepository).findMyTransactionsInactive(user.getId(), null, null, null, DEFAULT_PAGE);
            verifyNoMoreInteractions(transactionRepository);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Retorna page vazia quando não há transações em contas inativas")
        void shouldReturnEmptyPageWhenThereAreNoTransactionsInInactiveAccounts() {
            //Arrange
            when(transactionRepository.findMyTransactionsInactive(user.getId(), null, null, null, DEFAULT_PAGE)).thenReturn(Page.empty(DEFAULT_PAGE));

            //Act
            var result = transactionService.listTransactionsInactiveAccount(user, null, null, null, DEFAULT_PAGE);

            //Assert
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("deleteTransaction()")
    class DeleteTransaction {
        @Test
        @DisplayName("Exclui transação PENDING sem alterar saldo da conta")
        void shouldDeletePendingTransactionWithoutChangingAccountBalance() {
            //Arrange
            var transactionId = 1L;
            var balanceBefore = account.getCurrentBalance();
            when(transactionRepository.findMyTransactionById(transactionId, user.getId())).thenReturn(Optional.of(transaction));
            when(transactionRepository.deleteMyTransaction(transactionId, user.getId())).thenReturn(1);

            //Act & Assert
            assertThatNoException().isThrownBy(() -> transactionService.deleteTransaction(transactionId, user));
            verify(transactionRepository).findMyTransactionById(transactionId, user.getId());
            verify(transactionRepository).deleteMyTransaction(transactionId, user.getId());
            verifyNoMoreInteractions(transactionRepository);
            assertThat(account.getCurrentBalance()).isEqualByComparingTo(balanceBefore);
        }

        @Test
        @DisplayName("Exclui transação EXPENSE liquidada e credita de volta a conta")
        void shouldDeleteSettledExpenseTransactionAndCreditAccountBack() {
            //Arrange
            var transactionId = 1L;
            var settledTransaction = new Transaction(user, account, category, null, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), LocalDate.now(), "Compra");
            when(transactionRepository.findMyTransactionById(transactionId, user.getId())).thenReturn(Optional.of(settledTransaction));
            when(transactionRepository.deleteMyTransaction(transactionId, user.getId())).thenReturn(1);

            //Act
            transactionService.deleteTransaction(transactionId, user);

            //Assert
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("150");
        }

        @Test
        @DisplayName("Exclui transação INCOME liquidada e debita a conta")
        void shouldDeleteSettledIncomeTransactionAndDebitAccount() {
            //Arrange
            var transactionId = 1L;
            var settledTransaction = new Transaction(user, account, category, null, BigDecimal.valueOf(50), TransactionType.INCOME, LocalDate.now(), LocalDate.now(), "Recebimento");
            when(transactionRepository.findMyTransactionById(transactionId, user.getId())).thenReturn(Optional.of(settledTransaction));
            when(transactionRepository.deleteMyTransaction(transactionId, user.getId())).thenReturn(1);

            //Act
            transactionService.deleteTransaction(transactionId, user);

            //Assert
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando transação não encontrada")
        void shouldThrowWhenTransactionNotFound() {
            //Arrange
            var transactionId = 1L;
            when(transactionRepository.findMyTransactionById(transactionId, user.getId())).thenReturn(Optional.empty());

            //Act & Assert
            assertThatThrownBy(() -> transactionService.deleteTransaction(transactionId, user))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transaction not found");
            verify(transactionRepository).findMyTransactionById(transactionId, user.getId());
            verifyNoMoreInteractions(transactionRepository);
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando nenhuma linha é afetada na exclusão")
        void shouldThrowWhenNoRowsAffectedOnDelete() {
            //Arrange
            var transactionId = 1L;
            when(transactionRepository.findMyTransactionById(transactionId, user.getId())).thenReturn(Optional.of(transaction));
            when(transactionRepository.deleteMyTransaction(transactionId, user.getId())).thenReturn(0);

            //Act & Assert
            assertThatThrownBy(() -> transactionService.deleteTransaction(transactionId, user))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transaction id not found for hard delete");
            verify(transactionRepository).findMyTransactionById(transactionId, user.getId());
            verify(transactionRepository).deleteMyTransaction(transactionId, user.getId());
            verifyNoMoreInteractions(transactionRepository);
        }
    }
}