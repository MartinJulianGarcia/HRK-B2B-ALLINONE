package com.hrk.tienda_b2b.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpaRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirigir cualquier ruta que no sea API ni recursos estáticos al index de Angular
        registry.addViewController("/")
                .setViewName("forward:/index.html");

        registry.addViewController("/{spring:[\\w\\-]+}")
                .setViewName("forward:/index.html");

        registry.addViewController("/**/{spring:[\\w\\-]+}")
                .setViewName("forward:/index.html");

        registry.addViewController("/{spring:[\\w\\-]+}/**{spring:?!(\\.js|\\.css|\\.png|\\.jpg|\\.jpeg|\\.gif|\\.svg|\\.ico)$}")
                .setViewName("forward:/index.html");
    }
}

