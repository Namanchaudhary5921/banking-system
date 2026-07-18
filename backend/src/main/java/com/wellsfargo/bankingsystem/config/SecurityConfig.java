package com.wellsfargo.bankingsystem.config;

import com.wellsfargo.bankingsystem.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                              PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authProvider) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authenticationProvider(authProvider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/h2-console/**").permitAll()
                .requestMatchers("/api/me/**").authenticated()
                .requestMatchers("/api/auth/**").hasRole("ADMIN")
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "TELLER")
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "TELLER")
                .anyRequest().permitAll()
            )
            .httpBasic(basic -> {})
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
