package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.EcheanceDto;
import com.tsix.immocrm.dto.dossier.echeancier.EtatSchemaDto;
import com.tsix.immocrm.dto.dossier.echeancier.GenererPeriodiqueRequest;
import com.tsix.immocrm.services.dossier.EcheancierSchemaService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Dossiers — schéma de financement")
@RestController
@RequestMapping("/api/dossiers/{id}/echeancier")
public class DossierEcheancierController {

    private static final String DOSSIER = "Identifiant du dossier (ComFolder.PKey).";
    private static final String DESCRIPTION_404 = "Dossier inexistant ou hors périmètre.";

    private final EcheancierSchemaService schemaService;

    public DossierEcheancierController(EcheancierSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @GetMapping("/schema")
    @Operation(summary = "État du schéma : avance, crédit, autofinancement, écart au prix et montant de périodicité")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "État calculé."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public EtatSchemaDto etat(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return schemaService.etat(id);
    }

    @DeleteMapping("/{echeanceId}")
    @PreAuthorize("hasAnyRole('Administrateur', 'Commercial Chef', 'Direction Générale', "
            + "'Responsable marketing et communication')")
    @Operation(summary = "Supprime une ligne d'échéancier (droit OMRAN ComFolderDeleteLine)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ligne supprimée ; état du schéma après report, écart compris."),
            @ApiResponse(responseCode = "403", description = "Réservé à Administrateur, Commercial Chef, Direction Générale, "
                    + "Responsable marketing et communication."),
            @ApiResponse(responseCode = "404", description = "Dossier ou échéance introuvable (ou d'un autre dossier)."),
            @ApiResponse(responseCode = "409", description = "Échéance réglée, facturée, en décompte ou en recouvrement ; "
                    + "ou suppression qui creuse l'écart du schéma.")
    })
    public EtatSchemaDto supprimer(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                   @Parameter(description = "Identifiant de l'échéance (ComDocument.PKey).")
                                   @PathVariable UUID echeanceId) {
        return schemaService.supprimer(id, echeanceId);
    }

    @PostMapping("/periodique")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Génère des échéances périodiques")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lignes créées (sans celles ramenées à zéro)."),
            @ApiResponse(responseCode = "400", description = "Champ manquant, montant au-delà du montant initial, "
                    + "ou date avant le début des finalisations."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404),
            @ApiResponse(responseCode = "409", description = DossierController.SCHEMA_CREUSE)
    })
    public List<EcheanceDto> genererPeriodique(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                               @RequestBody GenererPeriodiqueRequest request) {
        return schemaService.genererPeriodique(id, request);
    }
}
