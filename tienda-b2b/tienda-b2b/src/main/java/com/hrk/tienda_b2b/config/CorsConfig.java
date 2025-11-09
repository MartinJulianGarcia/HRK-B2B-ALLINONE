
package com.hrk.tienda_b2b.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Permitir credenciales
        config.setAllowCredentials(true);

        // Permitir origen del frontend (local y túneles HTTPS como ngrok)
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.ngrok-free.dev",
                "https://*.ngrok.app",
                "https://*.ngrok.io"
        ));

        // Permitir todos los headers
        config.addAllowedHeader("*");

        // Permitir todos los métodos HTTP
        config.addAllowedMethod("*");

        // ⭐ AGREGAR ESTA LÍNEA - Configurar CORS para /uploads
        source.registerCorsConfiguration("/uploads/**", config);
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}