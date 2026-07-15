package com.wellsfargo.bankingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Multi-Tier Banking System.
 *
 * Layers:
 *  - Presentation:  frontend/ (static HTML/CSS/JS) talking to REST endpoints below
 *  - Business:      service/ package - transaction rules, fraud checks, validation
 *  - Data:          repository/ + model/ package - JPA entities and Spring Data repos
 */
@SpringBootApplication
public class BankingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingSystemApplication.class, args);
    }
}
