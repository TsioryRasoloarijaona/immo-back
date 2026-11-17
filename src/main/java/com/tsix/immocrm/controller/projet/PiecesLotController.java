package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.domain.projet.NaturePieceLot;
import com.tsix.immocrm.dto.projet.PieceLotDetailDto;
import com.tsix.immocrm.dto.projet.PieceLotModeleDto;
import com.tsix.immocrm.dto.projet.PieceLotSaisieDto;
import com.tsix.immocrm.services.projet.EcriturePieceLot;
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

import java.util.UUID;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/lots/{id}")
public class PiecesLotController {

    private static final String PIECES = "/pieces-a-payer";
    private static final String UNE_PIECE = PIECES + "/{pieceId}";
    private static final String ID_PIECE = "Identifiant de la pièce (BuyDocument.PKey).";
    private static final String INTROUVABLE = "Lot invisible ou inconnu (LOT_INTROUVABLE), ou pièce inconnue / d'un autre lot / d'une autre nature.";
    private static final String DROIT = "Droit MarketMonitoringList.toolAdvance absent (sauf remboursement de prorata).";
    private static final String CORPS_INVALIDE = "Date absente, montant ou pénalité négatif, échéance avant la date, nature absente ou inconnue.";
    private static final String REFUS_CREATION = "Lot mono-fournisseur sans fournisseur, ou statut du lot / blocage de la RG (message OMRAN).";
    private static final String RELUE = "Pièce relue après écriture.";

    private final EcriturePieceLot pieces;

    public PiecesLotController(EcriturePieceLot pieces) {
        this.pieces = pieces;
    }

    @GetMapping(PIECES + "/modele")
    @Operation(summary = "Pré-remplissage d'une pièce à payer du lot")
    @ApiResponse(responseCode = "200", description = "Date, échéance, référence, montant par défaut, notes et fournisseur.")
    @ApiResponse(responseCode = "403", description = DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = REFUS_CREATION, content = @Content)
    public PieceLotModeleDto modele(@Parameter(description = AvenantsLotController.ID_LOT) @PathVariable UUID id,
                                    @Parameter(description = "Nature de la pièce à créer.") @RequestParam NaturePieceLot nature) {
        return pieces.modele(id, nature);
    }

    @PostMapping(PIECES)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une pièce à payer du lot")
    @ApiResponse(responseCode = "201", description = RELUE)
    @ApiResponse(responseCode = "400", description = CORPS_INVALIDE, content = @Content)
    @ApiResponse(responseCode = "403", description = DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = REFUS_CREATION, content = @Content)
    public PieceLotDetailDto creer(@Parameter(description = AvenantsLotController.ID_LOT) @PathVariable UUID id,
                                   @RequestBody PieceLotSaisieDto corps) {
        return pieces.creer(id, corps);
    }

    @GetMapping(UNE_PIECE)
    @Operation(summary = "Fiche d'une pièce à payer du lot")
    @ApiResponse(responseCode = "200", description = "Pièce, montants et actions permises.")
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public PieceLotDetailDto lire(@Parameter(description = AvenantsLotController.ID_LOT) @PathVariable UUID id,
                                  @Parameter(description = ID_PIECE) @PathVariable UUID pieceId) {
        return pieces.lire(id, pieceId);
    }

    @PutMapping(UNE_PIECE)
    @Operation(summary = "Modifier une pièce à payer du lot")
    @ApiResponse(responseCode = "200", description = RELUE)
    @ApiResponse(responseCode = "400", description = CORPS_INVALIDE, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Vous ne pouvez pas modifier cette pièce dans l'état actuel du lot !",
            content = @Content)
    public PieceLotDetailDto modifier(@Parameter(description = AvenantsLotController.ID_LOT) @PathVariable UUID id,
                                      @Parameter(description = ID_PIECE) @PathVariable UUID pieceId,
                                      @RequestBody PieceLotSaisieDto corps) {
        return pieces.modifier(id, pieceId, corps);
    }

    @DeleteMapping(UNE_PIECE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une pièce à payer du lot")
    @ApiResponse(responseCode = "204", description = "Pièce, lignes, ventilations et historique supprimés.")
    @ApiResponse(responseCode = "403", description = DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Statut de la pièce en lecture seule, ou règlements liés.", content = @Content)
    public void supprimer(@Parameter(description = AvenantsLotController.ID_LOT) @PathVariable UUID id,
                          @Parameter(description = ID_PIECE) @PathVariable UUID pieceId) {
        pieces.supprimer(id, pieceId);
    }

    @DeleteMapping("/avoirs/{avoirId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un avoir du lot")
    @ApiResponse(responseCode = "204", description = "Avoir, lignes, ventilations et historique supprimés.")
    @ApiResponse(responseCode = "403", description = "Droit RefundList.toolDelete absent.", content = @Content)
    @ApiResponse(responseCode = "404", description = "Lot invisible ou inconnu, ou pièce qui n'est pas un avoir de ce lot.",
            content = @Content)
    @ApiResponse(responseCode = "409", description = "Statut de l'avoir en lecture seule, ou règlements liés.", content = @Content)
    public void supprimerAvoir(@Parameter(description = AvenantsLotController.ID_LOT) @PathVariable UUID id,
                               @Parameter(description = "Identifiant de l'avoir (BuyDocument.PKey).") @PathVariable UUID avoirId) {
        pieces.supprimerAvoir(id, avoirId);
    }
}
