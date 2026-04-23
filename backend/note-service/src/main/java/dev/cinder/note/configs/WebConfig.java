package dev.cinder.note.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web-layer config: CORS, interceptors, etc.
 * Auth config will live in a separate SecurityConfig when added.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cinder.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type")
                .allowCredentials(false) // no cookies / no auth headers
                .maxAge(3600); // browser caches preflight for 1hr
    }
}
