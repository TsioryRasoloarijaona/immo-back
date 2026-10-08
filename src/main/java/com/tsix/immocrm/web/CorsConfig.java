package com.tsix.immocrm.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS et branchement de {@link PerimetreInterceptor}. {@code allowCredentials(true)} (cookie JWT) interdit {@code *} :
 * toute origine de production doit être listée ici, il n'y a pas de réglage externe.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final PerimetreInterceptor perimetreInterceptor;

    public CorsConfig(PerimetreInterceptor perimetreInterceptor) {
        this.perimetreInterceptor = perimetreInterceptor;
    }

    /**
     * Contrôle par id sur ces quatre collections seulement (FEATURE-perimetre-commercial.md §6.5) ; les sous-ressources
     * clients, prospects, devis et hotline sont filtrées dans leurs services (§9bis.2).
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(perimetreInterceptor)
                .addPathPatterns("/api/dossiers/**", "/api/prospections/**",
                        "/api/pre-prospections/**", "/api/encaissements/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "https://immo.tsiory.tech",
                        "https://arpent.ayaddeveloppement.com",
                        "http://192.168.11.114:5473", "http://localhost:*", "http://192.168.11.*:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
