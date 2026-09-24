package com.tsix.immocrm.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the deployed frontend origin plus any localhost origin (regardless of port) to call the
 * API — the latter covers local frontend dev servers (e.g. Vite on 5173, CRA on 3000) without
 * hardcoding a specific port. Also the single place where {@link PerimetreInterceptor} (périmètre
 * commercial) is wired into the MVC stack.
 * <p>
 * {@code allowCredentials(true)} is required by the JWT-in-httpOnly-cookie scheme, and it rules
 * out the {@code *} wildcard: every production origin has to be listed here explicitly. A new
 * deployment domain therefore means editing this file — there is no configuration escape hatch.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final PerimetreInterceptor perimetreInterceptor;

    public CorsConfig(PerimetreInterceptor perimetreInterceptor) {
        this.perimetreInterceptor = perimetreInterceptor;
    }

    /**
     * Le périmètre commercial ne s'applique qu'à ces quatre collections — patrimoine, calendrier,
     * hotline, clients et prospects en sont explicitement exclus (décision produit, §6.5 de
     * {@code instructions/FEATURE-perimetre-commercial.md}).
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
