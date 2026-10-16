package com.tsix.immocrm.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS et branchement de {@link PerimetreInterceptor}. {@code allowCredentials(true)} (cookie JWT) interdit {@code *} :
 * toute origine de production doit être listée ici, il n'y a pas de réglage externe.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final PerimetreInterceptor perimetreInterceptor;
    private final SuiviUtilisateursInterceptor suiviUtilisateursInterceptor;

    public CorsConfig(PerimetreInterceptor perimetreInterceptor,
                      SuiviUtilisateursInterceptor suiviUtilisateursInterceptor) {
        this.perimetreInterceptor = perimetreInterceptor;
        this.suiviUtilisateursInterceptor = suiviUtilisateursInterceptor;
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
        registry.addInterceptor(suiviUtilisateursInterceptor).addPathPatterns("/api/**");
    }

    /**
     * Lu par le filtre CORS de Spring Security ({@code SecurityConfig}, {@code .cors(...)}) et non par MVC : les 401/403
     * de la chaîne de sécurité partent avant MVC et doivent porter les en-têtes, sinon le front voit une erreur CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "https://immo.tsiory.tech",
                "https://arpent.ayaddeveloppement.com",
                "http://localhost:*", "http://192.168.11.*:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
