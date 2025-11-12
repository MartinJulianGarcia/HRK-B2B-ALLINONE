package com.hrk.tienda_b2b.config;

import com.hrk.tienda_b2b.security.JwtAuthenticationEntryPoint;
import com.hrk.tienda_b2b.security.JwtRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/manifest.json",
                                "/uploads/**",
                                "/images/**",
                                "/public/**",
                                "/static/**",
                                "/HIRKUM-MONOGRAMA-B.jpg",
                                "/**/*.js",
                                "/**/*.css",
                                "/**/*.html",
                                "/**/*.png",
                                "/**/*.jpg",
                                "/**/*.jpeg",
                                "/**/*.gif",
                                "/**/*.svg",
                                "/**/*.webp"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/catalog",
                                "/catalog/**",
                                "/cart",
                                "/cart/**",
                                "/profile",
                                "/profile/**",
                                "/orders-history",
                                "/orders-history/**",
                                "/devolucion",
                                "/devolucion/**",
                                "/login",
                                "/register",
                                "/home",
                                "/info",
                                "/add-product",
                                "/add-product/**",
                                "/edit-product",
                                "/edit-product/**",
                                "/dashboard",
                                "/dashboard/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}

