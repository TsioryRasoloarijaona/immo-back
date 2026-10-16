package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.modalite.CreerModalitePaiementRequest;
import com.tsix.immocrm.dto.dossier.modalite.ModalitePaiementDto;
import com.tsix.immocrm.services.dossier.modalite.ModalitePaiementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Dossiers — modalité de paiement")
@RestController
@RequestMapping("/api/dossiers/{id}/modalite-paiement")
public class DossierModalitePaiementController {

    private static final String DOSSIER = "Identifiant du dossier (ComFolder.PKey).";
    private static final String DESCRIPTION_404 = "Dossier inexistant ou hors périmètre.";
    private static final String DESCRIPTION_200 = "Lignes à jour, date décroissante.";

    private final ModalitePaiementService service;

    public DossierModalitePaiementController(ModalitePaiementService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Modalité de paiement : schéma de financement figé à la vente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes, date décroissante (peut être vide)."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public List<ModalitePaiementDto> lister(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.lister(id);
    }

    @PostMapping
    @Operation(summary = "Ajoute une ligne de modalité de paiement")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = DESCRIPTION_200),
            @ApiResponse(responseCode = "400", description = "Date, catégorie ou montant absent, ou catégorie introuvable."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public ResponseEntity<List<ModalitePaiementDto>> ajouter(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                                             @RequestBody CreerModalitePaiementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouter(id, request));
    }

    @DeleteMapping("/{ligneId}")
    @Operation(summary = "Supprime une ligne de modalité de paiement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = DESCRIPTION_200),
            @ApiResponse(responseCode = "404", description = "Dossier ou ligne introuvable (ou d'un autre dossier).")
    })
    public List<ModalitePaiementDto> supprimer(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                               @Parameter(description = "Ligne (ComDocumentPaymentMethod.PKey).")
                                               @PathVariable UUID ligneId) {
        return service.supprimer(id, ligneId);
    }

    @PostMapping("/copier-schema")
    @Operation(summary = "Copier schéma de financement : remplace les lignes par l'échéancier actuel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = DESCRIPTION_200),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public List<ModalitePaiementDto> copierSchema(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.copierSchema(id);
    }
}
