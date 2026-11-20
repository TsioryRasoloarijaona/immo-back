package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.dto.achat.demandes.DemandeAchatFiche;
import com.tsix.immocrm.dto.achat.demandes.SaisieDemandeAchatLot;
import com.tsix.immocrm.services.achat.demandes.OngletsDemandeAchatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** « Ajouter une DIA » de la fiche lot (R18) ; hors de LotsProjetController, que les lots parallèles modifient. */
@Tag(name = "Projet")
@RestController
public class DemandesAchatLotController {

    private final OngletsDemandeAchatService service;

    public DemandesAchatLotController(OngletsDemandeAchatService service) {
        this.service = service;
    }

    @PostMapping("/api/projet/lots/{id}/demandes-achat")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée une demande interne d'achat depuis le lot, bordereau estimatif copié")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Demande créée."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lot inconnu ou hors périmètre.", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public DemandeAchatFiche creer(@Parameter(description = "Identifiant du lot (PrjMarket.PKey).") @PathVariable UUID id,
                                   @RequestBody(required = false) SaisieDemandeAchatLot saisie) {
        return service.creerDepuisLot(id, saisie);
    }
}
