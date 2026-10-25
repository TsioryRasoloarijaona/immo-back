package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.relances.ApercuPremieresRelancesDto;
import com.tsix.immocrm.dto.patrimoine.relances.ResultatPremieresRelancesDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Premières relances")
@RestController
@RequestMapping("/api/tranches")
@PreAuthorize("hasRole('Administrateur')")
public class PremieresRelancesController {

    private static final String ID_TRANCHE = "Id de la tranche (CfgTranche.PKey).";
    private static final String SANS_DATE = "Tranche sans Date de 1ère relance (« Vous devez saisir une Date de 1ère relance! »).";
    private static final String INTROUVABLE = "Tranche inconnue ou d'une entité inactive.";
    private static final String NON_ADMIN = "L'appelant n'a pas le rôle Administrateur.";

    @GetMapping("/{id}/premieres-relances/apercu")
    @Operation(summary = "Aperçu des premières relances : dossiers retenus, nouvelle échéance, alertes à créer")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = SANS_DATE, content = @Content),
            @ApiResponse(responseCode = "403", description = NON_ADMIN, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public ApercuPremieresRelancesDto apercu(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        throw new UnsupportedOperationException();
    }

    @PostMapping("/{id}/premieres-relances")
    @Operation(summary = "Écrit les dates de 1re relance des dossiers et les alertes, en une transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = SANS_DATE, content = @Content),
            @ApiResponse(responseCode = "403", description = NON_ADMIN, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public ResultatPremieresRelancesDto executer(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        throw new UnsupportedOperationException();
    }

    @GetMapping("/{id}/premieres-relances.pdf")
    @Operation(summary = "Lettres de 1re relance (PDF inline), une page par dossier relancé")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF.", content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = SANS_DATE + " Ou « Aucun dossier à imprimer ».",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = NON_ADMIN, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public ResponseEntity<byte[]> lettres(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        throw new UnsupportedOperationException();
    }
}
