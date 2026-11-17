package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.EcheanceRgSaisieDto;
import com.tsix.immocrm.dto.projet.ReceptionDetailDto;
import com.tsix.immocrm.dto.projet.ReceptionSaisieDto;
import com.tsix.immocrm.dto.projet.RemboursementsLotDto;
import com.tsix.immocrm.services.projet.EcritureEcheancesRgLot;
import com.tsix.immocrm.services.projet.EcritureReceptionLot;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AvenantsLotController.ID_LOT;
import static com.tsix.immocrm.controller.projet.AvenantsLotController.INTROUVABLE;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/lots/{id}")
public class ReceptionsLotController {

    private static final String ID_RECEPTION = "Identifiant de la réception (PrjMarketReception.PKey).";
    private static final String CORPS_INVALIDE = "Champ obligatoire absent, inconnu ou trop long ; tiers en double ; id d'une autre réception.";
    private static final String RELUE = "Réception relue après écriture.";

    private final EcritureReceptionLot receptions;
    private final EcritureEcheancesRgLot echeances;

    public ReceptionsLotController(EcritureReceptionLot receptions, EcritureEcheancesRgLot echeances) {
        this.receptions = receptions;
        this.echeances = echeances;
    }

    @PutMapping("/echeances-rg")
    @Operation(summary = "Enregistrer la grille entière des échéances de remboursement de RG")
    @ApiResponse(responseCode = "200", description = "Remboursements du lot relus.")
    @ApiResponse(responseCode = "400", description = "Ordre ou pourcentage absent, hors bornes ; id en double ou d'un autre lot.",
            content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public RemboursementsLotDto enregistrerEcheances(@Parameter(description = ID_LOT) @PathVariable UUID id,
                                                     @RequestBody List<EcheanceRgSaisieDto> corps) {
        return echeances.enregistrerEcheances(id, corps);
    }

    @GetMapping("/receptions/{receptionId}")
    @Operation(summary = "Fiche d'une réception du lot")
    @ApiResponse(responseCode = "200", description = "Réception, réserves et tiers invités.")
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public ReceptionDetailDto reception(@Parameter(description = ID_LOT) @PathVariable UUID id,
                                        @Parameter(description = ID_RECEPTION) @PathVariable UUID receptionId) {
        return receptions.lire(id, receptionId);
    }

    @PostMapping("/receptions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une réception du lot")
    @ApiResponse(responseCode = "201", description = RELUE)
    @ApiResponse(responseCode = "400", description = CORPS_INVALIDE, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public ReceptionDetailDto creerReception(@Parameter(description = ID_LOT) @PathVariable UUID id,
                                             @RequestBody ReceptionSaisieDto corps) {
        return receptions.creer(id, corps);
    }

    @PutMapping("/receptions/{receptionId}")
    @Operation(summary = "Modifier une réception du lot (réserves et tiers envoyés entiers)")
    @ApiResponse(responseCode = "200", description = RELUE)
    @ApiResponse(responseCode = "400", description = CORPS_INVALIDE, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public ReceptionDetailDto modifierReception(@Parameter(description = ID_LOT) @PathVariable UUID id,
                                                @Parameter(description = ID_RECEPTION) @PathVariable UUID receptionId,
                                                @RequestBody ReceptionSaisieDto corps) {
        return receptions.modifier(id, receptionId, corps);
    }

    @DeleteMapping("/receptions/{receptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une réception du lot, ses réserves et ses tiers invités")
    @ApiResponse(responseCode = "204", description = "Réception supprimée.")
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public void supprimerReception(@Parameter(description = ID_LOT) @PathVariable UUID id,
                                   @Parameter(description = ID_RECEPTION) @PathVariable UUID receptionId) {
        receptions.supprimer(id, receptionId);
    }
}
