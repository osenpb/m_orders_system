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
            .oauth2Login(Customizer.withDefaults())      // Habilita OAuth2
            .oauth2Client(Customizer.withDefaults())     // Permite al Gateway actuar como cliente
            //.csrf(ServerHttpSecurity.CsrfSpec::disable); // Desactivar CSRF para pruebas (en prod se evalúa)
            .csrf(csrf-> csrf.disable());
        return http.build();
    }
}
