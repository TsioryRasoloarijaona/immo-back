package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.dto.tresorerie.AffectationLigneDto;
import com.tsix.immocrm.dto.tresorerie.DecaissementFicheDto;
import com.tsix.immocrm.dto.tresorerie.DecaissementLigneDto;
import com.tsix.immocrm.dto.tresorerie.FiltresTresorerie;
import com.tsix.immocrm.dto.tresorerie.FiltresTresorerie.SensTresorerie;
import com.tsix.immocrm.dto.tresorerie.ListeTresorerieDto;
import com.tsix.immocrm.dto.tresorerie.RecettesDto;
import com.tsix.immocrm.dto.tresorerie.TotauxTresorerieDto.TotauxAffectationsDto;
import com.tsix.immocrm.dto.tresorerie.TotauxTresorerieDto.TotauxDecaissementsDto;
import com.tsix.immocrm.services.achat.TresorerieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Trésorerie")
@RestController
@RequestMapping("/api/tresorerie")
public class TresorerieController {

    private static final String REFUS = "Droit OMRAN de l'écran absent.";
    private static final String INVALIDE = "module absent ou hors PRJ/BUY, ou filtre mal formé.";

    private final TresorerieService service;

    public TresorerieController(TresorerieService service) {
        this.service = service;
    }

    @GetMapping("/decaissements")
    @Operation(summary = "Décaissements (règlements débit) visibles du module")
    @ApiResponse(responseCode = "200", description = "Page de décaissements et totaux de toute la liste, hors annulés.")
    @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content)
    @ApiResponse(responseCode = "403", description = REFUS, content = @Content)
    public ListeTresorerieDto<DecaissementLigneDto, TotauxDecaissementsDto> decaissements(
            @ParameterObject @ModelAttribute FiltresTresorerie filtres) {
        return service.decaissements(filtres);
    }

    @GetMapping("/decaissements/{id}")
    @Operation(summary = "Fiche en lecture d'un décaissement visible")
    @ApiResponse(responseCode = "200", description = "En-tête et pièces affectées visibles.")
    @ApiResponse(responseCode = "403", description = REFUS, content = @Content)
    @ApiResponse(responseCode = "404", description = "Décaissement inconnu ou invisible.", content = @Content)
    public DecaissementFicheDto decaissement(@Parameter(description = "StlSettlement.PKey.") @PathVariable UUID id) {
        return service.decaissement(id);
    }

    @GetMapping("/affectations")
    @Operation(summary = "Affectations de règlements (une ligne par StlSettlementLine visible)",
            operationId = "affectationsTresorerie")
    @ApiResponse(responseCode = "200", description = "Page d'affectations et totaux de toute la liste, hors annulés.")
    @ApiResponse(responseCode = "400", description = "sens absent ou invalide ; " + INVALIDE, content = @Content)
    @ApiResponse(responseCode = "403", description = REFUS, content = @Content)
    public ListeTresorerieDto<AffectationLigneDto, TotauxAffectationsDto> affectations(
            @Parameter(description = "DEBIT : Dépenses › Affectations ; CREDIT : Recettes › Affectations.")
            @RequestParam SensTresorerie sens,
            @ParameterObject @ModelAttribute FiltresTresorerie filtres) {
        return service.affectations(sens, filtres);
    }

    @GetMapping("/recettes/{vue}")
    @Operation(summary = "Listes Recettes : factures proforma, factures, avoirs ou encaissements du module")
    @ApiResponse(responseCode = "200", description = "Colonnes, page de lignes et total ; vide = lignes [] et total 0.")
    @ApiResponse(responseCode = "400", description = "vue inconnue, dateSur POINTAGE* sur une pièce ; " + INVALIDE,
            content = @Content)
    @ApiResponse(responseCode = "403", description = REFUS, content = @Content)
    public RecettesDto recettes(
            @Parameter(description = "proforma | factures | avoirs | encaissements.", example = "factures")
            @PathVariable String vue,
            @ParameterObject @ModelAttribute FiltresTresorerie filtres) {
        return service.recettes(vue, filtres);
    }
}
