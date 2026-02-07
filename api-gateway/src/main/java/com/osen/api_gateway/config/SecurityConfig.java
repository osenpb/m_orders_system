package com.osen.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/public/**").permitAll()
                .anyExchange().authenticated()           // Todo lo demás requiere login
            )
            .oauth2Login(Customizer.withDefaults())      // Habilita OAuth2 para navegador
            .oauth2Client(Customizer.withDefaults())     // Permite al Gateway actuar como cliente
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())) // Acepta Bearer tokens
            .csrf(csrf-> csrf.disable());
        return http.build();
    }
}
