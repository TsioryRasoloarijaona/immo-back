package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.PrevisionsLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsSaisieLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsSaisieRequest;
import com.tsix.immocrm.services.projet.PrevisionsLotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/lots/{id}/previsions")
public class PrevisionsLotController {

    private static final String ID_LOT = "Identifiant du lot (PrjMarket.PKey).";
    private static final String LOT_INTROUVABLE = "Lot introuvable : id inconnu, lot sans projet, projet hors périmètre ou statut invisible.";
    private static final String SANS_DROIT = "Droit ProjectPrevision.toolNew absent.";

    private final PrevisionsLotService service;

    public PrevisionsLotController(PrevisionsLotService service) {
        this.service = service;
    }

    @GetMapping("/saisie")
    @Operation(summary = "Fenêtre de saisie des prévisions d'un lot")
    @ApiResponse(responseCode = "200", description = "Dates, estimation de base, montants de référence et points stockés.")
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public PrevisionsSaisieLotDto saisiePrevisions(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return service.saisie(id);
    }

    @PutMapping
    @Operation(summary = "Enregistrer les prévisions d'un lot")
    @ApiResponse(responseCode = "200", description = "Graphe des prévisions relu après écriture et recalcul.")
    @ApiResponse(responseCode = "400", description = "Date ou durée absente, durée négative, points incohérents.", content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT + " Ou estimation de base modifiée sans droit.", content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public PrevisionsLotDto enregistrerPrevisions(@Parameter(description = ID_LOT) @PathVariable UUID id,
                                        @RequestBody PrevisionsSaisieRequest corps) {
        return service.enregistrer(id, corps);
    }
}
