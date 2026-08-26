package com.gustavo.trackflowapp.modules.user;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    private static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 10);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;

    @BeforeEach
    void setup() {
        user1 = new User("Mark", "mark@email.com", "123456&");
        user2 = new User("Nolan", "nolan@email.com", "123456&");
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {
        @Test
        @DisplayName("Retorna todos os usuários paginados")
        void shouldReturnAllUsersPaginated() {
            //Act
            var result = userRepository.findAll(DEFAULT_PAGE);

            //Assert
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(User::getId)
                    .containsExactlyInAnyOrder(user1.getId(), user2.getId());
        }

        @Test
        @DisplayName("Retorna page vazia quando não há usuários cadastrados")
        void shouldReturnEmptyPageWhenThereAreNoUsers() {
            //Arrange
            userRepository.deleteAll();
            entityManager.flush();

            //Act
            var result = userRepository.findAll(DEFAULT_PAGE);

            //Assert
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("deleteByIdAndReturnCount()")
    class DeleteByIdAndReturnCount {
        @Test
        @DisplayName("Deleta usuário existente e retorna 1 linha alterada")
        void shouldDeleteUserAndReturnOne() {
            //Arrange
            var userId = user1.getId();

            //Act
            var result = userRepository.deleteByIdAndReturnCount(userId);

            //Assert
            assertThat(result).isEqualTo(1);
            assertThat(userRepository.findById(userId)).isEqualTo(Optional.of(user1));
        }

        @Test
        @DisplayName("Retorna 0 quando usuário não existe")
        void shouldReturnZeroWhenUserDoesNotExist() {
            //Arrange
            var nonExistedUserId = 9999L;

            //Act
            var result = userRepository.deleteByIdAndReturnCount(nonExistedUserId);

            //Assert
            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {
        @Test
        @DisplayName("Retorna usuário quando email existe")
        void shouldReturnUserWhenEmailExists() {
            //Act
            var result = userRepository.findByEmail(user1.getEmail());

            //Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(user1.getId());
        }

        @Test
        @DisplayName("Retorna vazio quando email não existe")
        void shouldReturnEmptyWhenEmailDoesNotExist() {
            //Act
            var result = userRepository.findByEmail("naoexiste@email.com");

            //Assert
            assertThat(result).isEmpty();
        }
    }
}