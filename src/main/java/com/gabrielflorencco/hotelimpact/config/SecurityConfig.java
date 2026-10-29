package com.gabrielflorencco.hotelimpact.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança da aplicação: o site institucional (página inicial, sobre e
 * funcionalidades) e o login são públicos; toda a área administrativa exige
 * usuário autenticado, com login por formulário próprio (pt-BR), logout via
 * POST na topbar e CSRF habilitado (padrão). Após o login, a recepção cai
 * direto no painel operacional.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/sobre", "/funcionalidades").permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/css/*", "/js/*", "/vendor/**", "/img/*", "/images/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/painel", true)
                        .permitAll())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HxRedirectEntryPoint("/login")))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }
}
