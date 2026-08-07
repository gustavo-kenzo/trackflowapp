package com.gustavo.trackflowapp.modules.account;

import com.gustavo.trackflowapp.modules.account.dto.AccountDataDTO;
import com.gustavo.trackflowapp.modules.account.dto.AccountRegisterDTO;
import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.exception.BusinessRuleException;
import com.gustavo.trackflowapp.shared.exception.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Service")
class AccountServiceTest {

    private static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 10);

    @Mock
    AccountRepository accountRepository;
    @InjectMocks
    AccountService accountService;

    private User user;
    private Account account1;
    private Account account2;

    @BeforeEach
    void setup() {
        user = new User("Kenzo", "kenzo@email.com", "123456");
        account1 = new Account(user, "Nubank", BigDecimal.valueOf(156.77));
        account2 = new Account(user, "Bradesco", BigDecimal.valueOf(5.77));
    }

    @Nested
    @DisplayName("create()")
    class Create {
        @Test
        @DisplayName("Salva conta corretamente no banco e retorna esses dados em um DTO")
        void shouldSaveAccountAndReturnDataWhenCreateAccount() {
            //Arrange
            var dto = new AccountRegisterDTO("Nubank", BigDecimal.valueOf(156.77));
            var captor = ArgumentCaptor.forClass(Account.class);

            //Act
            var result = accountService.create(dto, user);

            //Assert
            verify(accountRepository).save(captor.capture());
            verifyNoMoreInteractions(accountRepository);

            assertThat(result.owner()).isEqualTo(user.getName());
            assertThat(result.accountName()).isEqualTo(dto.name());
            assertThat(result.currentBalance()).isEqualByComparingTo(dto.openingBalance());
            assertThat(result.active()).isTrue();
        }

    }

    @Nested
    @DisplayName("findMyAccounts()")
    class FindMyAccounts {
        @Test
        @DisplayName("Retorna contas paginadas quando usuário tem contas")
        void shouldReturnPaginatedUserAccounts() {
            //Arrange
            var id = 1L;
            var accountPages = new PageImpl<>(List.of(account1, account2), DEFAULT_PAGE, 2);
            when(accountRepository.findMyAccounts(id, DEFAULT_PAGE)).thenReturn(accountPages);
            var expectedAccounts = accountPages.get().map(Account::getName).toList();

            //Act
            var result = accountService.findMyAccounts(id, DEFAULT_PAGE);

            //Assert
            verify(accountRepository).findMyAccounts(id, DEFAULT_PAGE);
            verifyNoMoreInteractions(accountRepository);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getContent()).extracting(AccountDataDTO::accountName)
                    .containsExactlyElementsOf(expectedAccounts);
        }

        @Test
        @DisplayName("Retorna pagina vazia quando usuário ainda não tem contas")
        void shouldReturnEmptyPagesWhenUserDoesNotHaveAccounts() {
            //Arrange
            var id = 1L;
            when(accountRepository.findMyAccounts(id, DEFAULT_PAGE)).thenReturn(Page.empty(DEFAULT_PAGE));

            //Act
            var result = accountService.findMyAccounts(id, DEFAULT_PAGE);

            //Assert
            verify(accountRepository).findMyAccounts(id, DEFAULT_PAGE);
            verifyNoMoreInteractions(accountRepository);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getTotalPages()).isZero();
        }
    }

    @Nested
    @DisplayName("hardDelete()")
    class HardDelete {
        @Test
        @DisplayName("Exclui registro de conta do usuário do banco com sucesso")
        void shouldDeleteUserAccount() {
            //Arrange
            var accountId = 1L;
            var userId = 1L;
            when(accountRepository.deleteMyAccountAndReturnCount(accountId, userId))
                    .thenReturn(1);

            //Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> accountService.hardDelete(accountId, userId));
            verify(accountRepository).deleteMyAccountAndReturnCount(accountId, userId);
            verifyNoMoreInteractions(accountRepository);

        }


        @Test
        @DisplayName("Lança ResourceNotFoundException quando conta não encontrada")
        void shouldThrowWhenUserAccountNotFound() {
            //Arrange
            var accountId = 1L;
            var userId = 1L;
            when(accountRepository.deleteMyAccountAndReturnCount(accountId, userId)).thenReturn(0);

            //Act & Assert
            assertThatThrownBy(() -> accountService.hardDelete(accountId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account id not found for hard delete");
            verify(accountRepository).deleteMyAccountAndReturnCount(accountId, userId);
            verifyNoMoreInteractions(accountRepository);
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {
        @Test
        @DisplayName("Desativa conta do usuário com sucesso")
        void shouldDeactivateUserAccount() {
            //Arrange
            var accountId = 1L;
            var userId = 1L;
            when(accountRepository.findMyAccount(accountId, userId))
                    .thenReturn(Optional.of(account1));

            //Act
            var result = accountService.deactivate(accountId, userId);

            //Assert
            verify(accountRepository).findMyAccount(accountId, userId);
            verifyNoMoreInteractions(accountRepository);
            /*Não chame save() explicitamente - depende do dirty checking do Transactional*/
            verify(accountRepository, never()).save(any());
            assertThat(result.active()).isFalse();
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando conta não encontrada")
        void shouldThrowWhenUserAccountNotFoundToDeactivate() {
            //Arrange
            var accountId = 1L;
            var userId = 1L;
            when(accountRepository.findMyAccount(accountId, userId))
                    .thenReturn(Optional.empty());

            //Act & Assert
            assertThatThrownBy(() -> accountService.deactivate(accountId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account id not found for deactivate");
            verify(accountRepository).findMyAccount(accountId, userId);
            verifyNoMoreInteractions(accountRepository);
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando conta já está inativa")
        void shouldThrowWhenAccountAlreadyInactive() {
            //Arrange
            var accountId = 1L;
            var userId = 1L;
            account1.deactivate();
            when(accountRepository.findMyAccount(accountId, userId)).thenReturn(Optional.of(account1));

            //Act & Assert
            assertThatThrownBy(() -> accountService.deactivate(accountId, userId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Account is already inactive");
            verify(accountRepository).findMyAccount(accountId, userId);
            verifyNoMoreInteractions(accountRepository);
        }
    }

    @Nested
    @DisplayName("activate()")
    class Activate {
        @Test
        @DisplayName("Ativa conta do usuário")
        void shouldActivateUserAccount() {
            //Arrange
            var accountId = 1L;
            var userId = 1L;
            account1.deactivate();
            when(accountRepository.findMyAccount(accountId, userId))
                    .thenReturn(Optional.of(account1));

            //Act
            var result = accountService.activate(accountId, userId);

            //Assert
            verify(accountRepository).findMyAccount(accountId, userId);
            verifyNoMoreInteractions(accountRepository);
            /*Não chame save() explicitamente - depende do dirty checking do Transactional*/
            verify(accountRepository, never()).save(any());
            assertThat(result.active()).isTrue();
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando conta não encontrado")
        void shouldThrowWhenUserAccountNotFoundToActivate() {
            //Arrange
            var accountId = 1L;
            var userId = 1L;
            when(accountRepository.findMyAccount(accountId, userId))
                    .thenReturn(Optional.empty());

            //Act & Assert
            assertThatThrownBy(() -> accountService.activate(accountId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account id not found for activate");
            verify(accountRepository).findMyAccount(accountId, userId);
            verifyNoMoreInteractions(accountRepository);
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando conta já está ativa")
        void shouldThrowWhenAccountAlreadyActive() {
            //Arrange
            var accountId = 1L;
            var userId = 1L;
            when(accountRepository.findMyAccount(accountId, userId)).thenReturn(Optional.of(account1));

            //Act & Assert
            assertThatThrownBy(() -> accountService.activate(accountId, userId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Account is already active");
            verify(accountRepository).findMyAccount(accountId, userId);
            verifyNoMoreInteractions(accountRepository);
        }
    }

    @Nested
    @DisplayName("getAccount()")
    class GetAccount {
        @Test
        @DisplayName("Retorna entidade completa da conta buscada pelo usuário")
        void shouldReturnEntityAccount() {
            //Arrange
            var accountId = 1L;
            var userId = 1L;
            when(accountRepository.findMyAccount(accountId, userId)).thenReturn(Optional.of(account1));

            //Act
            var result = accountService.getAccount(accountId, userId);

            //Assert
            verify(accountRepository).findMyAccount(accountId, userId);
            verifyNoMoreInteractions(accountRepository);
            assertThat(result).isSameAs(account1);
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando conta não encontrada")
        void shouldThrowWhenUserAccountNotFound() {
            //Arrange
            var accountId = 1L;
            var userId = 1L;
            when(accountRepository.findMyAccount(accountId, userId)).thenReturn(Optional.empty());

            //Act & Assert
            assertThatThrownBy(() -> accountService.getAccount(accountId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account id not found");
            verify(accountRepository).findMyAccount(accountId, userId);
            verifyNoMoreInteractions(accountRepository);
        }

    }

}
