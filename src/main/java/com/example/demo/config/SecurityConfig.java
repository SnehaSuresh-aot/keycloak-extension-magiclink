package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/auth/login", "/auth/login/**", "/auth/generate/magic-link").permitAll()  // Allow access to login and magic link generation
                                .anyRequest().permitAll() // Allow all other requests without authentication
                )
                .formLogin(formLogin ->
                        formLogin
                                .loginPage("/auth/login")// Custom login page
                                .permitAll()  // Allow access to the login page without authentication
                )
                .logout(logout ->
                        logout.permitAll()  // Allow logout without authentication
                )
                .csrf(csrf ->
                        csrf.disable()  // Disable CSRF protection
                );

        return http.build();  // Return the security filter chain
    }
}
