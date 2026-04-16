package com.gustavo.trackflowapp.infra.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.gustavo.trackflowapp.modules.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    @Value("${jwt.secret}")
    private String secret;

    public String generateToken(User user) {
        try {
            var algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("auth0")
                    .withSubject(user.getEmail())
                    .withClaim("id",user.getId())
                    .withExpiresAt(expiresAt())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error generating JWT token");
        }
    }

    private Instant expiresAt() {
        return Instant.now().plus(2, ChronoUnit.HOURS);
    }

}
