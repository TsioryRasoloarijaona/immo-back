package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.reajustement.LigneReajustementDto;
import com.tsix.immocrm.dto.dossier.reajustement.ValiderReajustementRequest;
import com.tsix.immocrm.services.dossier.fiche.ReajustementSuperficiesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Dossiers — réajustement des superficies")
@RestController
@RequestMapping("/api/tranches/{id}/reajustement-superficies")
@PreAuthorize("hasRole('Administrateur')")
public class ReajustementSuperficiesController {

    private static final String DESCRIPTION_403 = "Réservé au rôle Administrateur.";

    private final ReajustementSuperficiesService reajustementService;

    public ReajustementSuperficiesController(ReajustementSuperficiesService reajustementService) {
        this.reajustementService = reajustementService;
    }

    @GetMapping
    @Operation(summary = "« Vérifier les données » : dossiers de la tranche dont la superficie diffère du stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossiers à réajuster (liste éventuellement vide)."),
            @ApiResponse(responseCode = "403", description = DESCRIPTION_403),
            @ApiResponse(responseCode = "404", description = "Tranche inexistante, hors périmètre ou d'une entité inactive.")
    })
    public List<LigneReajustementDto> verifier(
            @Parameter(description = "Identifiant de la tranche (CfgTranche.PKey).")
            @PathVariable UUID id) {
        return reajustementService.verifier(id);
    }

    @PostMapping
    @Operation(summary = "« Valider » : réajuste les dossiers cochés et leur schéma de financement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes appliquées, telles que renvoyées par la vérification."),
            @ApiResponse(responseCode = "400", description = "dossierIds absent ou vide."),
            @ApiResponse(responseCode = "403", description = DESCRIPTION_403),
            @ApiResponse(responseCode = "404", description = "Tranche introuvable, ou dossier coché hors de la tranche."),
            @ApiResponse(responseCode = "409")
    })
    public List<LigneReajustementDto> valider(
            @Parameter(description = "Identifiant de la tranche (CfgTranche.PKey).")
            @PathVariable UUID id,
            @RequestBody ValiderReajustementRequest request) {
        return reajustementService.validerReajustement(id, request != null ? request.dossierIds() : null);
    }
}
