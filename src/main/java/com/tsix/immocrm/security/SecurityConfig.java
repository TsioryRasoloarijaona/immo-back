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
 * {@code /actuator/health} et {@code /actuator/prometheus} sont ouverts, et ce sont les seules
 * exceptions <b>non applicatives</b> : aucune donnée métier n'y transite. {@code health} ne rend
 * que {@code {"status":"UP"}} — {@code show-details=never} est posé dans
 * {@code application.properties} et doit y rester, sinon l'endpoint divulguerait l'état du
 * {@code DataSource} et du stockage objet. Elles existent parce que leurs appelants — sonde de
 * déploiement du pipeline, scrape Prometheus — n'ont aucun cookie JWT à présenter : derrière
 * l'authentification ils recevraient {@code 401}, et la sonde déclarerait le conteneur en échec
 * en permanence.
 * <p>
 * <b>Cette chaîne de filtres couvre bien le port de management.</b> {@code
 * management.server.port=9091} déplace l'actuator sur un port dédié, non proxifié par Caddy et
 * publié sur {@code 127.0.0.1} seulement — mais la sécurité continue de s'y appliquer. Vérifié
 * en production : {@code 9091/actuator/health} rend {@code 200} sans cookie (la règle matche),
 * {@code 9091/actuator/metrics} rend {@code 401} (aucune règle ne le couvre). Ces deux lignes
 * sont donc <b>porteuses</b>, pas décoratives : les retirer casse la sonde de déploiement et le
 * scrape.
 * <p>
 * Sur le port applicatif 8080 en revanche, ces chemins n'ont plus de handler MVC, donc le
 * matcher ne matche pas et {@code anyRequest().authenticated()} rend {@code 401}. C'est pourquoi
 * {@code https://api.tsiory.tech/actuator/health} ne répond plus depuis le passage au port de
 * management — ce n'est pas une panne.
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
                // Authentification sans session serveur, JWT dans un cookie httpOnly.
                //
                // ATTENTION — la justification historique de ce disable() ne tient plus. Elle
                // reposait sur SameSite=Strict, qui empêchait le navigateur d'attacher le cookie
                // à une requête cross-site. Le frontend étant désormais servi depuis un autre
                // site, le cookie est passé à SameSite=None (voir CookieJwt) : il part avec
                // toute requête cross-site, y compris celles d'une page hostile.
                //
                // Ce qui protège encore : les endpoints qui modifient l'état consomment du JSON,
                // et application/json n'est pas un type de contenu « simple » — le navigateur
                // impose un préflight, que CORS refuse à une origine non listée.
                //
                // Ce qui n'est PAS protégé : les deux POST multipart de pièces jointes
                // (/api/dossiers/{id}/pieces-jointes et .../reglements/{id}/pieces-jointes).
                // multipart/form-data EST un type simple : un formulaire hébergé n'importe où
                // peut les soumettre sans préflight, cookie attaché. L'attaquant ne lit pas la
                // réponse (CORS l'en empêche), mais l'écriture aboutit.
                //
                // Correctif retenu si on ferme ce trou : exiger un en-tête applicatif sur les
                // méthodes non sûres — sa seule présence force un préflight, donc le contrôle
                // d'origine. Un double-submit par cookie ne conviendrait pas : le frontend est
                // sur un autre domaine et ne peut pas lire un cookie posé par l'API.
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
                        // Chemins exacts, jamais /** : /actuator/metrics doit rester fermé.
                        // Voir le javadoc de la classe pour la portée réelle de ces deux règles.
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
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
