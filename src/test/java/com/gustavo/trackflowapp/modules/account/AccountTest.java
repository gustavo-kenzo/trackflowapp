package com.gustavo.trackflowapp.modules.account;

import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Account")
class AccountTest {

    private Account account;
    private User user;

    @BeforeEach
    void setup() {
        user = new User("Kenzo", "kenzo@email.com", "123456");
        account = new Account(user, "Nubank", BigDecimal.valueOf(100.00));
    }

    @Nested
    @DisplayName("constructor()")
    class Constructor {
        @Test
        @DisplayName("Cria conta ativa com saldo informado pelo usuário")
        void shouldCreateActiveAccountWithBalanceSpecifiedByUser() {
            //Arrange
            var name = "Bradesco";
            var openingBalance = BigDecimal.valueOf(75);

            //Act
            var accountTest = new Account(user, name, openingBalance);

            //Assert
            assertThat(accountTest.getUser()).isEqualTo(user);
            assertThat(accountTest.getName()).isEqualTo(name);
            assertThat(accountTest.getOpeningBalance()).isEqualByComparingTo(openingBalance);
            assertThat(accountTest.getCurrentBalance()).isEqualByComparingTo(openingBalance);
            assertThat(accountTest.isActive()).isTrue();
        }

        @Test
        @DisplayName("Quando openingBalance é nulo define saldo como 0")
        void shouldDefineBalanceAsZeroWhenOpeningBalanceIsNull() {
            //Arrange
            var name = "Bradesco";

            //Act
            var accountTest = new Account(user, name, null);

            //Assert
            assertThat(accountTest.getOpeningBalance()).isEqualByComparingTo("0");
            assertThat(accountTest.getCurrentBalance()).isEqualByComparingTo("0");

        }
    }

    @Nested
    @DisplayName("credit()")
    class Credit {
        @Test
        @DisplayName("Adiciona valor ao saldo atual")
        void shouldAddAmountToCurrentBalance() {
            //Arrange
            var amount = BigDecimal.valueOf(4.55);

            //Act
            account.credit(amount);

            //Assert
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("104.55");
        }


        @ParameterizedTest
        @NullSource
        @ValueSource(doubles = {0.0, -18})
        @DisplayName("Lança BusinessRuleException se amount for nulo, negativo ou 0")
        void shouldThrowWhenAmountIsNullNegativeOrZero(Double value) {
            //Arrange
            var amount = value != null ? BigDecimal.valueOf(value) : null;

            //Act & Assert
            assertThatThrownBy(() -> account.credit(amount))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("amount must be positive");
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("100");
        }
    }

    @Nested
    @DisplayName("debit()")
    class Debit {
        @Test
        @DisplayName("Retirar valor do saldo atual")
        void shouldSubtractAmountFromCurrentBalance() {
            //Arrange
            var amount = BigDecimal.valueOf(4.55);

            //Act
            account.debit(amount);

            //Assert
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("95.45");
        }

        @Test
        @DisplayName("Permite saldo ficar negativo")
        void shouldAllowNegativaBalance() {
            //Arrange
            var amount = BigDecimal.valueOf(101.55);

            //Act
            account.debit(amount);

            //Assert
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("-1.55");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(doubles = {0, -15})
        @DisplayName("Lança BusinessRuleException se amount for nulo, negativo ou 0")
        void shouldThrowWhenAmountIsNullNegativeOrZero(Double value) {
            //Arrange
            var amount = value != null ? BigDecimal.valueOf(value) : null;

            //Act & Assert
            assertThatThrownBy(() -> account.debit(amount))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("amount must be positive");
            assertThat(account.getCurrentBalance()).isEqualByComparingTo("100");
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {
        @Test
        @DisplayName("Desativa a conta do usuário sem exceção")
        void shouldDeactivateUserAccount() {
            //Act & Assert
            assertThatCode(() -> account.deactivate())
                    .doesNotThrowAnyException();
            assertThat(account.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("activate()")
    class Activate {
        @Test
        @DisplayName("Ativa a conta do usuário sem exceção")
        void shouldActivateUserAccount() {
            //Arrange
            account.deactivate();

            //Act & Assert
            assertThatCode(() -> account.activate())
                    .doesNotThrowAnyException();
            assertThat(account.isActive()).isTrue();
        }
    }

}
