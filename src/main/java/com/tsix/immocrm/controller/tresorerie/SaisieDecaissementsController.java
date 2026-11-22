package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.dto.tresorerie.DecaissementFicheDto;
import com.tsix.immocrm.dto.tresorerie.FiltresTresorerie.ModuleTresorerie;
import com.tsix.immocrm.dto.tresorerie.PieceAAffecterDto;
import com.tsix.immocrm.dto.tresorerie.ReferentielsDecaissementDto;
import com.tsix.immocrm.dto.tresorerie.SaisieDecaissementDto;
import com.tsix.immocrm.services.tresorerie.SaisieDecaissementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Trésorerie")
@RestController
@RequestMapping("/api/tresorerie/decaissements")
public class SaisieDecaissementsController {

    private static final String REFUS = "Droit OMRAN de l'écran ou de l'action absent.";
    private static final String INTROUVABLE = "Règlement, société, fournisseur, compte ou pièce inconnus ou invisibles.";
    private static final String INVALIDE = "Saisie refusée (règles R-a à R-k, invariants) ; message OMRAN dans detail.";
    private static final String ID = "StlSettlement.PKey.";

    private final SaisieDecaissementService service;

    public SaisieDecaissementsController(SaisieDecaissementService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un décaissement et ses affectations")
    @ApiResponse(responseCode = "201", description = "Fiche du décaissement créé (même forme que GET /decaissements/{id}).")
    @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content)
    @ApiResponse(responseCode = "403", description = REFUS, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Référence déjà utilisée (R-l) ou pointage avant le dernier rapprochement.",
            content = @Content)
    public DecaissementFicheDto creer(@RequestBody SaisieDecaissementDto saisie) {
        return service.creer(saisie);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un décaissement visible")
    @ApiResponse(responseCode = "200", description = "Fiche du décaissement modifié.")
    @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content)
    @ApiResponse(responseCode = "403", description = REFUS + " Ou société changée sans CanModifyCompany.", content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Unicité, rapprochement, ou règlement d'un virement interne.",
            content = @Content)
    public DecaissementFicheDto modifier(@Parameter(description = ID) @PathVariable UUID id,
                                         @RequestBody SaisieDecaissementDto saisie) {
        return service.modifier(id, saisie);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un décaissement visible")
    @ApiResponse(responseCode = "204", description = "Règlement et son historique supprimés.")
    @ApiResponse(responseCode = "403", description = REFUS, content = @Content)
    @ApiResponse(responseCode = "404", description = "Règlement inconnu ou invisible.", content = @Content)
    @ApiResponse(responseCode = "409", description = "Pointé, avec affectations, référencé ou virement interne.",
            content = @Content)
    public void supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        service.supprimer(id);
    }

    @GetMapping("/referentiels")
    @Operation(summary = "Listes de choix et droits de la fiche d'écriture")
    @ApiResponse(responseCode = "200", description = "Types, sociétés, comptes, devise, statuts et droits.")
    @ApiResponse(responseCode = "400", description = "module absent ou hors PRJ/BUY.", content = @Content)
    @ApiResponse(responseCode = "403", description = REFUS, content = @Content)
    public ReferentielsDecaissementDto referentiels(
            @Parameter(description = "PRJ ou BUY.", example = "PRJ") @RequestParam ModuleTresorerie module) {
        return service.referentiels(module);
    }

    @GetMapping("/pieces-a-affecter")
    @Operation(summary = "Pièces d'achat affectables à un décaissement")
    @ApiResponse(responseCode = "200", description = "Pièces non soldées du fournisseur ; vide = []. Tri par code.")
    @ApiResponse(responseCode = "400", description = "module, fournisseurId ou societeId absent ou mal formé.",
            content = @Content)
    @ApiResponse(responseCode = "403", description = REFUS, content = @Content)
    public List<PieceAAffecterDto> piecesAAffecter(
            @Parameter(description = "PRJ ou BUY.", example = "PRJ") @RequestParam ModuleTresorerie module,
            @Parameter(description = "Fournisseur du règlement (CfgTier.PKey).") @RequestParam UUID fournisseurId,
            @Parameter(description = "Société du règlement (CfgCompany.PKey).") @RequestParam UUID societeId,
            @Parameter(description = "Règlement modifié : ses propres lignes ne comptent pas dans le réglé.")
            @RequestParam(required = false) UUID reglementId) {
        return service.piecesAAffecter(module, fournisseurId, societeId, reglementId);
    }
}
