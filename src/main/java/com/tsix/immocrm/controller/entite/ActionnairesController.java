package com.tsix.immocrm.controller.entite;

import com.tsix.immocrm.dto.entite.ActionnaireDto;
import com.tsix.immocrm.dto.entite.ActionnaireEcritureDto;
import com.tsix.immocrm.dto.entite.GroupeActionnairesDto;
import com.tsix.immocrm.dto.entite.GroupeActionnairesEcritureDto;
import com.tsix.immocrm.services.patrimoine.ActionnairesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.entite.EntiteJuridiqueController.ADMIN;
import static com.tsix.immocrm.controller.entite.EntiteJuridiqueController.PAS_ADMIN;

@Tag(name = "Actionnaires", description = "Actionnaires (CfgShareholder) et groupes d'actionnaires (CfgGroupShareholder) ; "
        + "écriture Administrateur.")
@RestController
public class ActionnairesController {

    private static final String ACTIONNAIRES = "/api/actionnaires";
    private static final String ACTIONNAIRE = ACTIONNAIRES + "/{id}";
    private static final String GROUPES = "/api/groupes-actionnaires";
    private static final String GROUPE = GROUPES + "/{id}";
    private static final String ID_ACTIONNAIRE = "Id de l'actionnaire (CfgShareholder.PKey).";
    private static final String ID_GROUPE = "Id du groupe (CfgGroupShareholder.PKey).";
    private static final String ACTIONNAIRE_INTROUVABLE = "Actionnaire introuvable.";
    private static final String GROUPE_INTROUVABLE = "Groupe introuvable.";
    private static final String CODE_PRIS = "Code déjà pris.";
    private static final String GROUPE_INVALIDE =
            "Code ou description manquant, somme des pourcentages ≠ 100 %, actionnaire en double ou inconnu.";

    private final ActionnairesService service;

    public ActionnairesController(ActionnairesService service) {
        this.service = service;
    }

    @GetMapping(ACTIONNAIRES)
    @Operation(summary = "Actionnaires, par code")
    public List<ActionnaireDto> actionnaires() {
        return service.actionnaires();
    }

    @GetMapping(ACTIONNAIRE)
    @Operation(summary = "Fiche d'un actionnaire")
    @ApiResponse(responseCode = "404", description = ACTIONNAIRE_INTROUVABLE)
    public ActionnaireDto actionnaire(@Parameter(description = ID_ACTIONNAIRE) @PathVariable UUID id) {
        return service.actionnaire(id);
    }

    @PostMapping(ACTIONNAIRES)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Crée un actionnaire")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Actionnaire créé."),
            @ApiResponse(responseCode = "400", description = "Code ou description manquant."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "409", description = CODE_PRIS)
    })
    public ActionnaireDto creerActionnaire(@RequestBody ActionnaireEcritureDto saisie) {
        return service.creerActionnaire(saisie);
    }

    @PatchMapping(ACTIONNAIRE)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Modifie un actionnaire (absent ou null = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actionnaire modifié."),
            @ApiResponse(responseCode = "400", description = "Code ou description vidé."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = ACTIONNAIRE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = CODE_PRIS)
    })
    public ActionnaireDto modifierActionnaire(@Parameter(description = ID_ACTIONNAIRE) @PathVariable UUID id,
                                              @RequestBody ActionnaireEcritureDto saisie) {
        return service.modifierActionnaire(id, saisie);
    }

    @DeleteMapping(ACTIONNAIRE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime un actionnaire absent de toute répartition et de tout groupe")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Actionnaire supprimé."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = ACTIONNAIRE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Cité par une répartition d'entité ou une ligne de groupe.")
    })
    public void supprimerActionnaire(@Parameter(description = ID_ACTIONNAIRE) @PathVariable UUID id) {
        service.supprimerActionnaire(id);
    }

    @GetMapping(GROUPES)
    @Operation(summary = "Groupes d'actionnaires avec leurs lignes, par code")
    public List<GroupeActionnairesDto> groupes() {
        return service.groupes();
    }

    @GetMapping(GROUPE)
    @Operation(summary = "Fiche d'un groupe d'actionnaires")
    @ApiResponse(responseCode = "404", description = GROUPE_INTROUVABLE)
    public GroupeActionnairesDto groupe(@Parameter(description = ID_GROUPE) @PathVariable UUID id) {
        return service.groupe(id);
    }

    @PostMapping(GROUPES)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Crée un groupe et ses lignes (somme des pourcentages = 100 %)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Groupe créé."),
            @ApiResponse(responseCode = "400", description = GROUPE_INVALIDE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "409", description = CODE_PRIS)
    })
    public GroupeActionnairesDto creerGroupe(@RequestBody GroupeActionnairesEcritureDto saisie) {
        return service.creerGroupe(saisie);
    }

    @PatchMapping(GROUPE)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Modifie un groupe ; lignes présentes = remplacées")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groupe modifié."),
            @ApiResponse(responseCode = "400", description = GROUPE_INVALIDE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = GROUPE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = CODE_PRIS)
    })
    public GroupeActionnairesDto modifierGroupe(@Parameter(description = ID_GROUPE) @PathVariable UUID id,
                                                @RequestBody GroupeActionnairesEcritureDto saisie) {
        return service.modifierGroupe(id, saisie);
    }

    @DeleteMapping(GROUPE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime un groupe qu'aucune entité ne choisit")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Groupe et lignes supprimés."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = GROUPE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Une entité juridique a choisi ce groupe (la supprimer l'emporterait).")
    })
    public void supprimerGroupe(@Parameter(description = ID_GROUPE) @PathVariable UUID id) {
        service.supprimerGroupe(id);
    }
}
