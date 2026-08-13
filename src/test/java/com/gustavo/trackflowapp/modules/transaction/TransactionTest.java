package com.gustavo.trackflowapp.modules.transaction;

import com.gustavo.trackflowapp.modules.account.Account;
import com.gustavo.trackflowapp.modules.category.Category;
import com.gustavo.trackflowapp.modules.recurrence.Recurrence;
import com.gustavo.trackflowapp.modules.recurrence.RecurrenceFrequency;
import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Transaction")
class TransactionTest {

    private User user;
    private User anotherUser;
    private Account account;
    private Category category;
    private Recurrence recurrence;
    private Transaction transaction;

    @BeforeEach
    void setup() {
        user = new User("Kenzo", "kenzo@email.com", "123456");
        anotherUser = new User("Mark", "mark@email.com", "123456&");
        // Entidades não persistidas têm id nulo por padrão; setamos manualmente pois
        // validateOwnership() compara os ids dos usuários (owner.getId().equals(user.getId())).
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(anotherUser, "id", 2L);
        account = new Account(user, "Nubank", BigDecimal.valueOf(100));
        category = new Category(user, "Mercado");
        // Recurrence ainda está em desenvolvimento; instância usada apenas como preenchimento
        // para satisfazer o construtor de Transaction, sem relevância para as regras testadas aqui.
        recurrence = new Recurrence(user, RecurrenceFrequency.WEEKLY,LocalDate.now(),LocalDate.now().plusWeeks(1),3000);

        transaction = new Transaction(
                user,
                account,
                category,
                recurrence,
                BigDecimal.valueOf(50),
                TransactionType.EXPENSE,
                LocalDate.now(),
                null,
                "Compras do mês"
        );
    }

    @Nested
    @DisplayName("constructor()")
    class Constructor {
        @Test
        @DisplayName("Cria transação pendente quando settlementDate não informada")
        void shouldCreatePendingTransactionWhenSettlementDateIsNull() {
            //Arrange
            var competenceDate = LocalDate.now();

            //Act
            var result = new Transaction(user, account, category, recurrence, BigDecimal.valueOf(50), TransactionType.EXPENSE, competenceDate, null, "descrição");

            //Assert
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getAccount()).isEqualTo(account);
            assertThat(result.getCategory()).isEqualTo(category);
            assertThat(result.getRecurrence()).isEqualTo(recurrence);
            assertThat(result.getAmount()).isEqualByComparingTo("50");
            assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.getCompetenceDate()).isEqualTo(competenceDate);
            assertThat(result.getSettlementDate()).isNull();
            assertThat(result.getDescription()).isEqualTo("descrição");
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
        }

        @Test
        @DisplayName("Cria transação com status SETTLED quando settlementDate informada")
        void shouldCreateSettledTransactionWhenSettlementDateIsInformed() {
            //Arrange
            var competenceDate = LocalDate.now().minusDays(2);
            var settlementDate = LocalDate.now();

            //Act
            var result = new Transaction(user, account, category, recurrence, BigDecimal.valueOf(50), TransactionType.EXPENSE, competenceDate, settlementDate, "descrição");

            //Assert
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.SETTLED);
            assertThat(result.getSettlementDate()).isEqualTo(settlementDate);
        }

        @Test
        @DisplayName("Permite category, recurrence e settlementDate nulos")
        void shouldAllowOptionalFieldsNull() {
            //Act & Assert
            assertThatCode(() -> new Transaction(user, account, null, null, BigDecimal.valueOf(10), TransactionType.INCOME, LocalDate.now(), null, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lança NullPointerException quando user é nulo")
        void shouldThrowWhenUserIsNull() {
            //Act & Assert
            assertThatThrownBy(() -> new Transaction(null, account, category, recurrence, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), null, "desc"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Lança NullPointerException quando account é nula")
        void shouldThrowWhenAccountIsNull() {
            //Act & Assert
            assertThatThrownBy(() -> new Transaction(user, null, category, recurrence, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), null, "desc"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Lança NullPointerException quando type é nulo")
        void shouldThrowWhenTypeIsNull() {
            //Act & Assert
            assertThatThrownBy(() -> new Transaction(user, account, category, recurrence, BigDecimal.valueOf(50), null, LocalDate.now(), null, "desc"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Lança NullPointerException quando competenceDate é nula")
        void shouldThrowWhenCompetenceDateIsNull() {
            //Act & Assert
            assertThatThrownBy(() -> new Transaction(user, account, category, recurrence, BigDecimal.valueOf(50), TransactionType.EXPENSE, null, null, "desc"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando category não pertence ao usuário")
        void shouldThrowWhenCategoryDoesNotBelongToUser() {
            //Arrange
            var categoryFromAnotherUser = new Category(anotherUser, "Lazer");

            //Act & Assert
            assertThatThrownBy(() -> new Transaction(user, account, categoryFromAnotherUser, recurrence, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), null, "desc"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("category does not belong to this user");
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando account não pertence ao usuário")
        void shouldThrowWhenAccountDoesNotBelongToUser() {
            //Arrange
            var accountFromAnotherUser = new Account(anotherUser, "Bradesco", BigDecimal.valueOf(10));

            //Act & Assert
            assertThatThrownBy(() -> new Transaction(user, accountFromAnotherUser, category, recurrence, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), null, "desc"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("this account does not belong to this user");
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando recurrence não pertence ao usuário")
        void shouldThrowWhenRecurrenceDoesNotBelongToUser() {
            //Arrange
            // Recurrence ainda está em desenvolvimento; usada aqui só como dado de preenchimento
            // para acionar a validação de ownership, sem relevância funcional própria.
            var recurrenceFromAnotherUser = new Recurrence(anotherUser, RecurrenceFrequency.WEEKLY,LocalDate.now(),LocalDate.now().plusWeeks(1),3000);

            //Act & Assert
            assertThatThrownBy(() -> new Transaction(user, account, category, recurrenceFromAnotherUser, BigDecimal.valueOf(50), TransactionType.EXPENSE, LocalDate.now(), null, "desc"))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando amount é nulo, negativo ou zero")
        void shouldThrowWhenAmountIsNullNegativeOrZero() {
            //Act & Assert
            assertThatThrownBy(() -> new Transaction(user, account, category, recurrence, null, TransactionType.EXPENSE, LocalDate.now(), null, "desc"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("amount must be positive");

            assertThatThrownBy(() -> new Transaction(user, account, category, recurrence, BigDecimal.ZERO, TransactionType.EXPENSE, LocalDate.now(), null, "desc"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("amount must be positive");

            assertThatThrownBy(() -> new Transaction(user, account, category, recurrence, BigDecimal.valueOf(-10), TransactionType.EXPENSE, LocalDate.now(), null, "desc"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("amount must be positive");
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando settlementDate é anterior à competenceDate")
        void shouldThrowWhenSettlementDateIsBeforeCompetenceDate() {
            //Arrange
            var competenceDate = LocalDate.now();
            var settlementDate = competenceDate.minusDays(1);

            //Act & Assert
            assertThatThrownBy(() -> new Transaction(user, account, category, recurrence, BigDecimal.valueOf(50), TransactionType.EXPENSE, competenceDate, settlementDate, "desc"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("settlementDate cannot be before competenceDate");
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando settlementDate é futura")
        void shouldThrowWhenSettlementDateIsFuture() {
            //Arrange
            var competenceDate = LocalDate.now();
            var settlementDate = LocalDate.now().plusDays(1);

            //Act & Assert
            assertThatThrownBy(() -> new Transaction(user, account, category, recurrence, BigDecimal.valueOf(50), TransactionType.EXPENSE, competenceDate, settlementDate, "desc"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("settlementDate shouldn't be future");
        }
    }

    @Nested
    @DisplayName("settle()")
    class Settle {
        @Test
        @DisplayName("Marca transação como liquidada com a data informada")
        void shouldSettleTransactionWithInformedDate() {
            //Arrange
            var settlementDate = LocalDate.now();

            //Act
            transaction.settle(settlementDate);

            //Assert
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SETTLED);
            assertThat(transaction.getSettlementDate()).isEqualTo(settlementDate);
        }

        @Test
        @DisplayName("Usa data atual quando settlementDate não é informada")
        void shouldUseCurrentDateWhenSettlementDateIsNull() {
            //Act
            transaction.settle(null);

            //Assert
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SETTLED);
            assertThat(transaction.getSettlementDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando já está liquidada")
        void shouldThrowWhenAlreadySettled() {
            //Arrange
            transaction.settle(LocalDate.now());

            //Act & Assert
            assertThatThrownBy(() -> transaction.settle(LocalDate.now()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("transaction already settled");
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando data é anterior à competenceDate")
        void shouldThrowWhenDateIsBeforeCompetenceDate() {
            //Act & Assert
            assertThatThrownBy(() -> transaction.settle(transaction.getCompetenceDate().minusDays(1)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("settlementDate cannot be before competenceDate");
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando data é futura")
        void shouldThrowWhenDateIsFuture() {
            //Act & Assert
            assertThatThrownBy(() -> transaction.settle(LocalDate.now().plusDays(1)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("settlementDate shouldn't be future");
        }
    }

    @Nested
    @DisplayName("reopen()")
    class Reopen {
        @Test
        @DisplayName("Reabre transação liquidada, limpando settlementDate")
        void shouldReopenSettledTransaction() {
            //Arrange
            transaction.settle(LocalDate.now());

            //Act
            transaction.reopen();

            //Assert
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(transaction.getSettlementDate()).isNull();
        }

        @Test
        @DisplayName("Lança BusinessRuleException quando já está pendente")
        void shouldThrowWhenAlreadyPending() {
            //Act & Assert
            assertThatThrownBy(() -> transaction.reopen())
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("transaction already pending");
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {
        @Test
        @DisplayName("Atualiza apenas os campos informados (não nulos)")
        void shouldUpdateOnlyInformedFields() {
            //Arrange
            var newAccount = new Account(user, "PagBank", BigDecimal.valueOf(20));
            var newCategory = new Category(user, "Lazer");
            var newAmount = BigDecimal.valueOf(99);
            var newCompetenceDate = LocalDate.now().minusDays(1);

            //Act
            transaction.update(newAccount, newCategory, newAmount, TransactionType.INCOME, newCompetenceDate, "nova descrição");

            //Assert
            assertThat(transaction.getAccount()).isEqualTo(newAccount);
            assertThat(transaction.getCategory()).isEqualTo(newCategory);
            assertThat(transaction.getAmount()).isEqualByComparingTo(newAmount);
            assertThat(transaction.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(transaction.getCompetenceDate()).isEqualTo(newCompetenceDate);
            assertThat(transaction.getDescription()).isEqualTo("nova descrição");
        }

        @Test
        @DisplayName("Mantém valores atuais quando parâmetros são nulos")
        void shouldKeepCurrentValuesWhenParametersAreNull() {
            //Arrange
            var originalAccount = transaction.getAccount();
            var originalCategory = transaction.getCategory();
            var originalAmount = transaction.getAmount();
            var originalType = transaction.getType();
            var originalCompetenceDate = transaction.getCompetenceDate();
            var originalDescription = transaction.getDescription();

            //Act
            transaction.update(null, null, null, null, null, null);

            //Assert
            assertThat(transaction.getAccount()).isEqualTo(originalAccount);
            assertThat(transaction.getCategory()).isEqualTo(originalCategory);
            assertThat(transaction.getAmount()).isEqualByComparingTo(originalAmount);
            assertThat(transaction.getType()).isEqualTo(originalType);
            assertThat(transaction.getCompetenceDate()).isEqualTo(originalCompetenceDate);
            assertThat(transaction.getDescription()).isEqualTo(originalDescription);
        }
    }
}