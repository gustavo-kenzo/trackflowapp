package com.gustavo.trackflowapp.modules.user;

import com.gustavo.trackflowapp.infra.security.SecurityFilter;
import com.gustavo.trackflowapp.modules.user.dto.UserDataDTO;
import com.gustavo.trackflowapp.modules.user.dto.UserRegisterDTO;
import com.gustavo.trackflowapp.modules.user.dto.UserUpdateDTO;
import com.gustavo.trackflowapp.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
public class UserControllerTest {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private User authenticatedUser;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setup() {
        authenticatedUser = new User("Kenzo", "kenzo@email.com", "123456S&");
        ReflectionTestUtils.setField(authenticatedUser, "id", 1L);
        authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
    }

    @Nested
    @DisplayName("POST /users")
    class RegisterUser {
        @Test
        @DisplayName("Cria usuário com sucesso e retorna dados")
        void shouldCreateUserAndReturnDataDto() throws Exception {
            //Arrange
            var dto = new UserRegisterDTO("Kenzo", "kenzo@email.com", "123456S&");
            var expect = new UserDataDTO(1L, "Kenzo", "kenzo@email.com");
            when(userService.registerUser(dto)).thenReturn(expect);

            //Act
            var response = mockMvc.perform(post("/users")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            verify(userService).registerUser(dto);
            response.andExpect(header().string("Location", endsWith("/users/1")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(authenticatedUser.getId()))
                    .andExpect(jsonPath("$.name").value(dto.name()))
                    .andExpect(jsonPath("$.email").value(dto.email()));
        }

        @Test
        @DisplayName("Deve retornar 400 quando houver registro de campos inválidos")
        void shouldReturn400WhenThereIsInvalidFieldsRegister() throws Exception {
            //Arrange
            var json = """
                    {
                        "name":"Maria",
                        "email":null,
                        "password":"valid_password_123456S&"
                    }
                    """;

            //Act
            var response = mockMvc.perform(post("/users")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            );

            //Assert
            response.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.field").value("email"))
                    .andExpect(jsonPath("$.message").exists());
            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("DELETE /users")
    class DeleteMyProfile {
        @Test
        @DisplayName("Deleta usuário")
        void shouldDeleteUser() throws Exception {
            //Arrange
            var userId = authenticatedUser.getId();
            doNothing().when(userService).delete(userId);

            //Act
            var response = mockMvc.perform(delete("/users")
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isNoContent());
            verify(userService).delete(userId);
        }

        @Test
        @DisplayName("Retorna 404 quando usuário não encontrada para exclusão")
        void shouldReturn404WhenUserNotFound() throws Exception {
            //Arrange
            var userId = authenticatedUser.getId();
            doThrow(new ResourceNotFoundException("User id not found for hard delete"))
                    .when(userService).delete(userId);
            //Act
            var response = mockMvc.perform(delete("/users")
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /users")
    class UpdateMyProfile {
        @Test
        @DisplayName("Atualiza dados do usuário com sucesso")
        void shouldUpdateUserSuccessfully() throws Exception {
            //Arrange
            var dto = new UserUpdateDTO("Luk", "new_valid_password_123456S&");
            var expected = new UserDataDTO(1L, "Luk", "kenzo@email.com");
            when(userService.update(dto, authenticatedUser.getId())).thenReturn(expected);

            //Act
            var response = mockMvc.perform(put("/users")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(dto.name()))
                    .andExpect(jsonPath("$.email").value("kenzo@email.com"));
            verify(userService).update(dto, authenticatedUser.getId());
        }

        @Test
        @DisplayName("Deve retornar 400 quando senha é inválido")
        void shouldReturn400WhenIdIsNull() throws Exception {
            //Arrange
            var json = """
                    {
                        "name":"nova nome",
                        "password":"invalid_password"
                    }
                    """;

            //Act
            var response = mockMvc.perform(put("/users")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            );

            //Assert
            response.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.field").value("password"))
                    .andExpect(jsonPath("$.message").exists());
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Retorna 404 quando usuário não encontrada para atualização")
        void shouldReturn404WhenUserNotFound() throws Exception {
            //Arrange
            var dto = new UserUpdateDTO("Luk", "new_valid_password_123456S&");
            var userId = authenticatedUser.getId();
            doThrow(new ResourceNotFoundException("User id not found for update"))
                    .when(userService).update(dto, userId);
            //Act
            var response = mockMvc.perform(put("/users")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /users")
    class GetMyProfile{
        @Test
        @DisplayName("Retorna informações do usuário com sucesso")
        void shouldReturnUserDataSuccessfully() throws Exception {
            //Arrange

            var expected = new UserDataDTO(1L,"Kenzo","kenzo@email.com");
            Long userId = authenticatedUser.getId();
            when(userService.getMyProfile(userId)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/users")
                    .with(authentication(authentication)));

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.name").value(authenticatedUser.getName()))
                    .andExpect(jsonPath("$.email").value(authenticatedUser.getEmail()));
            verify(userService).getMyProfile(userId);
        }
        @Test
        @DisplayName("Retorna 404 quando usuário não")
        void shouldReturn404WhenUserNotFound() throws Exception {
            //Arrange
            var userId = authenticatedUser.getId();
            doThrow(new ResourceNotFoundException("User id not found for list"))
                    .when(userService).getMyProfile(userId);
            //Act
            var response = mockMvc.perform(get("/users")
                    .with(authentication(authentication))
            );

            //Assert
            response.andExpect(status().isNotFound());
        }
    }

}
