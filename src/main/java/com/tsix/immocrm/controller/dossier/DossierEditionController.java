package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.edition.DossierModifieDto;
import com.tsix.immocrm.dto.dossier.edition.DroitsEditionDto;
import com.tsix.immocrm.dto.dossier.edition.ModifierDossierRequest;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.dossier.edition.DossierEditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Dossiers — modification de la fiche")
@RestController
@RequestMapping("/api/dossiers")
public class DossierEditionController {

    private static final String DESCRIPTION_ID = "Identifiant du dossier (ComFolder.PKey).";
    private static final String DESCRIPTION_404 = "Dossier inexistant, hors périmètre ou d'une entité inactive.";

    private final DossierEditionService editionService;

    public DossierEditionController(DossierEditionService editionService) {
        this.editionService = editionService;
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Modifie la fiche du dossier (corps partiel) et renvoie la fiche relue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modification enregistrée ; dossier signé et historisé."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403",
                    description = "Un champ envoyé appartient à un groupe interdit au rôle, ou est dans champsVerrouilles."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404),
            @ApiResponse(responseCode = "409",
                    description = "Dossier sorti hors onglet Annulation, ou délai / crédit maximal de la tranche dépassé.")
    })
    public DossierModifieDto modifier(
            @Parameter(description = DESCRIPTION_ID) @PathVariable UUID id,
            @RequestBody ModifierDossierRequest request) {
        return editionService.modifier(id, request);
    }

    @GetMapping("/{id}/edition")
    @Operation(summary = "Groupes de champs de la fiche que l'appelant peut modifier sur ce dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groupes modifiables ; seul ANNULATION si le dossier est sorti."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public DroitsEditionDto droits(@Parameter(description = DESCRIPTION_ID) @PathVariable UUID id) {
        return editionService.droits(id);
    }

    @GetMapping("/decisions-meilleurtaux")
    @Operation(summary = "Décisions MEILLEURTAUX possibles (ComLoanCompanyDecision), pour l'onglet Autres")
    public List<ReferentielItemDto> decisionsMeilleurTaux() {
        return editionService.decisionsMeilleurTaux();
    }
}
