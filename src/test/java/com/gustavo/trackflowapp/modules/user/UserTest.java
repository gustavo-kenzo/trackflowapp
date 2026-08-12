package com.gustavo.trackflowapp.modules.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User")
class UserTest {

    private User user;

    @BeforeEach
    void setup() {
        user = new User("Kenzo", "kenzo@email.com", "123456");
    }

    @Nested
    @DisplayName("constructor()")
    class Constructor {
        @Test
        @DisplayName("Cria usuário com nome, email e senha informados")
        void shouldCreateUserWithProvidedNameEmailAndPassword() {
            //Arrange
            var name = "Mark";
            var email = "mark@email.com";
            var password = "abc123";

            //Act
            var userTest = new User(name, email, password);

            //Assert
            assertThat(userTest.getName()).isEqualTo(name);
            assertThat(userTest.getEmail()).isEqualTo(email);
            assertThat(userTest.getPassword()).isEqualTo(password);
        }
    }

    @Nested
    @DisplayName("getAuthorities()")
    class GetAuthorities {
        @Test
        @DisplayName("Retorna somente a authority ROLE_USER")
        void shouldReturnOnlyRoleUserAuthority() {
            //Act
            var authorities = user.getAuthorities();

            //Assert
            assertThat(authorities).hasSize(1);
            assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("getUsername()")
    class GetUsername {
        @Test
        @DisplayName("Retorna o email do usuário como username")
        void shouldReturnEmailAsUsername() {
            //Act & Assert
            assertThat(user.getUsername()).isEqualTo(user.getEmail());
        }
    }

    @Nested
    @DisplayName("updateName()")
    class UpdateName {
        @Test
        @DisplayName("Atualiza o nome quando valor informado é não nulo")
        void shouldUpdateNameWhenValueIsNotNull() {
            //Arrange
            var newName = "Nolan";

            //Act
            user.updateName(newName);

            //Assert
            assertThat(user.getName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("Não altera o nome quando valor informado é nulo")
        void shouldNotUpdateNameWhenValueIsNull() {
            //Arrange
            var originalName = user.getName();

            //Act
            user.updateName(null);

            //Assert
            assertThat(user.getName()).isEqualTo(originalName);
        }
    }

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePassword {
        @Test
        @DisplayName("Atualiza a senha quando valor informado é não nulo")
        void shouldUpdatePasswordWhenValueIsNotNull() {
            //Arrange
            var newPassword = "newEncodedPassword";

            //Act
            user.updatePassword(newPassword);

            //Assert
            assertThat(user.getPassword()).isEqualTo(newPassword);
        }

        @Test
        @DisplayName("Não altera a senha quando valor informado é nulo")
        void shouldNotUpdatePasswordWhenValueIsNull() {
            //Arrange
            var originalPassword = user.getPassword();

            //Act
            user.updatePassword(null);

            //Assert
            assertThat(user.getPassword()).isEqualTo(originalPassword);
        }
    }
}