package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.dto.auth.ConnexionRequest;
import com.tsix.immocrm.dto.auth.UtilisateurCourantDto;
import com.tsix.immocrm.security.CookieJwt;
import com.tsix.immocrm.security.JwtService;
import com.tsix.immocrm.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentification", description = "Connexion/déconnexion par cookie JWT httpOnly.")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieJwt cookieJwt;

    public AuthController(AuthService authService, JwtService jwtService, CookieJwt cookieJwt) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieJwt = cookieJwt;
    }

    @PostMapping("/connexion")
    @Operation(summary = "Connexion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie, cookie posé."),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides ou compte désactivé.")
    })
    public UtilisateurCourantDto connexion(@RequestBody ConnexionRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.connexion(request.username(), request.password());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieJwt.poser(result.token(), jwtService.expirationSeconds()).toString());
        return result.utilisateur();
    }

    @PostMapping("/deconnexion")
    @Operation(summary = "Déconnexion")
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Cookie effacé."))
    public ResponseEntity<Void> deconnexion(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieJwt.effacer().toString());
        return ResponseEntity.noContent().build();
    }
}
