package com.gustavo.trackflowapp.integration.fixtures;

import com.gustavo.trackflowapp.modules.authentication.dto.LoginDTO;
import com.gustavo.trackflowapp.modules.authentication.dto.TokenDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Component
public class LoginTestFixture {

    @Autowired
    private RestTestClient restTestClient;

    public String login(String email, String password) {

        var loginDTO = new LoginDTO(email, password);
        var responseLogin = restTestClient.post().uri("/login")
                .body(loginDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenDTO.class)
                .returnResult()
                .getResponseBody();

        assertThat(responseLogin).isNotNull();
        return responseLogin.accessToken();
    }
}
