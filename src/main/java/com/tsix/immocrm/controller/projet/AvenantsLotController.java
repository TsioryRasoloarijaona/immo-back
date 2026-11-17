package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.AvenantDetailDto;
import com.tsix.immocrm.dto.projet.AvenantSaisieDto;
import com.tsix.immocrm.services.projet.EcritureAvenantLot;
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

import java.util.UUID;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/lots/{id}/avenants")
public class AvenantsLotController {

    static final String ID_LOT = "Identifiant du lot (PrjMarket.PKey).";
    static final String INTROUVABLE = "Lot invisible ou inconnu (LOT_INTROUVABLE), ou fiche inconnue / d'un autre lot.";
    private static final String ID_AVENANT = "Identifiant de l'avenant (PrjClause.PKey).";
    private static final String CORPS_INVALIDE = "Champ obligatoire absent, hors bornes ou trop long ; id de ligne d'un autre avenant.";
    private static final String FACTURE = "Avenant facturé : lignes et retenues non modifiables.";
    private static final String RELU = "Avenant relu après écriture.";

    private final EcritureAvenantLot avenants;

    public AvenantsLotController(EcritureAvenantLot avenants) {
        this.avenants = avenants;
    }

    @GetMapping("/{avenantId}")
    @Operation(summary = "Fiche d'un avenant du lot")
    @ApiResponse(responseCode = "200", description = "Avenant, lignes, montants et actions permises.")
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public AvenantDetailDto avenant(@Parameter(description = ID_LOT) @PathVariable UUID id,
                                    @Parameter(description = ID_AVENANT) @PathVariable UUID avenantId) {
        return avenants.lire(id, avenantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un avenant du lot")
    @ApiResponse(responseCode = "201", description = RELU)
    @ApiResponse(responseCode = "400", description = CORPS_INVALIDE, content = @Content)
    @ApiResponse(responseCode = "403", description = "Droit ClauseList.toolNew absent, ou retenue par montant sans le droit.",
            content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public AvenantDetailDto creer(@Parameter(description = ID_LOT) @PathVariable UUID id, @RequestBody AvenantSaisieDto corps) {
        return avenants.creer(id, corps);
    }

    @PutMapping("/{avenantId}")
    @Operation(summary = "Modifier un avenant du lot (lignes envoyées entières)")
    @ApiResponse(responseCode = "200", description = RELU)
    @ApiResponse(responseCode = "400", description = CORPS_INVALIDE, content = @Content)
    @ApiResponse(responseCode = "403", description = "Retenue passée ou changée par montant sans le droit.", content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = FACTURE, content = @Content)
    public AvenantDetailDto modifier(@Parameter(description = ID_LOT) @PathVariable UUID id,
                                     @Parameter(description = ID_AVENANT) @PathVariable UUID avenantId,
                                     @RequestBody AvenantSaisieDto corps) {
        return avenants.modifier(id, avenantId, corps);
    }

    @DeleteMapping("/{avenantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un avenant du lot et ses lignes")
    @ApiResponse(responseCode = "204", description = "Avenant supprimé.")
    @ApiResponse(responseCode = "403", description = "Droit ClauseList.toolDelete absent.", content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Vous ne pouvez pas supprimer un avenant avec une facture associée.",
            content = @Content)
    public void supprimer(@Parameter(description = ID_LOT) @PathVariable UUID id,
                          @Parameter(description = ID_AVENANT) @PathVariable UUID avenantId) {
        avenants.supprimer(id, avenantId);
    }
}
