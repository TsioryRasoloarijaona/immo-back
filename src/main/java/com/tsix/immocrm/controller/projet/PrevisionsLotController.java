package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.AlignementPrevisionsDto;
import com.tsix.immocrm.dto.projet.AlignementPrevisionsRequest;
import com.tsix.immocrm.dto.projet.ModeleSimulationSaisieDto;
import com.tsix.immocrm.dto.projet.PrevisionsLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsSaisieLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsSaisieRequest;
import com.tsix.immocrm.dto.projet.PropositionPointsDto;
import com.tsix.immocrm.services.projet.AlignementPrevisionsService;
import com.tsix.immocrm.services.projet.PrevisionsLotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private static final String SANS_DEBUT = "Lot sans date début.";

    private final PrevisionsLotService service;
    private final AlignementPrevisionsService alignement;

    public PrevisionsLotController(PrevisionsLotService service, AlignementPrevisionsService alignement) {
        this.service = service;
        this.alignement = alignement;
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

    @GetMapping("/alignement")
    @Operation(summary = "Écarts au réel de la modale « Aligner les prévisions au réel »")
    @ApiResponse(responseCode = "200", description = "Écarts de montant, d'avancement et de temps, date fin proposée.")
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = SANS_DEBUT, content = @Content)
    public AlignementPrevisionsDto ecartsAlignement(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return alignement.ecarts(id);
    }

    @PostMapping("/alignement")
    @Operation(summary = "Propose les points « Prévisions » alignés sur le réel (n'écrit rien)")
    @ApiResponse(responseCode = "200", description = "Points proposés ; fin et durée si la date fin change.")
    @ApiResponse(responseCode = "400", description = "Mode inconnu, ou FIXEE sans dateFin.", content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Lot multi-fournisseur, sans date début, ou date fixée avant le dernier réel.",
            content = @Content)
    public PropositionPointsDto proposerAlignement(@Parameter(description = ID_LOT) @PathVariable UUID id,
                                                   @RequestBody AlignementPrevisionsRequest corps) {
        return alignement.aligner(id, corps);
    }

    @PostMapping("/appliquer-modele/{modeleId}")
    @Operation(summary = "Propose les points « Prévisions » d'un modèle de simulation (n'écrit rien)")
    @ApiResponse(responseCode = "200", description = "Un point par ligne du modèle : début + durée × temps, avancement du modèle.")
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE + " Ou modèle inconnu.", content = @Content)
    @ApiResponse(responseCode = "409", description = SANS_DEBUT, content = @Content)
    public PropositionPointsDto proposerApplicationModele(@Parameter(description = ID_LOT) @PathVariable UUID id,
            @Parameter(description = "PrjMarketPrevisionModel.PKey.") @PathVariable UUID modeleId) {
        return alignement.appliquerModele(id, modeleId);
    }

    @GetMapping("/modele-propose")
    @Operation(summary = "Modèle de simulation pré-rempli depuis les points de base du lot (n'écrit rien)")
    @ApiResponse(responseCode = "200", description = "Lignes = points de base triés par date ; code et description vides.")
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public ModeleSimulationSaisieDto modelePropose(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return alignement.modelePropose(id);
    }
}
