package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.dto.auth.ChangerMotDePasseRequest;
import com.tsix.immocrm.dto.auth.CompleterProfilRequest;
import com.tsix.immocrm.dto.auth.UtilisateurCourantDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.auth.AuthService;
import com.tsix.immocrm.services.auth.MonCompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mon compte", description = "Profil de l'utilisateur authentifié — les seules routes "
        + "accessibles (avec /api/auth/**) tant que mustChangePassword/profileCompleted bloquent le reste de l'API.")
@RestController
@RequestMapping("/api/mon-compte")
public class MonCompteController {

    private final MonCompteService monCompteService;
    private final boolean cookieSecure;

    public MonCompteController(MonCompteService monCompteService, @Value("${app.jwt.cookie-secure}") boolean cookieSecure) {
        this.monCompteService = monCompteService;
        this.cookieSecure = cookieSecure;
    }

    @GetMapping
    @Operation(summary = "Profil courant", description = "Le compte authentifié : identité, "
            + "rôles, et l'état mustChangePassword/profileCompleted qui pilote l'onboarding.")
    public UtilisateurCourantDto moi(@AuthenticationPrincipal AuthenticatedUser principal) {
        return monCompteService.moi(principal);
    }

    @PostMapping("/changer-mot-de-passe")
    @Operation(
            summary = "Change le mot de passe",
            description = "Vérifie ancienMotDePasse puis stocke nouveauMotDePasse (bcrypt, min. "
                    + "8 caractères). Efface mustChangePassword et réémet un cookie access_token "
                    + "à jour — invalide tous les tokens émis avant cet appel (TokenVersion "
                    + "incrémenté), y compris sur d'autres appareils."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe changé, nouveau cookie posé."),
            @ApiResponse(responseCode = "400", description = "Champs manquants, nouveau mot de passe trop court, ou ancien mot de passe incorrect.")
    })
    public UtilisateurCourantDto changerMotDePasse(@AuthenticationPrincipal AuthenticatedUser principal,
                                                     @RequestBody ChangerMotDePasseRequest request,
                                                     HttpServletResponse response) {
        AuthService.LoginResult result = monCompteService.changerMotDePasse(principal, request);
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(result.token()).toString());
        return result.utilisateur();
    }

    @PatchMapping("/profil")
    @Operation(
            summary = "Complète le profil (\"première connexion\")",
            description = "Renseigne prenom/nom/telephone/email/dateNaissance (adresse "
                    + "optionnelle) sur la fiche employé (CfgTier) liée au compte, et marque "
                    + "profileCompleted=true. Réémet un cookie access_token à jour."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil complété, nouveau cookie posé."),
            @ApiResponse(responseCode = "400", description = "Un champ obligatoire manque, ou le compte n'est lié à aucune fiche employé.")
    })
    public UtilisateurCourantDto completerProfil(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @RequestBody CompleterProfilRequest request,
                                                   HttpServletResponse response) {
        AuthService.LoginResult result = monCompteService.completerProfil(principal, request);
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(result.token()).toString());
        return result.utilisateur();
    }

    private ResponseCookie buildCookie(String token) {
        return ResponseCookie.from(AuthController.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .build();
    }
}
