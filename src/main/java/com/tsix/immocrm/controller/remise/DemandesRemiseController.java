package com.tsix.immocrm.controller.remise;

import com.tsix.immocrm.domain.StatutDemandeRemise;
import com.tsix.immocrm.dto.remise.AccepterRemiseRequest;
import com.tsix.immocrm.dto.remise.DemandeRemiseDto;
import com.tsix.immocrm.dto.remise.DemanderRemiseRequest;
import com.tsix.immocrm.dto.remise.RefuserRemiseRequest;
import com.tsix.immocrm.services.remise.DemandesRemiseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Demandes de remise")
@RestController
@RequestMapping("/api")
public class DemandesRemiseController {

    private static final String DOSSIER = "Id du dossier de vente.";
    private static final String DEMANDE = "Id de la demande de remise.";
    private static final String DOSSIER_INTROUVABLE = "Dossier inconnu ou hors du périmètre de l'appelant.";
    private static final String DEMANDE_INTROUVABLE = "Demande inconnue ou hors du périmètre de l'appelant.";
    private static final String PAS_AGENCE = "Le compte ne porte pas le rôle Agence.";
    private static final String PAS_VALIDEUR = "Le compte n'est pas valideur de remise (profil validationRemise).";
    private static final String PAS_EN_ATTENTE = "La demande n'est plus en attente.";

    private final DemandesRemiseService service;

    public DemandesRemiseController(DemandesRemiseService service) {
        this.service = service;
    }

    @PostMapping("/dossiers/{id}/demandes-remise")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Demande une remise sur un dossier (agence)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "La demande créée, statut EN_ATTENTE."),
            @ApiResponse(responseCode = "400", description = "Taux absent, ≤ 0, > 100 ou ≤ taux actuel ; motif vide ou trop long.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_AGENCE, content = @Content),
            @ApiResponse(responseCode = "404", description = DOSSIER_INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = "Dossier sorti, ou une demande déjà en attente sur ce dossier.",
                    content = @Content)
    })
    public DemandeRemiseDto demander(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                     @RequestBody DemanderRemiseRequest requete) {
        return service.demander(id, requete);
    }

    @GetMapping("/dossiers/{id}/demandes-remise")
    @Operation(summary = "Historique des demandes de remise d'un dossier, la plus récente d'abord")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Les demandes du dossier, tous statuts (liste vide si aucune)."),
            @ApiResponse(responseCode = "404", description = DOSSIER_INTROUVABLE, content = @Content)
    })
    public List<DemandeRemiseDto> historique(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.historique(id);
    }

    @GetMapping("/demandes-remise")
    @Operation(summary = "File des demandes de remise à décider (siège valideur)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Les demandes du statut demandé, la plus ancienne d'abord."),
            @ApiResponse(responseCode = "400", description = "Statut inconnu.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_VALIDEUR, content = @Content)
    })
    public List<DemandeRemiseDto> file(
            @Parameter(description = "Statut filtré ; absent = EN_ATTENTE.", example = "EN_ATTENTE")
            @RequestParam(defaultValue = "EN_ATTENTE") StatutDemandeRemise statut) {
        return service.file(statut);
    }

    @PostMapping("/demandes-remise/{id}/acceptation")
    @Operation(summary = "Accepte une demande et applique la remise au dossier (siège valideur)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La demande ACCEPTEE ; le prix du dossier est recalculé."),
            @ApiResponse(responseCode = "400", description = "Taux accordé ≤ 0, > taux demandé ou ≤ remise actuelle du dossier.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_VALIDEUR, content = @Content),
            @ApiResponse(responseCode = "404", description = DEMANDE_INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = "Demande plus en attente, ou dossier sorti.", content = @Content)
    })
    public DemandeRemiseDto accepter(@Parameter(description = DEMANDE) @PathVariable UUID id,
                                     @RequestBody(required = false) AccepterRemiseRequest requete) {
        return service.accepter(id, requete);
    }

    @PostMapping("/demandes-remise/{id}/refus")
    @Operation(summary = "Refuse une demande avec un motif (siège valideur)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La demande REFUSEE ; le dossier n'est pas modifié."),
            @ApiResponse(responseCode = "400", description = "Motif absent, vide ou trop long.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_VALIDEUR, content = @Content),
            @ApiResponse(responseCode = "404", description = DEMANDE_INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = PAS_EN_ATTENTE, content = @Content)
    })
    public DemandeRemiseDto refuser(@Parameter(description = DEMANDE) @PathVariable UUID id,
                                    @RequestBody RefuserRemiseRequest requete) {
        return service.refuser(id, requete);
    }

    @PostMapping("/demandes-remise/{id}/annulation")
    @Operation(summary = "Annule une demande en attente (agence du dossier)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La demande ANNULEE."),
            @ApiResponse(responseCode = "403", description = PAS_AGENCE, content = @Content),
            @ApiResponse(responseCode = "404", description = DEMANDE_INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = PAS_EN_ATTENTE, content = @Content)
    })
    public DemandeRemiseDto annuler(@Parameter(description = DEMANDE) @PathVariable UUID id) {
        return service.annuler(id);
    }
}
