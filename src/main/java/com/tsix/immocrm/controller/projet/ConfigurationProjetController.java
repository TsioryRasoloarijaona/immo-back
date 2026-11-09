package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.ProjetConfigDto;
import com.tsix.immocrm.dto.projet.ProjetConfigRequest;
import com.tsix.immocrm.dto.projet.ReferentielsProjetConfigDto;
import com.tsix.immocrm.dto.projet.StatutLotDto;
import com.tsix.immocrm.services.projet.ProjetConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projet — configuration")
@RestController
@RequestMapping("/api/projet/config")
public class ConfigurationProjetController {

    private static final String ADMIN = "hasRole('Administrateur')";
    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";
    private static final String ID_PROJET = "Id du projet (PrjProject.PKey).";
    private static final String INTROUVABLE = "Projet introuvable.";
    private static final String SAISIE = "Code, libellé ou société manquant, société ou gestionnaire inconnu, texte trop long.";
    private static final String CODE_PRIS = "Code déjà utilisé par un autre projet.";

    private final ProjetConfigService service;

    public ConfigurationProjetController(ProjetConfigService service) {
        this.service = service;
    }

    @GetMapping("/projets")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Tous les projets, par code, hors périmètre (écran d'administration)")
    @ApiResponse(responseCode = "403", description = PAS_ADMIN)
    public List<ProjetConfigDto> lister() {
        return service.lister();
    }

    @GetMapping("/projets/{id}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Fiche d'un projet")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public ProjetConfigDto lire(@Parameter(description = ID_PROJET) @PathVariable UUID id) {
        return service.lire(id);
    }

    @PostMapping("/projets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Crée un projet et habilite son créateur dans la même transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Projet créé, statut « Nouveau »."),
            @ApiResponse(responseCode = "400", description = SAISIE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "409", description = CODE_PRIS)
    })
    public ProjetConfigDto creer(@RequestBody ProjetConfigRequest saisie) {
        return service.creer(saisie);
    }

    @PutMapping("/projets/{id}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Remplace les champs saisissables d'un projet (statut et type d'objet inchangés)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = SAISIE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = CODE_PRIS)
    })
    public ProjetConfigDto modifier(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                    @RequestBody ProjetConfigRequest saisie) {
        return service.modifier(id, saisie);
    }

    @DeleteMapping("/projets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime un projet que rien ne référence, avec ses habilitations")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Projet et habilitations supprimés."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "« Utilisé par … » : une autre table référence le projet.")
    })
    public void supprimer(@Parameter(description = ID_PROJET) @PathVariable UUID id) {
        service.supprimer(id);
    }

    @GetMapping("/referentiels")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Listes de choix du formulaire projet (sociétés, statuts de projet, personnel)")
    @ApiResponse(responseCode = "403", description = PAS_ADMIN)
    public ReferentielsProjetConfigDto referentiels() {
        return service.referentiels();
    }

    @GetMapping("/statuts-lot")
    @Operation(summary = "Statuts de lot, groupés par genre de lot puis par ordre (lecture seule, tout authentifié)")
    public List<StatutLotDto> statutsLot() {
        return service.statutsLot();
    }
}
