package com.gustavo.trackflowapp.modules.user;

import com.gustavo.trackflowapp.modules.user.dto.UserRegisterDTO;
import com.gustavo.trackflowapp.modules.user.dto.UserUpdateDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service")
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    UserService userService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User("Kenzo", "kenzo@email.com", "encodedPassword");
    }

    @Nested
    @DisplayName("registerUser()")
    class RegisterUser {
        @Test
        @DisplayName("Codifica a senha, salva o usuário e retorna os dados em um DTO")
        void shouldEncodePasswordSaveUserAndReturnData() {
            //Arrange
            var dto = new UserRegisterDTO("Kenzo", "kenzo@email.com", "Abc123!@");
            var captor = ArgumentCaptor.forClass(User.class);
            when(passwordEncoder.encode(dto.password())).thenReturn("encodedPassword");

            //Act
            var result = userService.registerUser(dto);

            //Assert
            verify(passwordEncoder).encode(dto.password());
            verify(userRepository).save(captor.capture());
            verifyNoMoreInteractions(userRepository);

            var capturedUser = captor.getValue();
            assertThat(capturedUser.getName()).isEqualTo(dto.name());
            assertThat(capturedUser.getEmail()).isEqualTo(dto.email());
            assertThat(capturedUser.getPassword()).isEqualTo("encodedPassword");

            assertThat(result.name()).isEqualTo(dto.name());
            assertThat(result.email()).isEqualTo(dto.email());
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {
        @Test
        @DisplayName("Exclui usuário do banco com sucesso")
        void shouldDeleteUser() {
            //Arrange
            var userId = 1L;
            when(userRepository.deleteByIdAndReturnCount(userId)).thenReturn(1);

            //Act & Assert
            assertThatNoException().isThrownBy(() -> userService.delete(userId));
            verify(userRepository).deleteByIdAndReturnCount(userId);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando usuário não encontrado")
        void shouldThrowWhenUserNotFound() {
            //Arrange
            var userId = 1L;
            when(userRepository.deleteByIdAndReturnCount(userId)).thenReturn(0);

            //Act & Assert
            assertThatThrownBy(() -> userService.delete(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User id not found for hard delete");
            verify(userRepository).deleteByIdAndReturnCount(userId);
            verifyNoMoreInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {
        @Test
        @DisplayName("Atualiza nome e senha quando ambos são informados")
        void shouldUpdateNameAndPasswordWhenBothAreProvided() {
            //Arrange
            var userId = 1L;
            var dto = new UserUpdateDTO("Nolan", "NewAbc123!@");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(dto.password())).thenReturn("newEncodedPassword");

            //Act
            var result = userService.update(dto, userId);

            //Assert
            verify(userRepository).findById(userId);
            verify(passwordEncoder).encode(dto.password());
            verifyNoMoreInteractions(userRepository);

            assertThat(user.getName()).isEqualTo(dto.name());
            assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
            assertThat(result.name()).isEqualTo(dto.name());
        }

        @Test
        @DisplayName("Não altera senha quando somente o nome é informado")
        void shouldOnlyUpdateNameWhenPasswordIsNull() {
            //Arrange
            var userId = 1L;
            var originalPassword = user.getPassword();
            var dto = new UserUpdateDTO("Nolan", null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            //Act
            var result = userService.update(dto, userId);

            //Assert
            verify(userRepository).findById(userId);
            verifyNoInteractions(passwordEncoder);
            verifyNoMoreInteractions(userRepository);

            assertThat(user.getName()).isEqualTo(dto.name());
            assertThat(user.getPassword()).isEqualTo(originalPassword);
            assertThat(result.name()).isEqualTo(dto.name());
        }

        @Test
        @DisplayName("Não altera nome quando somente a senha é informada")
        void shouldOnlyUpdatePasswordWhenNameIsNull() {
            //Arrange
            var userId = 1L;
            var originalName = user.getName();
            var dto = new UserUpdateDTO(null, "NewAbc123!@");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(dto.password())).thenReturn("newEncodedPassword");

            //Act
            var result = userService.update(dto, userId);

            //Assert
            assertThat(user.getName()).isEqualTo(originalName);
            assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
            assertThat(result.name()).isEqualTo(originalName);
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando usuário não encontrado")
        void shouldThrowWhenUserNotFound() {
            //Arrange
            var userId = 1L;
            var dto = new UserUpdateDTO("Nolan", null);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            //Act & Assert
            assertThatThrownBy(() -> userService.update(dto, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found for update");
            verify(userRepository).findById(userId);
            verifyNoMoreInteractions(userRepository);
            verifyNoInteractions(passwordEncoder);
        }
    }

    @Nested
    @DisplayName("getMyProfile()")
    class GetMyProfile {
        @Test
        @DisplayName("Retorna dados do usuário autenticado")
        void shouldReturnAuthenticatedUserData() {
            //Arrange
            var userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            //Act
            var result = userService.getMyProfile(userId);

            //Assert
            verify(userRepository).findById(userId);
            verifyNoMoreInteractions(userRepository);

            assertThat(result.name()).isEqualTo(user.getName());
            assertThat(result.email()).isEqualTo(user.getEmail());
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando usuário não encontrado")
        void shouldThrowWhenUserNotFound() {
            //Arrange
            var userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            //Act & Assert
            assertThatThrownBy(() -> userService.getMyProfile(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found for list");
            verify(userRepository).findById(userId);
            verifyNoMoreInteractions(userRepository);
        }
    }
}