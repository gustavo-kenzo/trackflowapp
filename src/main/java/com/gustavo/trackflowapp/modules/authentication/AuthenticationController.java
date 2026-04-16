package com.gustavo.trackflowapp.modules.authentication;

import com.gustavo.trackflowapp.infra.token.TokenService;
import com.gustavo.trackflowapp.modules.authentication.dto.LoginDTO;
import com.gustavo.trackflowapp.modules.authentication.dto.TokenDTO;
import com.gustavo.trackflowapp.modules.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @PostMapping
    public ResponseEntity login(@RequestBody LoginDTO loginDTO) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(loginDTO.email(), loginDTO.password());
        var authentication = authenticationManager.authenticate(authenticationToken);
        var token = tokenService.generateToken((User) authentication.getPrincipal());
        return ResponseEntity.ok(new TokenDTO(token));
    }


}
