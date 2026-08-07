package com.gustavo.trackflowapp.modules.account;

import com.gustavo.trackflowapp.integration.container.AbstractIntegrationContainer;
import com.gustavo.trackflowapp.modules.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest extends AbstractIntegrationContainer {

    private static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 10);

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TestEntityManager entityManager;

    private List<Account> accounts = new ArrayList<>();
    private User user1;
    private User user2;

    @BeforeEach
    void setup() {
        user1 = new User("Mark", "mark@email.com", "123456&");
        user2 = new User("Nolan", "nolan@email.com", "123456&");
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        var account1 = new Account(user1, "Nubank", BigDecimal.valueOf(1075));
        var account2 = new Account(user1, "Bradesco", BigDecimal.valueOf(400));
        var account3 = new Account(user1, "Itaú", null);
        var account4 = new Account(user2, "Nubank", BigDecimal.valueOf(780));
        var account5 = new Account(user2, "Brasil", BigDecimal.valueOf(1560));
        accounts.addAll(List.of(account1, account2, account3, account4, account5));
        accountRepository.saveAll(accounts);
    }

    @Nested
    @DisplayName("findMyAccounts()")
    class FindMyAccounts {
        @Test
        @DisplayName("Retorna apenas as contas pertencentes ao usuário informado")
        void shouldReturnOnlyAccountsBelongingToUser() {
            //Arrange
            var userId = user1.getId();
            var expectedAccountsIds = Stream.of(accounts.get(0), accounts.get(1), accounts.get(2)).map(Account::getId).toList();

            //Act
            var result = accountRepository.findMyAccounts(userId, DEFAULT_PAGE);

            //Assert
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent()).extracting(Account::getId)
                    .containsExactlyInAnyOrderElementsOf(expectedAccountsIds);
            assertThat(result.getContent())
                    .allMatch(account -> account.getUser().getId().equals(userId));
        }

        @Test
        @DisplayName("Não deve retornar contas de outros usuários")
        void shouldNotReturnAccountsFromAnotherUsers() {
            //Arrange
            var accountsFromUser1 = Stream.of(accounts.get(0), accounts.get(1), accounts.get(2)).map(Account::getId).toList();

            //Act
            var result = accountRepository.findMyAccounts(user2.getId(), DEFAULT_PAGE);

            //Assert
            assertThat(result.getContent()).extracting(Account::getId)
                    .doesNotContainAnyElementsOf(accountsFromUser1);
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário ainda não tem contas")
        void shouldReturnEmptyPageWhenUserHasNoAccounts() {
            //Arrange
            var userWithoutAccounts = new User("Anna", "anna@email.com", "123456&");
            entityManager.persistAndFlush(userWithoutAccounts);
            var userId = userWithoutAccounts.getId();

            //Act
            var result = accountRepository.findMyAccounts(userId, DEFAULT_PAGE);

            //Assert
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Inclui contas inativas no conteúdo retornado")
        void shouldIncludeInactiveAccounts() {
            //Arrange
            var userId = user1.getId();
            var inactiveAccounts = List.of(accounts.get(0), accounts.get(1));
            inactiveAccounts.forEach(Account::deactivate);
            entityManager.flush();
            var inactiveIdsExpected = inactiveAccounts.stream().map(Account::getId).toList();

            //Act
            var result = accountRepository.findMyAccounts(userId, DEFAULT_PAGE);

            //Assert
            assertThat(result.getContent()).extracting(Account::getId).containsAll(inactiveIdsExpected);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("findMyAccount()")
    class FindMyAccount {
        @Test
        @DisplayName("Retorna conta especificada do usuário informado")
        void shouldReturnSpecificAccountFromUser() {
            //Arrange
            var expectedAccount = accounts.getFirst();
            var userId = user1.getId();

            //Act
            var result = accountRepository.findMyAccount(expectedAccount.getId(), userId);

            //Assert
            assertThat(result.isPresent()).isTrue();
            assertThat(result.get().getId()).isEqualTo(expectedAccount.getId());
            assertThat(result.get().getUser().getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Retorna vazio quando conta não existe")
        void shouldReturnEmptyWhenAccountDoesNotExist() {
            //Arrange
            var nonExistedAccountId = 9999L;

            //Act
            var result = accountRepository.findMyAccount(nonExistedAccountId, user1.getId());

            //Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Retorna vazio quando conta não pertence ao usuário")
        void shouldReturnEmptyWhenAccountDoesNotBelongsToUser() {
            //Arrange
            var accountFromUser2 = accounts.get(4).getId();

            //Act
            var result = accountRepository.findMyAccount(accountFromUser2, user1.getId());

            //Assert
            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Inclui conta inativa no retorno")
        void shouldIncludeInactiveAccount() {
            //Arrange
            var accountUser1 = accounts.getFirst();
            accountUser1.deactivate();
            entityManager.flush();

            //Act
            var result = accountRepository.findMyAccount(accountUser1.getId(), user1.getId());

            //Assert
            assertThat(result.isPresent()).isTrue();
            assertThat(result.get().getId()).isEqualTo(accountUser1.getId());
        }
    }

    @Nested
    @DisplayName("deleteMyAccountAndReturnCount()")
    class DeleteMyAccountAndReturnCount {
        @Test
        @DisplayName("Deleta conta do usuário e retorna linhas alteradas")
        void shouldDeleteUserAccountAndReturnRowsAffected() {
            //Arrange
            var accountId = accounts.getFirst().getId();
            var userId = user1.getId();

            //Act
            var result = accountRepository.deleteMyAccountAndReturnCount(accountId, userId);

            //Assert
            assertThat(result).isEqualTo(1);
            assertThat(accountRepository.findMyAccount(accountId, userId)).isEmpty();
        }

        @Test
        @DisplayName("Retorna 0 quando conta não existe")
        void shouldReturnZeroWhenAccountDoesNotExists() {
            //Arrange
            var nonExistedAccount = 9999L;

            //Act
            var result = accountRepository.deleteMyAccountAndReturnCount(nonExistedAccount, user1.getId());

            //Assert
            assertThat(result).isEqualTo(0);
            assertThat(accountRepository.findMyAccount(nonExistedAccount, user1.getId())).isEmpty();

        }

        @Test
        @DisplayName("Retorna 0 quando conta não pertence ao usuário")
        void shouldReturnZeroWhenAccountDoesNotBelongsToUser() {
            //Arrange
            var accountFromUser2 = accounts.get(4).getId();

            //Act
            var result = accountRepository.deleteMyAccountAndReturnCount(accountFromUser2, user1.getId());

            //Assert
            assertThat(result).isEqualTo(0);
            assertThat(accountRepository.findMyAccount(accountFromUser2, user2.getId())).isPresent();

        }
    }

}
