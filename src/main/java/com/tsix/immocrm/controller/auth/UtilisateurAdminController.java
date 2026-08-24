package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.dto.auth.CreerEmployeRequest;
import com.tsix.immocrm.dto.auth.CreerEmployeResponse;
import com.tsix.immocrm.dto.auth.ReinitialiserMotDePasseResponse;
import com.tsix.immocrm.services.auth.UtilisateurAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Utilisateurs (admin)", description = "Actions administratives sur les comptes "
        + "AdmUser — réservées au rôle Administrateur.")
@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurAdminController {

    private final UtilisateurAdminService utilisateurAdminService;

    public UtilisateurAdminController(UtilisateurAdminService utilisateurAdminService) {
        this.utilisateurAdminService = utilisateurAdminService;
    }

    @PostMapping("/{admUserId}/reinitialiser-mot-de-passe")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Réinitialise le mot de passe d'un utilisateur",
            description = "Génère un mot de passe temporaire, le renvoie une seule fois dans la "
                    + "réponse (à communiquer par un canal hors-bande — jamais stocké en clair), "
                    + "et force l'utilisateur à le changer à sa prochaine connexion "
                    + "(mustChangePassword=true). Invalide immédiatement tous les tokens déjà "
                    + "émis pour ce compte, sur tous les appareils. Crée l'enregistrement "
                    + "d'identifiants web si ce compte legacy (AdmUser) ne s'était encore jamais "
                    + "connecté à l'application web. Réservé au rôle Administrateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe temporaire généré."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Aucun AdmUser avec cet id.")
    })
    public ReinitialiserMotDePasseResponse reinitialiserMotDePasse(
            @Parameter(description = "Id de l'AdmUser à réinitialiser.") @PathVariable UUID admUserId) {
        return utilisateurAdminService.reinitialiserMotDePasse(admUserId);
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Crée un nouvel employé",
            description = "Crée en une seule opération la fiche personne (CfgTier), le compte de "
                    + "connexion (AdmUser) qui lui est lié, les rôles demandés (AdmUserRole), et "
                    + "génère un mot de passe temporaire renvoyé une seule fois dans la réponse "
                    + "(à communiquer par un canal hors-bande — jamais stocké en clair). Le "
                    + "compte devra changer ce mot de passe et compléter son profil à sa première "
                    + "connexion (mustChangePassword=true, profileCompleted=false — voir "
                    + "GET /api/mon-compte). Réservé au rôle Administrateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employé créé."),
            @ApiResponse(responseCode = "400", description = "Champ requis manquant, ou "
                    + "typeEmployeCode/rôle référençant un code inconnu en base."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "409", description = "L'identifiant demandé est déjà utilisé "
                    + "par un autre compte.")
    })
    public ResponseEntity<CreerEmployeResponse> creerEmploye(@Valid @RequestBody CreerEmployeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurAdminService.creerEmploye(request));
    }
}
