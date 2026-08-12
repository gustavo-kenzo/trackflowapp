package com.gustavo.trackflowapp.modules.category;

import com.gustavo.trackflowapp.infra.security.SecurityFilter;
import com.gustavo.trackflowapp.modules.category.dto.CategoryDataDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryRegisterDTO;
import com.gustavo.trackflowapp.modules.category.dto.CategoryUpdateDTO;
import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CategoryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
class CategoryControllerTest {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    private User authenticatedUser;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setup() {
        authenticatedUser = new User("Kenzo", "kenzo@email.com", "123456");
        ReflectionTestUtils.setField(authenticatedUser, "id", 1L);
        authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
    }

    @Nested
    @DisplayName("POST /categories")
    class CreateCategory {
        @Test
        @DisplayName("Cria categoria com sucesso e retorna dados")
        void shouldCreateCategoryAndReturnDataDto() throws Exception {
            //Arrange
            var registerDTO = new CategoryRegisterDTO("Alimentação");
            var expected = new CategoryDataDTO(1L, authenticatedUser.getName(), registerDTO.name(), true);
            when(categoryService.createCategory(registerDTO, authenticatedUser)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(post("/categories")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(registerDTO))
            );

            //Assert
            verify(categoryService).createCategory(registerDTO, authenticatedUser);
            response.andExpect(header().string("Location", endsWith("categories/1")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(expected.id()))
                    .andExpect(jsonPath("$.owner").value(expected.owner()))
                    .andExpect(jsonPath("$.name").value(expected.name()))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("Deve retornar 400 quando houver registro de campos inválidos")
        void shouldReturn400WhenThereIsInvalidFieldsRegister() throws Exception {
            //Arrange
            var json = """
                    {
                        "name":null
                    }
                    """;

            //Act
            var response = mockMvc.perform(post("/categories")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            );

            //Assert
            response.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.field").value("name"))
                    .andExpect(jsonPath("$.message").exists());
            verifyNoInteractions(categoryService);
        }
    }

    @Nested
    @DisplayName("GET /categories/active")
    class ListCategoriesActive {
        @Test
        @DisplayName("Lista categorias ativas do usuário informado")
        void shouldListUserActiveCategories() throws Exception {
            //Arrange
            var defaultPageable = PageRequest.of(0, 20);
            var categories = List.of(
                    new CategoryDataDTO(1L, "Kenzo", "Alimentação", true),
                    new CategoryDataDTO(2L, "Kenzo", "Transporte", true)
            );
            var expected = new PageImpl<>(categories, defaultPageable, categories.size());
            when(categoryService.listCategoriesActive(authenticatedUser.getId(), defaultPageable)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/categories/active")
                    .with(authentication(authentication)));

            //Assert
            var assertedCategory1 = categories.get(0);
            var assertedCategory2 = categories.get(1);
            verify(categoryService).listCategoriesActive(authenticatedUser.getId(), defaultPageable);
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(categories.size()))

                    .andExpect(jsonPath("$.content[0].id").value(assertedCategory1.id()))
                    .andExpect(jsonPath("$.content[0].owner").value(assertedCategory1.owner()))
                    .andExpect(jsonPath("$.content[0].name").value(assertedCategory1.name()))
                    .andExpect(jsonPath("$.content[0].active").value(assertedCategory1.active()))

                    .andExpect(jsonPath("$.content[1].id").value(assertedCategory2.id()))
                    .andExpect(jsonPath("$.content[1].owner").value(assertedCategory2.owner()))
                    .andExpect(jsonPath("$.content[1].name").value(assertedCategory2.name()))
                    .andExpect(jsonPath("$.content[1].active").value(assertedCategory2.active()));
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário não tem categorias ativas")
        void shouldReturnEmptyPageWhenUserDoesNotHaveActiveCategories() throws Exception {
            //Arrange
            var defaultPageable = PageRequest.of(0, 20);
            var expected = new PageImpl<CategoryDataDTO>(List.of(), defaultPageable, 0);
            when(categoryService.listCategoriesActive(authenticatedUser.getId(), defaultPageable)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/categories/active")
                    .with(authentication(authentication)));

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /categories/inactive")
    class ListCategoriesInactive {
        @Test
        @DisplayName("Lista categorias inativas do usuário informado")
        void shouldListUserInactiveCategories() throws Exception {
            //Arrange
            var defaultPageable = PageRequest.of(0, 20);
            var categories = List.of(
                    new CategoryDataDTO(3L, "Kenzo", "Lazer", false)
            );
            var expected = new PageImpl<>(categories, defaultPageable, categories.size());
            when(categoryService.listCategoriesInactive(authenticatedUser.getId(), defaultPageable)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/categories/inactive")
                    .with(authentication(authentication)));

            //Assert
            verify(categoryService).listCategoriesInactive(authenticatedUser.getId(), defaultPageable);
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].active").value(false));
        }

        @Test
        @DisplayName("Retorna page vazia quando usuário não tem categorias inativas")
        void shouldReturnEmptyPageWhenUserDoesNotHaveInactiveCategories() throws Exception {
            //Arrange
            var defaultPageable = PageRequest.of(0, 20);
            var expected = new PageImpl<CategoryDataDTO>(List.of(), defaultPageable, 0);
            when(categoryService.listCategoriesInactive(authenticatedUser.getId(), defaultPageable)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(get("/categories/inactive")
                    .with(authentication(authentication)));

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }
    }

    @Nested
    @DisplayName("PATCH /categories/{categoryId}")
    class UpdateCategory {
        @Test
        @DisplayName("Atualiza categoria do usuário informado")
        void shouldUpdateUserCategory() throws Exception {
            //Arrange
            var categoryId = 1L;
            var userId = authenticatedUser.getId();
            var dto = new CategoryUpdateDTO("Lazer", false);
            var expected = new CategoryDataDTO(categoryId, authenticatedUser.getName(), dto.name(), false);
            when(categoryService.updateCategory(dto, userId, categoryId)).thenReturn(expected);

            //Act
            var response = mockMvc.perform(patch("/categories/{categoryId}", categoryId)
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dto))
            );

            //Assert
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(dto.name()))
                    .andExpect(jsonPath("$.active").value(false));
            verify(categoryService).updateCategory(dto, userId, categoryId);
        }

        @Test
        @DisplayName("Retorna 404 quando categoria não encontrada para atualização")
        void shouldReturn404WhenCategoryNotFound() throws Exception {
            //Arrange
            var categoryId = 1L;
            var userId = authenticatedUser.getId();
            var dto = new CategoryUpdateDTO("Lazer", false);
            when(categoryService.updateCategory(dto, userId, categoryId))
                    .thenThrow(new ResourceNotFoundException("Category not found for this user"));

            //Act
            var response = mockMvc.perform(patch("/categories/{categoryId}", categoryId)
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
    @DisplayName("DELETE /categories/{categoryId}")
    class DeleteCategory {
        @Test
        @DisplayName("Deleta categoria do usuário informado")
        void shouldDeleteUserCategory() throws Exception {
            //Arrange
            var categoryId = 1L;
            doNothing().when(categoryService).hardDelete(categoryId, authenticatedUser);

            //Act
            var response = mockMvc.perform(delete("/categories/{categoryId}", categoryId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isNoContent());
            verify(categoryService).hardDelete(categoryId, authenticatedUser);
        }

        @Test
        @DisplayName("Retorna 404 quando categoria não encontrada para exclusão")
        void shouldReturn404WhenCategoryNotFound() throws Exception {
            //Arrange
            var categoryId = 1L;
            doThrow(new ResourceNotFoundException("Category not found for hard delete"))
                    .when(categoryService).hardDelete(categoryId, authenticatedUser);

            //Act
            var response = mockMvc.perform(delete("/categories/{categoryId}", categoryId)
                    .with(authentication(authentication))
                    .with(csrf())
            );

            //Assert
            response.andExpect(status().isNotFound());
        }
    }
}