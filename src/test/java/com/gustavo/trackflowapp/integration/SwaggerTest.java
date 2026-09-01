package com.gustavo.trackflowapp.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SwaggerTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Inicializa Swagger UI")
    void shouldInitializeSwaggerUI() throws Exception {
        //Act & Assert
        var response = restTestClient.get().uri("/swagger-ui/index.html")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.contains("Swagger UI"));
    }
}
