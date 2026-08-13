package com.gustavo.trackflowapp.modules.transaction;

import com.gustavo.trackflowapp.modules.account.Account;
import com.gustavo.trackflowapp.modules.account.AccountRepository;
import com.gustavo.trackflowapp.modules.category.Category;
import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.modules.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TransactionRepositoryTest {

    private static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 10);

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TestEntityManager entityManager;

    private List<Transaction> transactions = new ArrayList<>();
    private User user1;
    private User user2;
    private Account activeAccountUser1;
    private Account inactiveAccountUser1;
    private Category categoryMercado;
    private Category categoryLazer;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        user1 = new User("Mark", "mark@email.com", "123456&");
        user2 = new User("Nolan", "nolan@email.com", "123456&");
        entityManager.persist(user1);
        entityManager.persist(user2);

        activeAccountUser1 = new Account(user1, "Nubank", BigDecimal.valueOf(1000));
        inactiveAccountUser1 = new Account(user1, "Bradesco", BigDecimal.valueOf(500));
        inactiveAccountUser1.deactivate();
        var accountUser2 = new Account(user2, "Itaú", BigDecimal.valueOf(700));
        entityManager.persist(activeAccountUser1);
        entityManager.persist(inactiveAccountUser1);
        entityManager.persist(accountUser2);

        categoryMercado = new Category(user1, "Mercado");
        categoryLazer = new Category(user1, "Lazer");
        entityManager.persist(categoryMercado);
        entityManager.persist(categoryLazer);
        entityManager.flush();

        var transaction1 = new Transaction(user1, activeAccountUser1, categoryMercado, null, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), null, "Compra 1");
        var transaction2 = new Transaction(user1, activeAccountUser1, categoryLazer, null, BigDecimal.valueOf(100), TransactionType.INCOME, LocalDate.now(), LocalDate.now(), "Recebimento");
        var transaction3 = new Transaction(user1, inactiveAccountUser1, categoryMercado, null, BigDecimal.valueOf(30), TransactionType.EXPENSE, LocalDate.now(), null, "Compra conta inativa");
        var transaction4 = new Transaction(user2, accountUser2, null, null, BigDecimal.valueOf(200), TransactionType.EXPENSE, LocalDate.now(), null, "Compra user2");

        transactions.addAll(List.of(transaction1, transaction2, transaction3, transaction4));
        transactions.forEach(entityManager::persist);
        entityManager.flush();
    }

    @Nested
    @DisplayName("findMyTransactionById()")
    class FindMyTransactionById {
        @Test
        @DisplayName("Retorna transação pertencente ao usuário informado")
        void shouldReturnTransactionBelongingToUser() {
            //Arrange
            var expected = transactions.get(0);

            //Act
            var result = transactionRepository.findMyTransactionById(expected.getId(), user1.getId());

            //Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(expected.getId());
        }

        @Test
        @DisplayName("Retorna vazio quando transação não existe")
        void shouldReturnEmptyWhenTransactionDoesNotExist() {
            //Act
            var result = transactionRepository.findMyTransactionById(9999L, user1.getId());

            //Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Retorna vazio quando transação não pertence ao usuário")
        void shouldReturnEmptyWhenTransactionDoesNotBelongToUser() {
            //Arrange
            var transactionFromUser2 = transactions.get(3);

            //Act
            var result = transactionRepository.findMyTransactionById(transactionFromUser2.getId(), user1.getId());

            //Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findMyTransactions()")
    class FindMyTransactions {
        @Test
        @DisplayName("Retorna apenas transações de contas ativas do usuário")
        void shouldReturnOnlyTransactionsFromActiveAccounts() {
            //Act
            var result = transactionRepository.findMyTransactions(user1.getId(), null, null, null, DEFAULT_PAGE);

            //Assert
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(Transaction::getId)
                    .containsExactlyInAnyOrder(transactions.get(0).getId(), transactions.get(1).getId());
        }

        @Test
        @DisplayName("Filtra por categoria quando informado")
        void shouldFilterByCategory() {
            //Act
            var result = transactionRepository.findMyTransactions(user1.getId(), categoryLazer.getId(), null, null, DEFAULT_PAGE);

            //Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(transactions.get(1).getId());
        }

        @Test
        @DisplayName("Filtra por tipo quando informado")
        void shouldFilterByType() {
            //Act
            var result = transactionRepository.findMyTransactions(user1.getId(), null, TransactionType.INCOME, null, DEFAULT_PAGE);

            //Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().getType()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("Filtra por status quando informado")
        void shouldFilterByStatus() {
            //Act
            var result = transactionRepository.findMyTransactions(user1.getId(), null, null, TransactionStatus.SETTLED, DEFAULT_PAGE);

            //Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().getStatus()).isEqualTo(TransactionStatus.SETTLED);
        }

        @Test
        @DisplayName("Não retorna transações de outro usuário")
        void shouldNotReturnTransactionsFromAnotherUser() {
            //Act
            var result = transactionRepository.findMyTransactions(user1.getId(), null, null, null, DEFAULT_PAGE);

            //Assert
            assertThat(result.getContent()).extracting(Transaction::getId)
                    .doesNotContain(transactions.get(3).getId());
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário não tem transações em contas ativas")
        void shouldReturnEmptyPageWhenUserHasNoTransactionsInActiveAccounts() {
            //Arrange
            var userWithoutTransactions = new User("Anna", "anna@email.com", "123456&");
            entityManager.persistAndFlush(userWithoutTransactions);

            //Act
            var result = transactionRepository.findMyTransactions(userWithoutTransactions.getId(), null, null, null, DEFAULT_PAGE);

            //Assert
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("findMyTransactionsInactive()")
    class FindMyTransactionsInactive {
        @Test
        @DisplayName("Retorna apenas transações de contas inativas do usuário")
        void shouldReturnOnlyTransactionsFromInactiveAccounts() {
            //Act
            var result = transactionRepository.findMyTransactionsInactive(user1.getId(), null, null, null, DEFAULT_PAGE);

            //Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(transactions.get(2).getId());
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário não tem transações em contas inativas")
        void shouldReturnEmptyPageWhenUserHasNoTransactionsInInactiveAccounts() {
            //Act
            var result = transactionRepository.findMyTransactionsInactive(user2.getId(), null, null, null, DEFAULT_PAGE);

            //Assert
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Filtra transações inativas por categoria, tipo e status")
        void shouldFilterInactiveTransactionsByCategoryTypeAndStatus() {
            //Act
            var result = transactionRepository.findMyTransactionsInactive(user1.getId(), categoryMercado.getId(), TransactionType.EXPENSE, TransactionStatus.PENDING, DEFAULT_PAGE);

            //Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(transactions.get(2).getId());
        }
    }

    @Nested
    @DisplayName("deleteMyTransaction()")
    class DeleteMyTransaction {
        @Test
        @DisplayName("Deleta transação do usuário e retorna linhas alteradas")
        void shouldDeleteUserTransactionAndReturnRowsAffected() {
            //Arrange
            var transactionId = transactions.get(0).getId();

            //Act
            var result = transactionRepository.deleteMyTransaction(transactionId, user1.getId());

            //Assert
            assertThat(result).isEqualTo(1);
            assertThat(transactionRepository.findMyTransactionById(transactionId, user1.getId())).isEmpty();
        }

        @Test
        @DisplayName("Retorna 0 quando transação não existe")
        void shouldReturnZeroWhenTransactionDoesNotExist() {
            //Act
            var result = transactionRepository.deleteMyTransaction(9999L, user1.getId());

            //Assert
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("Retorna 0 quando transação não pertence ao usuário")
        void shouldReturnZeroWhenTransactionDoesNotBelongToUser() {
            //Arrange
            var transactionFromUser2 = transactions.get(3).getId();

            //Act
            var result = transactionRepository.deleteMyTransaction(transactionFromUser2, user1.getId());

            //Assert
            assertThat(result).isEqualTo(0);
            assertThat(transactionRepository.findMyTransactionById(transactionFromUser2, user2.getId())).isPresent();
        }
    }
}