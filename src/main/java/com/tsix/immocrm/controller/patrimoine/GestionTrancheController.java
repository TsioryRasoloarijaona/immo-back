package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.services.patrimoine.ChargementTrancheService;
import com.tsix.immocrm.services.patrimoine.SuppressionTrancheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.controller.patrimoine.OngletsTrancheController.ADMIN;
import static com.tsix.immocrm.controller.patrimoine.OngletsTrancheController.ID_TRANCHE;
import static com.tsix.immocrm.controller.patrimoine.OngletsTrancheController.REFUS_ROLE;

@Tag(name = "Tranches")
@RestController
@RequestMapping("/api/tranches")
public class GestionTrancheController {

    private final ChargementTrancheService chargementService;
    private final SuppressionTrancheService suppressionService;

    public GestionTrancheController(ChargementTrancheService chargementService,
                                    SuppressionTrancheService suppressionService) {
        this.chargementService = chargementService;
        this.suppressionService = suppressionService;
    }

    @PostMapping("/{id}/charger-depuis/{sourceId}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Charge les paramètres d'une autre tranche dans la tranche (« Charger à partir d'une autre tranche »)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche de la tranche, paramètres chargés."),
            @ApiResponse(responseCode = "400", description = "La source est la tranche elle-même (`champ` = `sourceId`)."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = "Tranche ou source inconnue, ou d'une entité inactive.")
    })
    public TrancheDetailDto chargerDepuis(@Parameter(description = ID_TRANCHE) @PathVariable UUID id,
                                          @Parameter(description = "Tranche dont on reprend les paramètres.")
                                          @PathVariable UUID sourceId) {
        return chargementService.modifierDepuisTranche(id, sourceId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime une tranche")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tranche supprimée, avec ses terrains, onglets, PJ et notes."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = "Tranche inconnue, ou de son entité inactive."),
            @ApiResponse(responseCode = "409", description = "Des lignes la référencent : `detail` les liste avec leur nombre.")
    })
    public ResponseEntity<Void> supprimer(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        suppressionService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
