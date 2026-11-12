package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.dto.achat.AffecterBonCommandeRequest;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.TransitionPieceAchatRequest;
import com.tsix.immocrm.services.achat.ecriture.EcrituresPieceAchatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Achat")
@RestController
@RequestMapping("/api/achat/documents/{id}")
public class ActionsPieceAchatController {

    private static final String ID_PIECE = "Id de la pièce d'achat (BuyDocument.PKey).";

    private final EcrituresPieceAchatService ecritures;

    public ActionsPieceAchatController(EcrituresPieceAchatService ecritures) {
        this.ecritures = ecritures;
    }

    @PatchMapping("/statut")
    @Operation(summary = "Change le statut d'une pièce d'achat par une transition de sa gestion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La fiche relue, au nouveau statut."),
            @ApiResponse(responseCode = "400", description = "statutId absent, ou note vide exigée par la transition.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Transition existante mais réservée à d'autres rôles.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Pièce introuvable, hors périmètre ou BC d'un autre déclencheur.",
                    content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)})
    public DocumentAchatDetailDto modifierStatut(@Parameter(description = ID_PIECE) @PathVariable UUID id,
                                                 @RequestBody TransitionPieceAchatRequest corps) {
        return ecritures.modifierStatut(id, corps);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime une pièce d'achat avec ses lignes, ventilations et historique")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pièce supprimée."),
            @ApiResponse(responseCode = "403", description = "Sans le droit OMRAN de suppression de cette nature.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Pièce introuvable, hors périmètre ou BC d'un autre déclencheur.",
                    content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)})
    public void supprimer(@Parameter(description = ID_PIECE) @PathVariable UUID id) {
        ecritures.supprimer(id);
    }

    @PutMapping("/bon-commande")
    @Operation(summary = "Rattache une facture à un bon de commande (Affecter à un B.C.)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La fiche de la facture relue."),
            @ApiResponse(responseCode = "400", description = "bonCommandeId absent.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sans le droit de modification des factures.",
                    content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)})
    public DocumentAchatDetailDto affecterBonCommande(@Parameter(description = ID_PIECE) @PathVariable UUID id,
                                                      @RequestBody AffecterBonCommandeRequest corps) {
        return ecritures.affecterBonCommande(id, corps);
    }
}
