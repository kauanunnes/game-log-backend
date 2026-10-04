package com.kauan.gamelog.shared.security;

import static com.kauan.gamelog.shared.WebConfig.API_PREFIX;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * API sem sessão no servidor: o access token vem no header {@code Authorization}. Não há CSRF porque só as rotas
 * de {@code /auth} leem cookie, e o cookie de refresh é {@code SameSite=Lax}.
 */
@Configuration
class SecurityConfig {

    /** 401 e 403 passam pelo {@code GlobalExceptionHandler} para sair no mesmo formato Problem Details. */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        AuthenticationEntryPoint unauthenticated =
                (request, response, ex) -> resolver.resolveException(request, response, null, ex);
        AccessDeniedHandler forbidden =
                (request, response, ex) -> resolver.resolveException(request, response, null, ex);

        return http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.requestMatchers(API_PREFIX + "/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers(API_PREFIX + "/me/**", API_PREFIX + "/users/*/follow")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromToken()))
                        .authenticationEntryPoint(unauthenticated)
                        .accessDeniedHandler(forbidden))
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(unauthenticated).accessDeniedHandler(forbidden))
                .build();
    }

    /** Fica no filtro do Spring Security para o preflight responder antes da checagem de login. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${cors.origins}") List<String> origins) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("*"));
        cors.setAllowedHeaders(List.of("*"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder(AuthProperties properties) {
        return new BCryptPasswordEncoder(properties.bcryptStrength());
    }

    /** O claim {@code roles} vira {@code ROLE_USER} ou {@code ROLE_ADMIN}. */
    private static JwtAuthenticationConverter rolesFromToken() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
