package com.gustavo.trackflowapp.integration.fixtures;

import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.modules.user.UserRepository;
import com.gustavo.trackflowapp.modules.user.dto.UserDataDTO;
import com.gustavo.trackflowapp.modules.user.dto.UserRegisterDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class UserTestFixture {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private UserRepository userRepository;

    public User register(String name, String email, String password) {
        var registerDTO = new UserRegisterDTO(name, email, password);

        restTestClient.post().uri("/users")
                .body(registerDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserDataDTO.class);

        var user = userRepository.findByEmail(email);
        assertThat(user).isPresent();

        return user.get();
    }


}
