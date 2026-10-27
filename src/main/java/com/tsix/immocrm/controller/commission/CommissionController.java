package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.dto.commission.CommissionTrancheDto;
import com.tsix.immocrm.dto.commission.DemandesCommission.DroitsCommissionDto;
import com.tsix.immocrm.dto.commission.DemandesCommission.FiltresCommission;
import com.tsix.immocrm.dto.commission.DemandesCommission.GenererCommissionsRequest;
import com.tsix.immocrm.dto.commission.DemandesCommission.LignesCommissionRequest;
import com.tsix.immocrm.dto.commission.DemandesCommission.ModifierMontantCommissionRequest;
import com.tsix.immocrm.dto.commission.DocumentCommissionDto;
import com.tsix.immocrm.dto.commission.DossierCommissionDto;
import com.tsix.immocrm.dto.commission.LigneCommissionCalculeeDto;
import com.tsix.immocrm.dto.commission.ResultatGenerationDto;
import com.tsix.immocrm.services.commission.CommissionService;
import com.tsix.immocrm.services.commission.DocumentCommission;
import com.tsix.immocrm.services.commission.DroitsCommission;
import com.tsix.immocrm.services.commission.DroitsCommission.Droit;
import com.tsix.immocrm.services.commission.GenerationCommissions;
import com.tsix.immocrm.services.commission.LignesCommission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

@Tag(name = "Commissions")
@RestController
@RequestMapping("/api/commissions")
public class CommissionController {

    private static final String TRANCHE = "Id de la tranche (CfgTranche).";
    private static final String LIGNE = "Id de la ligne de commission (ComCommissionLine).";
    private static final String DROIT = "Droit AdmRight absent pour l'action.";
    private static final String INTROUVABLE = "Tranche, dossier ou ligne inconnu ou hors périmètre.";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DroitsCommission droits;
    private final CommissionService service;
    private final GenerationCommissions generation;
    private final LignesCommission lignes;
    private final DocumentCommission documents;

    public CommissionController(DroitsCommission droits, CommissionService service, GenerationCommissions generation,
                                LignesCommission lignes, DocumentCommission documents) {
        this.droits = droits;
        this.service = service;
        this.generation = generation;
        this.lignes = lignes;
        this.documents = documents;
    }

    @GetMapping("/droits")
    @Operation(summary = "Droits de l'appelant sur l'écran : écriture, suppression, impression")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trois booléens."),
            @ApiResponse(responseCode = "403", description = "Rôle sans l'écran Commissions.", content = @Content)
    })
    public DroitsCommissionDto droits() {
        return droits.lire();
    }

    @GetMapping("/tranches")
    @Operation(summary = "Tranches visibles avec leurs soldes de commission")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tranches triées par code."),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content)
    })
    public List<CommissionTrancheDto> tranches(
            @Parameter(description = "Recherche multi-critère sur le code et le libellé, espaces ignorés.")
            @RequestParam(required = false) String recherche) {
        droits.exiger(Droit.LECTURE);
        return service.tranches(recherche);
    }

    @GetMapping("/tranches/{trancheId}")
    @Operation(summary = "En-tête d'une tranche : soldes de commission et ventes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La tranche."),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public CommissionTrancheDto tranche(@Parameter(description = TRANCHE) @PathVariable UUID trancheId) {
        droits.exiger(Droit.LECTURE);
        return service.tranche(trancheId);
    }

    @GetMapping("/tranches/{trancheId}/dossiers")
    @Operation(summary = "Dossiers de vente de la tranche et leurs commissions par type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossiers triés par N°."),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public List<DossierCommissionDto> dossiersDeLaTranche(@Parameter(description = TRANCHE) @PathVariable UUID trancheId,
                                               @ParameterObject @ModelAttribute FiltresCommission filtres) {
        droits.exiger(Droit.LECTURE);
        return service.dossiers(trancheId, filtres);
    }

    @GetMapping("/tranches/{trancheId}/lignes")
    @Operation(summary = "Lignes de commission de la tranche")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes triées par date puis dossier."),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public List<LigneCommissionCalculeeDto> lignes(@Parameter(description = TRANCHE) @PathVariable UUID trancheId,
                                                   @ParameterObject @ModelAttribute FiltresCommission filtres) {
        droits.exiger(Droit.LECTURE);
        return service.lignes(trancheId, filtres);
    }

    @GetMapping("/tranches/{trancheId}/factures")
    @Operation(summary = "Documents à payer liés aux lignes de la tranche")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents triés par code."),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public List<DocumentCommissionDto> factures(@Parameter(description = TRANCHE) @PathVariable UUID trancheId) {
        droits.exiger(Droit.LECTURE);
        return documents.deLaTranche(trancheId);
    }

    @PostMapping("/tranches/{trancheId}/generation")
    @Operation(summary = "Génère un type de commission pour des dossiers de la tranche")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes créées et dossiers ignorés."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public ResultatGenerationDto genererLignes(@Parameter(description = TRANCHE) @PathVariable UUID trancheId,
                                         @Valid @RequestBody GenererCommissionsRequest demande) {
        droits.exiger(Droit.ECRITURE);
        return generation.generer(trancheId, demande.type(), demande.dossierIds());
    }

    @PutMapping("/lignes/{ligneId}/montant")
    @Operation(summary = "Modifie le montant d'une ligne sans document à payer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La ligne relue."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public LigneCommissionCalculeeDto modifierMontant(@Parameter(description = LIGNE) @PathVariable UUID ligneId,
                                                      @Valid @RequestBody ModifierMontantCommissionRequest demande) {
        droits.exiger(Droit.ECRITURE);
        return lignes.modifierMontant(ligneId, demande.montant());
    }

    @PostMapping("/lignes/suppression")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime des lignes de commission, tout ou rien")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lignes supprimées."),
            @ApiResponse(responseCode = "400", description = "Plus de 500 lignes.", content = @Content),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public void supprimerLignes(@Valid @RequestBody LignesCommissionRequest demande) {
        droits.exiger(Droit.SUPPRESSION);
        lignes.supprimer(demande.ligneIds());
    }

    @PostMapping("/documents-a-payer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Génère le document à payer d'un commercial pour les lignes choisies")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Le document créé."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public DocumentCommissionDto genererDocument(@Valid @RequestBody LignesCommissionRequest demande) {
        droits.exiger(Droit.ECRITURE);
        return documents.generer(demande.ligneIds());
    }

    @GetMapping("/lignes/{ligneId}/document-a-payer")
    @Operation(summary = "Document à payer d'une ligne")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le document."),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public DocumentCommissionDto documentDeLigne(@Parameter(description = LIGNE) @PathVariable UUID ligneId) {
        droits.exiger(Droit.LECTURE);
        return documents.deLaLigne(ligneId);
    }

    @GetMapping("/tranches/{trancheId}/lignes/export")
    @Operation(summary = "Export Excel des lignes de la tranche (mêmes filtres que la liste)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx.", content = @Content(mediaType = XLSX,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public ResponseEntity<byte[]> exporterLignes(@Parameter(description = TRANCHE) @PathVariable UUID trancheId,
                                                 @ParameterObject @ModelAttribute FiltresCommission filtres) {
        droits.exiger(Droit.IMPRESSION);
        return service.exporterLignes(trancheId, filtres);
    }

    @GetMapping("/tranches/export")
    @Operation(summary = "Export Excel de la liste des tranches")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx.", content = @Content(mediaType = XLSX,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "403", description = DROIT, content = @Content)
    })
    public ResponseEntity<byte[]> exporterTranches(
            @Parameter(description = "Recherche multi-critère, comme la liste.") @RequestParam(required = false)
            String recherche) {
        droits.exiger(Droit.IMPRESSION);
        return service.exporterTranches(recherche);
    }
}
