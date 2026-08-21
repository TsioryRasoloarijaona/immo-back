package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.dto.auth.ReinitialiserMotDePasseResponse;
import com.tsix.immocrm.services.auth.UtilisateurAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
