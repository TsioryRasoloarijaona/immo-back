package com.tsix.immocrm.security;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
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
 * Toute route exige un principal, hors login, doc OpenAPI et sondes actuator : le périmètre se déduit du principal,
 * donc une route ouverte est une route sans périmètre (FEATURE-perimetre-commercial.md §9bis.3). Pas d'exception
 * de confort. Les règles actuator s'appliquent aussi au port de management (9091) : elles sont porteuses.
 */
@SecurityScheme(name = "cleApi", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
        paramName = ApiKeyAuthenticationFilter.EN_TETE,
        description = "Clé d'API d'un client machine (n8n, agent), créée par POST /api/cles-api. "
                + "Authentifie le compte de service de la clé et n'autorise que les routes de ses "
                + "portées (403 `HORS_PORTEE` sinon). Le front, lui, utilise le cookie JWT posé par "
                + "POST /api/auth/connexion.")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMINISTRATEUR = "Administrateur";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) {
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                // CSRF désactivé : SameSite=Strict le remplace tant que le front partage le domaine (CookieJwt).
                // En SameSite=None, les POST multipart de pièces jointes (type simple, sans préflight) seraient
                // exposés : il faudrait alors exiger un en-tête applicatif sur les méthodes non sûres.
                .csrf(csrf -> csrf.disable())
                // Bean CorsConfigurationSource de CorsConfig : en-têtes CORS aussi sur les 401/403 de cette chaîne.
                .cors(Customizer.withDefaults())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401 = session expirée côté front (déconnexion), 403 = rôle manquant. setStatus et non sendError :
                // sendError(403) forwarde vers /error, où le contexte est vidé et la réponse récrite en 401.
                // Invisible sous MockMvc (pas de dispatch ERROR) ; voir AccesRefuseTest, qui passe par le vrai cookie.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, denied) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value())))
                .authorizeHttpRequests(auth -> auth
                        // Un préflight CORS ne porte jamais le cookie : sans cette règle, tout appel cross-origin casse.
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        // Fin d'un flux SSE (/api/notifications/flux) : redispatch ASYNC d'une requête déjà autorisée,
                        // sans cookie relu ; sans cette règle il finirait en 401 sur une réponse déjà envoyée.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        // Le dispatch ERROR (sendError → /error) relit la chaîne avec un contexte vide : sans cette règle, un 500
                        // devenait 401 et déconnectait le front. /error n'est pas ouvert en accès direct (FEATURE-erreur-500-401.md).
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Chemins exacts, jamais /** : /actuator/metrics doit rester fermé. health reste muet tant que
                        // show-details=never ; info ne porte que tag d'image et commit, déjà publics (InfosBuild).
                        .requestMatchers("/actuator/health", "/actuator/prometheus", "/actuator/info").permitAll()
                        // Avant la liaison des paramètres : 403 avant tout 400 (FEATURE-patrimoine-import-export.md §0.1).
                        .requestMatchers("/api/import-patrimoine/**").hasRole(ADMINISTRATEUR)
                        .requestMatchers("/api/tranches/*/premieres-relances", "/api/tranches/*/premieres-relances/**",
                                "/api/tranches/*/premieres-relances.pdf", "/api/champs-specifiques/*/definitions/**")
                        .hasRole(ADMINISTRATEUR)
                        .requestMatchers(HttpMethod.PUT, "/api/champs-specifiques/**").hasRole(ADMINISTRATEUR)
                        .anyRequest().authenticated())
                // Clé d'API AVANT le JWT : une clé présente fait foi, le cookie n'est lu qu'en son absence (FEATURE-cle-api-n8n.md §5).
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
