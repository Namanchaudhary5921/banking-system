package com.wellsfargo.bankingsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Simple role-based auth for demo purposes: ADMIN (full access),
 * TELLER (can operate accounts/transactions), CUSTOMER (read-only on
 * their own data in a fuller implementation). Credentials below are for
 * local demo/testing only - never do this in a real deployment.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN", "TELLER").build(),
                User.withUsername("teller").password(encoder.encode("teller123")).roles("TELLER").build()
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // simplifies calling the API from the plain JS frontend/demo tools
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/h2-console/**").permitAll()
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "TELLER")
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "TELLER")
                .anyRequest().permitAll()
            )
            .httpBasic(basic -> {})
            .headers(headers -> headers.frameOptions(frame -> frame.disable())); // needed for H2 console

        return http.build();
    }
}
