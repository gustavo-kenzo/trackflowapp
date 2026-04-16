package com.gustavo.trackflowapp;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TrackflowappApplication {

    public static void main(String[] args) {
        var dotenv = Dotenv.configure().load();
        System.setProperty("DB_HOST",dotenv.get("DB_HOST"));
        System.setProperty("DB_PORT",dotenv.get("DB_PORT"));
        System.setProperty("DB_NAME",dotenv.get("DB_NAME"));
        System.setProperty("DB_USER",dotenv.get("DB_USER"));
        System.setProperty("DB_PASSWORD",dotenv.get("DB_PASSWORD"));

        SpringApplication.run(TrackflowappApplication.class, args);
    }

}
