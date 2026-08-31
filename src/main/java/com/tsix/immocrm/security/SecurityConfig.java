package com.tsix.immocrm.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;

/**
 * Phase 2 of the auth rollout: the whole API now requires an authenticated caller (a valid
 * {@code access_token} cookie, parsed by {@link JwtAuthenticationFilter}) except
 * {@code /api/auth/**} (login itself) and the Swagger/OpenAPI doc endpoints. See
 * {@link OnboardingGateFilter} for the extra "première connexion" gate layered on top.
 * <p>
 * Phase 3 adds {@link EnableMethodSecurity} so {@code @PreAuthorize("hasRole(...)")} works on
 * individual endpoints — {@code UtilisateurAdminController} is the first (and, until another
 * endpoint needs one, only) place in the app that checks a specific role rather than just
 * "authenticated at all".
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OnboardingGateFilter onboardingGateFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, OnboardingGateFilter onboardingGateFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.onboardingGateFilter = onboardingGateFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless JWT-in-httpOnly-cookie auth, no server-side session — CSRF strategy
                // (SameSite=Strict already blocks cross-site form/script submission of the
                // cookie; revisit with a double-submit token if a cross-subdomain frontend needs
                // SameSite=Lax instead).
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // Real browser CORS preflight requests never carry the cookie — must be
                        // let through unconditionally or actual cross-origin calls break in prod.
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Générateurs de documents (PDF/Word) ouverts sans authentification pour
                        // permettre les tests manuels — TEMPORAIRE, à retirer avant mise en prod
                        // (voir SESSION_MEMORY.md). Ne couvre que les endpoints de génération, pas
                        // le reste de /api/dossiers ou /api/prospections.
                        .requestMatchers("/api/dossiers/*/documents/**").permitAll()
                        .requestMatchers("/api/prospections/*/documents/**").permitAll()
                        .requestMatchers("/api/prospections/*/devis/*/document").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(onboardingGateFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
