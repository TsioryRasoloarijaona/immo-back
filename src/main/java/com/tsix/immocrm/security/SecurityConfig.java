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
 * Il n'y a plus <b>aucune</b> exception applicative à {@code authenticated()} : le trou
 * {@code permitAll} qui laissait passer les générateurs de documents a été refermé avec le
 * périmètre commercial. Ce point est structurant pour l'autorisation et pas seulement pour
 * l'authentification — {@code PerimetreCommercialService} déduit le périmètre du principal, donc
 * toute route accessible sans principal est, par construction, une route sans périmètre.
 * N'y rajoutez pas d'exception pour du confort de test manuel.
 * <p>
 * {@code /actuator/health} est ouvert, et c'est la seule exception <b>non applicative</b> : elle
 * ne sert aucune donnée métier, seulement {@code {"status":"UP"}} — {@code show-details=never}
 * est posé dans {@code application.properties} et doit y rester, sinon l'endpoint divulguerait
 * l'état du {@code DataSource} et du stockage objet. Elle existe parce qu'une sonde de
 * déploiement (HEALTHCHECK Docker, vérification post-déploiement du pipeline) n'a pas de cookie
 * JWT à présenter : derrière l'authentification, elle recevrait {@code 401} et déclarerait le
 * conteneur en échec en permanence.
 * <p>
 * <b>En production cette règle ne s'applique plus</b> : {@code management.server.port=9091}
 * déplace tout l'actuator sur un port dédié, qui n'est pas celui que Caddy proxifie, et cette
 * chaîne de filtres ne le couvre pas. La règle reste ici comme <b>filet</b> — si le port de
 * management était un jour retiré, l'actuator retomberait sur 8080 et seul {@code health} y
 * serait ouvert, plutôt que tout. Ne pas la supprimer en la croyant morte.
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
                        // Sonde de déploiement uniquement — chemin exact, pas de /**, pour ne pas
                        // ouvrir /actuator/metrics au passage. Voir le javadoc de la classe.
                        .requestMatchers("/actuator/health").permitAll()
                        // Les générateurs de documents (PDF/Word) étaient ouverts en permitAll pour
                        // permettre des tests manuels, marqué TEMPORAIRE. Refermé avec le périmètre
                        // commercial : sans authentification il n'y a pas de principal, donc pas de
                        // périmètre — n'importe quel PDF de dossier (client, prix, échéancier)
                        // était téléchargeable sans le moindre cookie, ce qui vidait la feature de
                        // son sens pour tout document généré. Le front envoie déjà
                        // credentials:'include' partout, l'impact UI est nul.
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(onboardingGateFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
