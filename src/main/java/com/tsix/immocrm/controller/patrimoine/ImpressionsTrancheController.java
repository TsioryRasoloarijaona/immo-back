package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.impressions.ApercuRelance2Dto;
import com.tsix.immocrm.services.patrimoine.impressions.ImpressionsTrancheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Impressions de la tranche")
@RestController
@RequestMapping("/api/tranches")
@PreAuthorize("hasAnyRole('Administrateur', 'Direction Générale')")
public class ImpressionsTrancheController {

    private static final String ID_TRANCHE = "Id de la tranche (CfgTranche.PKey).";
    private static final String STATUT = "Code de statut à retenir, répétable, parmi les 5 de la Relance 2 ; absent = les 5.";
    private static final String HORS_LISTE = "Un `statut` hors des 5 codes de la Relance 2.";
    private static final String INTERDIT = "Rôle requis : Administrateur ou Direction Générale ; clé d'API : hors portée.";
    private static final String INTROUVABLE = "Tranche inconnue ou d'une entité inactive.";

    private final ImpressionsTrancheService service;

    public ImpressionsTrancheController(ImpressionsTrancheService service) {
        this.service = service;
    }

    @GetMapping("/{id}/relances-2/apercu")
    @Operation(summary = "Aperçu de la Relance 2 par statut : lettres par acquéreur, montants, dossiers écartés")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = HORS_LISTE, content = @Content),
            @ApiResponse(responseCode = "403", description = INTERDIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public ApercuRelance2Dto apercu(@Parameter(description = ID_TRANCHE) @PathVariable UUID id,
                                    @Parameter(description = STATUT, example = "CONCRETISATION")
                                    @RequestParam(name = "statut", required = false) List<String> statut) {
        return service.apercu(id, statut);
    }

    @GetMapping("/{id}/relances-2.pdf")
    @Operation(summary = "Lettres de Relance 2 (PDF inline), une page par acquéreur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF.", content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = HORS_LISTE, content = @Content),
            @ApiResponse(responseCode = "403", description = INTERDIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public ResponseEntity<byte[]> lettres(@Parameter(description = ID_TRANCHE) @PathVariable UUID id,
                                          @Parameter(description = STATUT, example = "CONCRETISATION")
                                          @RequestParam(name = "statut", required = false) List<String> statut) {
        ImpressionsTrancheService.Lettres lettres = service.lettres(id, statut);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(lettres.nomFichier()).build().toString())
                .body(lettres.pdf());
    }
}
