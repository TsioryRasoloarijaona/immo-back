package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ActionnaireDto;
import com.tsix.immocrm.dto.patrimoine.ActionnaireEcritureDto;
import com.tsix.immocrm.services.patrimoine.ActionnaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Actionnaires", description = "Actionnaires (CfgShareholder) : Configuration › Autres d'OMRAN.")
@RestController
@RequestMapping("/api/actionnaires")
public class ActionnaireController {

    private static final String ID = "Identifiant de l'actionnaire (CfgShareholder.PKey).";
    private static final String REFUS_ROLE = "L'appelant n'a pas le rôle Administrateur.";
    private static final String INTROUVABLE = "Aucun actionnaire avec cet id.";
    private static final String CODE_PRIS = "Code déjà utilisé par un autre actionnaire (champ = code).";

    private final ActionnaireService service;

    public ActionnaireController(ActionnaireService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Liste des actionnaires, triée par code")
    @ApiResponse(responseCode = "200", description = "Actionnaires (éventuellement aucun).")
    public List<ActionnaireDto> lister(
            @Parameter(description = "Recherche sur code et libellé, espaces ignorés des deux côtés, casse ignorée.")
            @RequestParam(required = false) String search) {
        return service.lister(search);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un actionnaire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actionnaire."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public ActionnaireDto fiche(@Parameter(description = ID) @PathVariable UUID id) {
        return service.fiche(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Crée un actionnaire")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Actionnaire créé."),
            @ApiResponse(responseCode = "400", description = "Code ou description manquant ; texte trop long."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "409", description = CODE_PRIS)
    })
    public ResponseEntity<ActionnaireDto> creer(@RequestBody ActionnaireEcritureDto demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(demande));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Modifie un actionnaire (champs absents inchangés)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actionnaire modifié."),
            @ApiResponse(responseCode = "400", description = "Code ou description vide ; texte trop long."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = CODE_PRIS)
    })
    public ActionnaireDto modifier(@Parameter(description = ID) @PathVariable UUID id,
                                   @RequestBody ActionnaireEcritureDto demande) {
        return service.modifier(id, demande);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Supprime un actionnaire")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Actionnaire supprimé."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Cité par une répartition d'entité ou une ligne de groupe.")
    })
    public ResponseEntity<Void> supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
