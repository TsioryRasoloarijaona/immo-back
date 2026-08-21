package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.dto.auth.ConnexionRequest;
import com.tsix.immocrm.dto.auth.UtilisateurCourantDto;
import com.tsix.immocrm.security.JwtService;
import com.tsix.immocrm.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentification", description = "Connexion/déconnexion par cookie JWT httpOnly.")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Name of the httpOnly cookie carrying the JWT. */
    public static final String COOKIE_NAME = "access_token";

    private final AuthService authService;
    private final JwtService jwtService;
    private final boolean cookieSecure;

    public AuthController(AuthService authService, JwtService jwtService,
                           @Value("${app.jwt.cookie-secure}") boolean cookieSecure) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/connexion")
    @Operation(
            summary = "Connexion",
            description = "Vérifie username/password contre AppUserCredential (bcrypt) et pose un "
                    + "cookie JWT httpOnly `access_token` (Secure, SameSite=Strict) valable "
                    + "app.jwt.expiration-minutes. Le corps de réponse contient le profil et les "
                    + "rôles, jamais le token lui-même. Si `mustChangePassword`/`profileCompleted` "
                    + "sont défavorables, seules les routes /api/auth/** et /api/mon-compte/** "
                    + "restent accessibles tant que l'utilisateur n'a pas régularisé sa situation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie, cookie posé."),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides ou compte désactivé.")
    })
    public UtilisateurCourantDto connexion(@RequestBody ConnexionRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.connexion(request.username(), request.password());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(result.token(), jwtService.expirationSeconds()).toString());
        return result.utilisateur();
    }

    @PostMapping("/deconnexion")
    @Operation(
            summary = "Déconnexion",
            description = "Efface le cookie access_token côté client. Ne révoque pas le token "
                    + "côté serveur avant son expiration naturelle (pas de session partagée entre "
                    + "appareils à invalider) — pour une révocation immédiate sur tous les "
                    + "appareils, voir le changement de mot de passe (bump de TokenVersion)."
    )
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Cookie effacé."))
    public ResponseEntity<Void> deconnexion(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", 0).toString());
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie buildCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
