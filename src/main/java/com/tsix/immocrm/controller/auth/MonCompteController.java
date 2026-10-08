package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.dto.auth.ChangerMotDePasseRequest;
import com.tsix.immocrm.dto.auth.CompleterProfilRequest;
import com.tsix.immocrm.dto.auth.UtilisateurCourantDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.CookieJwt;
import com.tsix.immocrm.services.auth.AuthService;
import com.tsix.immocrm.services.auth.MonCompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mon compte")
@RestController
@RequestMapping("/api/mon-compte")
public class MonCompteController {

    private final MonCompteService monCompteService;
    private final CookieJwt cookieJwt;

    public MonCompteController(MonCompteService monCompteService, CookieJwt cookieJwt) {
        this.monCompteService = monCompteService;
        this.cookieJwt = cookieJwt;
    }

    @GetMapping
    @Operation(summary = "Profil courant")
    public UtilisateurCourantDto moi(@AuthenticationPrincipal AuthenticatedUser principal) {
        return monCompteService.moi(principal);
    }

    @PostMapping("/changer-mot-de-passe")
    @Operation(summary = "Change le mot de passe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe changé, nouveau cookie posé."),
            @ApiResponse(responseCode = "400", description = "Champs manquants, nouveau mot de passe trop court, ou ancien mot de passe incorrect.")
    })
    public UtilisateurCourantDto changerMotDePasse(@AuthenticationPrincipal AuthenticatedUser principal,
                                                     @RequestBody ChangerMotDePasseRequest request,
                                                     HttpServletResponse response) {
        AuthService.LoginResult result = monCompteService.changerMotDePasse(principal, request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieJwt.poserSession(result.token()).toString());
        return result.utilisateur();
    }

    @PatchMapping("/profil")
    @Operation(summary = "Complète le profil (\"première connexion\")")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil complété, nouveau cookie posé."),
            @ApiResponse(responseCode = "400", description = "Un champ obligatoire manque, ou le compte n'est lié à aucune fiche employé.")
    })
    public UtilisateurCourantDto completerProfil(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @RequestBody CompleterProfilRequest request,
                                                   HttpServletResponse response) {
        AuthService.LoginResult result = monCompteService.completerProfil(principal, request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieJwt.poserSession(result.token()).toString());
        return result.utilisateur();
    }

}
